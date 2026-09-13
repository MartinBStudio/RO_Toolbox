package com.bstudio.ro_toolbox.service.textureReplacer.buffIcons;

import com.bstudio.ro_toolbox.config.GeneralConstants;
import com.bstudio.ro_toolbox.service.textureReplacer.AvailablePackage;
import com.bstudio.ro_toolbox.service.textureReplacer.GameResourceService;
import com.bstudio.ro_toolbox.service.app.AppConfigService;
import com.bstudio.ro_toolbox.util.AppDataPaths;
import com.bstudio.ro_toolbox.util.RepositoryZipDownloader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class IconsManagerService implements GameResourceService {
    private static final String ICONS_REMOTE_REPOSITORY = "https://github.com/MartinBStudio/RO_BuffIcons_resources";
    private static final String PACKAGE_MANIFEST_FILE = "manifestBuffIcons.json";

    private static final Path RESOURCES_DIR = AppDataPaths.resolveRoToolboxAppDataRoot().resolve("resources").resolve("buffIcons");
    private static final Path GAME_SUFFIX = Paths.get("");

    private final AppConfigService appConfigService;

    public Path getResourcesDir() {
        return RESOURCES_DIR;
    }

    public Path getSelectedGameItemFolder() {
        Path selectedGameBase = appConfigService.getSelectedGameBase();
        return selectedGameBase == null ? null : selectedGameBase.resolve(GAME_SUFFIX);
    }

    public void downloadAndExtract(String repoUrl, Path destDir) throws IOException {
        RepositoryZipDownloader.downloadAndExtract(
                repoUrl,
                ICONS_REMOTE_REPOSITORY,
                destDir,
                "RO_BuffIconsManager/1.0",
                log::info
        );
    }

    public void copyDirectoryContents(Path src, Path dst) throws IOException {
        if (!Files.exists(src) || !Files.isDirectory(src)) {
            return;
        }
        try (java.util.stream.Stream<Path> stream = Files.walk(src)) {
            stream.filter(sourcePath -> !isHiddenPathInTree(src, sourcePath))
                    .forEach(sourcePath -> {
                        try {
                            Path rel = src.relativize(sourcePath);
                            Path targetPath = dst.resolve(rel);
                            if (Files.isDirectory(sourcePath)) {
                                Files.createDirectories(targetPath);
                            } else {
                                Files.createDirectories(targetPath.getParent());
                                Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                                log.info("Copied: " + targetPath.toAbsolutePath());
                            }
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    private boolean isHiddenPathInTree(Path root, Path path) {
        if (path == null || root == null) {
            return false;
        }
        Path relative = root.relativize(path).normalize();
        if (relative.toString().isEmpty()) {
            return false;
        }
        for (Path segment : relative) {
            if (segment.toString().startsWith(".")) {
                return true;
            }
        }
        return false;
    }

    public void deleteDirectoryContents(Path dir) throws IOException {
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            return;
        }
        Files.walkFileTree(dir, new java.nio.file.SimpleFileVisitor<Path>() {
            @Override
            public java.nio.file.FileVisitResult visitFile(Path file, java.nio.file.attribute.BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                log.info("Deleted file: " + file.toAbsolutePath());
                return java.nio.file.FileVisitResult.CONTINUE;
            }

            @Override
            public java.nio.file.FileVisitResult postVisitDirectory(Path visitedDir, IOException exc) throws IOException {
                if (!visitedDir.equals(dir)) {
                    Files.deleteIfExists(visitedDir);
                    log.info("Deleted dir: " + visitedDir.toAbsolutePath());
                }
                return java.nio.file.FileVisitResult.CONTINUE;
            }
        });
    }

    public void clearResources() throws IOException {
        if (!Files.exists(RESOURCES_DIR) || !Files.isDirectory(RESOURCES_DIR)) {
            return;
        }
        try (var stream = Files.list(RESOURCES_DIR)) {
            for (Path entry : (Iterable<Path>) stream::iterator) {
                String name = entry.getFileName().toString();
                if (".default".equals(name)) {
                    continue;
                }
                if (Files.isDirectory(entry)) {
                    deleteDirectoryContents(entry);
                    Files.deleteIfExists(entry);
                    log.info("Deleted profile dir: " + entry.toAbsolutePath());
                } else {
                    Files.deleteIfExists(entry);
                    log.info("Deleted resource file: " + entry.toAbsolutePath());
                }
            }
        }
    }

    public void clearSelectedItemFolder() throws IOException {
        Path gameBase = appConfigService.getSelectedGameBase();
        if (gameBase == null || !Files.exists(gameBase) || !Files.isDirectory(gameBase)) {
            return;
        }

        deleteInstalledFiles(gameBase);
    }

    public void installProfile(String profileId) throws IOException {
        Path destination = getSelectedGameItemFolder();
        if (destination == null) {
            throw new IllegalStateException("No game installation folder is selected.");
        }
        AvailablePackage selected = findAvailableProfile(profileId);

        deleteInstalledFiles(destination);
        copyDirectoryContents(selected.getSource(), destination);
    }

    private void deleteInstalledFiles(Path baseDir) throws IOException {
        if (baseDir == null) {
            return;
        }
        Files.deleteIfExists(baseDir.resolve(PACKAGE_MANIFEST_FILE));
        Files.deleteIfExists(baseDir.resolve(Paths.get("3ddata", "control", "Res", "stateicon.dds")));
    }

    private AvailablePackage findAvailableProfile(String profileId) {
        String normalizedProfileId = profileId == null ? "" : profileId.trim();
        if (normalizedProfileId.isEmpty()) {
            throw new IllegalArgumentException("profileId is required.");
        }
        return listAvailableProfiles().stream()
                .filter(profile -> profile.getId().equals(normalizedProfileId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Profile not found: " + normalizedProfileId));
    }

    public record AvailableProfile(
            String id,
            String name,
            String author,
            String description,
            String url,
            String createdAt,
            String version,
            long normalizedVersion,
            Path source,
            List<String> previewImages
    ) {
        public AvailableProfile(
                String id,
                String name,
                String author,
                String description,
                String url,
                String createdAt,
                String version,
                long normalizedVersion,
                Path source
        ) {
            this(id, name, author, description, url, createdAt, version, normalizedVersion, source, List.of());
        }
    }

    public List<String> loadPreviewImages(Path profileDir) {
        Path previewDir = profileDir == null ? null : profileDir.resolve(".preview");
        if (previewDir == null || !Files.isDirectory(previewDir)) {
            return List.of();
        }
        try (var stream = Files.walk(previewDir)) {
            return stream.filter(Files::isRegularFile)
                    .filter(this::isSupportedPreviewImage)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER))
                    .map(this::toDataUrl)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (IOException e) {
            log.info("Failed to load preview images from " + previewDir.toAbsolutePath() + ": " + e.getMessage());
            return List.of();
        }
    }

    private boolean isSupportedPreviewImage(Path file) {
        if (file == null || !Files.isRegularFile(file)) {
            return false;
        }
        String name = file.getFileName().toString().toLowerCase();
        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".gif") || name.endsWith(".webp");
    }

    private String toDataUrl(Path file) {
        try (InputStream in = Files.newInputStream(file)) {
            byte[] bytes = in.readAllBytes();
            String mimeType = Files.probeContentType(file);
            if (mimeType == null) {
                String name = file.getFileName().toString().toLowerCase();
                if (name.endsWith(".png")) {
                    mimeType = "image/png";
                } else if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
                    mimeType = "image/jpeg";
                } else if (name.endsWith(".gif")) {
                    mimeType = "image/gif";
                } else if (name.endsWith(".webp")) {
                    mimeType = "image/webp";
                } else {
                    return null;
                }
            }
            return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (IOException e) {
            log.info("Failed to encode preview image " + file.toAbsolutePath() + ": " + e.getMessage());
            return null;
        }
    }

    public List<String> listDownloadedProfiles() {
        List<String> profiles = new ArrayList<>();
        if (RESOURCES_DIR == null || !Files.exists(RESOURCES_DIR) || !Files.isDirectory(RESOURCES_DIR)) {
            return profiles;
        }
        try (var stream = Files.list(RESOURCES_DIR)) {
            stream.filter(Files::isDirectory)
                    .filter(p -> !p.getFileName().toString().startsWith("."))
                    .forEach(p -> {
                        Path manifest = p.resolve(PACKAGE_MANIFEST_FILE);
                        if (Files.exists(manifest) && Files.isRegularFile(manifest)) {
                            profiles.add(p.getFileName().toString());
                        }
                    });
        } catch (IOException ignored) {
        }
        profiles.sort(String::compareToIgnoreCase);
        return profiles;
    }

    public List<AvailablePackage> listAvailableProfiles() {
        List<AvailablePackage> results = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        List<Path> roots = new ArrayList<>();
        roots.add(RESOURCES_DIR);
        Path selectedGameBase = appConfigService.getSelectedGameBase();
        if (selectedGameBase != null) {
            roots.add(selectedGameBase.resolveSibling(RESOURCES_DIR.getFileName()));
        }

        for (Path root : roots) {
            if (root == null || !Files.exists(root) || !Files.isDirectory(root)) {
                continue;
            }
            try (var stream = Files.walk(root)) {
                for (Path p : (Iterable<Path>) stream::iterator) {
                    if (!Files.isDirectory(p)) {
                        continue;
                    }
                    String name = p.getFileName().toString();
                    if (name.startsWith(".")) {
                        continue;
                    }
                    Path manifest = p.resolve(PACKAGE_MANIFEST_FILE);
                    if (!Files.exists(manifest) || !Files.isRegularFile(manifest)) {
                        continue;
                    }
                    if (seen.add(name)) {
                        AvailablePackage availablePackage = AvailablePackage.builder().id(readManifestName(manifest)).name(readManifestName(manifest)).author(readManifestAuthor(manifest)).description(readManifestDescription(manifest)).url(readManifestUrl(manifest)).createdAt(readManifestCreatedAt(manifest)).version(readManifestVersion(manifest)).previewImages(loadPreviewImages(manifest)).source(manifest).build();
                        results.add(availablePackage);
                    }
                }
            } catch (IOException ignored) {
            }
        }

        results.sort((a, b) -> {
            int versionDiff = Long.compare(b.getNormalizedVersion(), a.getNormalizedVersion());
            if (versionDiff != 0) {
                return versionDiff;
            }
            return a.getId().compareToIgnoreCase(b.getId());
        });
        return results;
    }

    public AvailablePackage getInstalledProfileInfo() {
        Path itemFolder = getSelectedGameItemFolder();
        if (itemFolder == null || !Files.exists(itemFolder)) {
            return null;
        }

        Path manifest = itemFolder.resolve(PACKAGE_MANIFEST_FILE);
        if (manifest == null || !Files.isRegularFile(manifest)) {
            return null;
        }
        AvailablePackage availablePackage = AvailablePackage.builder().id(readManifestName(manifest)).name(readManifestName(manifest)).author(readManifestAuthor(manifest)).description(readManifestDescription(manifest)).url(readManifestUrl(manifest)).createdAt(readManifestCreatedAt(manifest)).version(readManifestVersion(manifest)).previewImages(loadPreviewImages(manifest)).source(manifest).build();

        return availablePackage;
    }

    private String readManifestField(Path manifestFile, String... keys) {
        try {
            String content = Files.readString(manifestFile);
            for (String key : keys) {
                if (key == null || key.isBlank()) {
                    continue;
                }
                java.util.regex.Matcher matcher = java.util.regex.Pattern
                        .compile("\"" + java.util.regex.Pattern.quote(key) + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
                        .matcher(content);
                if (matcher.find()) {
                    return matcher.group(1).replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\").trim();
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private String readManifestName(Path manifestFile) {
        return readManifestField(manifestFile, "name");
    }

    private String readManifestDescription(Path manifestFile) {
        return readManifestField(manifestFile, "description");
    }

    private String readManifestUrl(Path manifestFile) {
        return readManifestField(manifestFile, "url", "repository");
    }

    private String readManifestAuthor(Path manifestFile) {
        return readManifestField(manifestFile, "author");
    }

    private String readManifestCreatedAt(Path manifestFile) {
        return readManifestField(manifestFile, "createdAt", "timestamp");
    }

    private String readManifestVersion(Path manifestFile) {
        String version = readManifestField(manifestFile, "version");
        return version == null ? "0.0.0" : version;
    }

    private long normalizeVersion(String version) {
        if (version == null || version.isBlank()) {
            return 0L;
        }
        String cleaned = version.trim().replaceFirst("(?i)^v", "");
        String[] parts = cleaned.split("[.-]");
        long value = 0L;
        long multiplier = 1_000_000_000L;
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            String digits = part.replaceAll("[^0-9]", "");
            if (digits.isEmpty()) {
                continue;
            }
            value += Long.parseLong(digits) * multiplier;
            multiplier /= 1000L;
        }
        return value;
    }

    public ResourcesUpdateCheckResult checkResourcesUpdate() {
        Path localManifest = RESOURCES_DIR.resolve(GeneralConstants.RESOURCE_MANIFEST_FILE_NAME);
        boolean localExists = Files.exists(localManifest) && Files.isRegularFile(localManifest);
        String localVersion = localExists ? readManifestVersion(localManifest) : "none";

        String[] branches = {"main", "master"};
        String repoUrl = ICONS_REMOTE_REPOSITORY;
        if (repoUrl.endsWith("/")) {
            repoUrl = repoUrl.substring(0, repoUrl.length() - 1);
        }
        String rawBase = repoUrl.replace("https://github.com/", "https://raw.githubusercontent.com/");

        for (String branch : branches) {
            String remoteUrl = rawBase + "/" + branch + "/" + GeneralConstants.RESOURCE_MANIFEST_FILE_NAME + "?cb=" + System.currentTimeMillis();
            try {
                InputStream in = RepositoryZipDownloader.openUrlStream(remoteUrl, "RO_BuffIconsManager/1.0");
                if (in == null) {
                    continue;
                }
                String content;
                try (java.io.InputStreamReader reader = new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)) {
                    content = new java.io.BufferedReader(reader).lines().collect(java.util.stream.Collectors.joining("\n"));
                }
                java.util.regex.Matcher matcher = java.util.regex.Pattern
                        .compile("\"version\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
                        .matcher(content);
                String remoteVersion = matcher.find() ? matcher.group(1).trim() : "0.0.0";
                boolean updateAvailable = !localExists || normalizeVersion(remoteVersion) > normalizeVersion(localVersion);
                String message = updateAvailable
                        ? "New resources available: v" + remoteVersion + (localExists ? " (local: v" + localVersion + ")" : " (not downloaded)")
                        : "Resources are up to date (v" + localVersion + ").";
                return new ResourcesUpdateCheckResult(localVersion, remoteVersion, localExists, updateAvailable, true, message);
            } catch (Exception e) {
                log.info("Remote manifest check failed for branch " + branch + ": " + e.getMessage());
            }
        }

        return new ResourcesUpdateCheckResult(localVersion, "unknown", localExists, false, false, "Unable to check remote manifest.");
    }

    public record ResourcesUpdateCheckResult(
            String localVersion,
            String remoteVersion,
            boolean localExists,
            boolean updateAvailable,
            boolean success,
            String message
    ) {
    }
}
