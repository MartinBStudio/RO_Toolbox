package com.bstudio.ro_toolbox.service.textureReplacer.combatText;

import com.bstudio.ro_toolbox.service.textureReplacer.AvailablePackage;
import com.bstudio.ro_toolbox.service.textureReplacer.GameResourceService;
import com.bstudio.ro_toolbox.service.app.AppConfigService;
import com.bstudio.ro_toolbox.util.AppDataPaths;
import com.bstudio.ro_toolbox.util.RepositoryZipDownloader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@Slf4j
public class CombatTextManagerService implements GameResourceService {
    private static final String DEFAULT_REPO = "https://github.com/MartinBStudio/RO_CombatText_resources";
    private static final String MANIFEST_FILE_NAME = "manifestCombatText.json";
    private static final String LEGACY_MANIFEST_FILE_NAME = "manifest.json";
    private static final Path APP_DATA_ROOT = AppDataPaths.resolveRoToolboxAppDataRoot();
    private static final Path RESOURCES_DIR = APP_DATA_ROOT.resolve("resources").resolve("combatText");
    private static final Path GAME_SUFFIX = Paths.get("3ddata");

    private static final Path CONFIG_DIR = APP_DATA_ROOT.resolve("config");

    private final AppConfigService appConfigService;
    private volatile String currentCombatTextProfile = null;

    public CombatTextManagerService(AppConfigService appConfigService) {
        this.appConfigService = appConfigService;
        AppDataPaths.ensureRuntimeDirs(APP_DATA_ROOT, CONFIG_DIR, RESOURCES_DIR);
    }

    public Path getResourcesDir() {
        return RESOURCES_DIR;
    }

    public Path getSelectedGameBase() {
        return appConfigService.getSelectedGameBase();
    }

    public Path getSelectedGameItemFolder() {
        Path selectedGameBase = getSelectedGameBase();
        return (selectedGameBase == null) ? null : selectedGameBase.resolve(GAME_SUFFIX);
    }

    public String getCurrentCombatTextProfile() {
        return currentCombatTextProfile;
    }

    public void setCurrentCombatTextProfile(String profile) {
        currentCombatTextProfile = (profile == null || profile.isBlank()) ? null : profile;
    }

    public void downloadAndExtract(String repoUrl, Path destDir) throws IOException {
        RepositoryZipDownloader.downloadAndExtract(
                repoUrl,
                DEFAULT_REPO,
                destDir,
                "RO_CombatTextManager/1.0",
                log::info
        );
    }

    public void copyDirectoryContents(Path src, Path dst) throws IOException {
        if (!Files.exists(src) || !Files.isDirectory(src)) return;
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
        if (!Files.exists(dir) || !Files.isDirectory(dir)) return;
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
        if (!Files.exists(RESOURCES_DIR) || !Files.isDirectory(RESOURCES_DIR)) return;
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
        Path itemFolder = getSelectedGameItemFolder();
        if (itemFolder == null || !Files.exists(itemFolder) || !Files.isDirectory(itemFolder)) return;

        Path manifest = resolveManifestPath(itemFolder);
        List<String> managedSubfolders = readManifestManagedSubfolders(manifest);
        if (managedSubfolders != null && !managedSubfolders.isEmpty()) {
            deleteManagedSubfolders(itemFolder, managedSubfolders);
        }
        deleteManifestFiles(itemFolder, MANIFEST_FILE_NAME);
    }

    private void removeInstalledProfileFiles(Path destination) throws IOException {
        Path manifest = destination.resolve(MANIFEST_FILE_NAME);
        if (Files.exists(manifest) && Files.isRegularFile(manifest)) {
            List<String> managedSubfolders = readManifestManagedSubfolders(manifest);
            if (managedSubfolders != null && !managedSubfolders.isEmpty()) {
                deleteManagedSubfolders(destination, managedSubfolders);
            }
        }
        deleteManifestFiles(destination, MANIFEST_FILE_NAME);
    }

    private void normalizeInstalledManifest(Path destination) throws IOException {
        Path currentManifest = destination.resolve(MANIFEST_FILE_NAME);
        Path legacyManifest = destination.resolve(LEGACY_MANIFEST_FILE_NAME);
        if (Files.exists(legacyManifest) && Files.isRegularFile(legacyManifest)) {
            if (!Files.exists(currentManifest) || !Files.isRegularFile(currentManifest)) {
                Files.move(legacyManifest, currentManifest, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.deleteIfExists(legacyManifest);
            }
        }
    }

    private Path resolveManifestPath(Path directory) {
        return directory.resolve(MANIFEST_FILE_NAME);
    }

    private void deleteManifestFiles(Path directory, String... manifestNames) throws IOException {
        for (String manifestName : manifestNames) {
            if (manifestName == null || manifestName.isBlank()) continue;
            Files.deleteIfExists(directory.resolve(manifestName));
        }
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
                if (name.endsWith(".png")) mimeType = "image/png";
                else if (name.endsWith(".jpg") || name.endsWith(".jpeg")) mimeType = "image/jpeg";
                else if (name.endsWith(".gif")) mimeType = "image/gif";
                else if (name.endsWith(".webp")) mimeType = "image/webp";
                else return null;
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
                        Path manifest = resolveManifestPath(p);
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
        Path selectedGameBase = getSelectedGameBase();
        if (selectedGameBase != null) {
            roots.add(selectedGameBase.resolveSibling(RESOURCES_DIR.getFileName()));
        }

        for (Path root : roots) {
            if (root == null || !Files.exists(root) || !Files.isDirectory(root)) continue;
            try (var stream = Files.walk(root)) {
                for (Path p : (Iterable<Path>) stream::iterator) {
                    if (!Files.isDirectory(p)) continue;
                    String name = p.getFileName().toString();
                    if (name.startsWith(".")) continue;
                    Path manifest = resolveManifestPath(p);
                    if (!Files.exists(manifest) || !Files.isRegularFile(manifest)) continue;
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
            if (versionDiff != 0) return versionDiff;
            return a.getId().compareToIgnoreCase(b.getId());
        });
        return results;
    }

    public void installProfile(String profileId) throws IOException {
        Path destination = getSelectedGameItemFolder();
        if (destination == null) {
            throw new IllegalStateException("No game installation folder is selected.");
        }
        AvailablePackage selected = findAvailableProfile(profileId);

        removeInstalledProfileFiles(destination);
        copyDirectoryContents(selected.getSource(), destination);
        normalizeInstalledManifest(destination);
        setCurrentCombatTextProfile(selected.getId());
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



    public AvailablePackage getInstalledProfileInfo() {
        Path itemFolder = getSelectedGameItemFolder();
        if (itemFolder == null || !Files.exists(itemFolder)) {
            return null;
        }

        Path manifest = resolveManifestPath(itemFolder);
        if (manifest == null || !Files.isRegularFile(manifest)) {
            return null;
        }
        AvailablePackage availablePackage = AvailablePackage.builder().id(readManifestName(manifest)).name(readManifestName(manifest)).author(readManifestAuthor(manifest)).description(readManifestDescription(manifest)).url(readManifestUrl(manifest)).createdAt(readManifestCreatedAt(manifest)).version(readManifestVersion(manifest)).previewImages(loadPreviewImages(manifest)).source(manifest).build();

        return availablePackage;
    }

    private String readManifestName(Path manifestFile) {
        try {
            String content = Files.readString(manifestFile);
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\"name\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"").matcher(content);
            if (!matcher.find()) return null;
            String value = matcher.group(1).replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
            return value.trim();
        } catch (Exception e) {
            return null;
        }
    }

    private String readManifestDescription(Path manifestFile) {
        try {
            String content = Files.readString(manifestFile);
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\"description\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"").matcher(content);
            if (!matcher.find()) return null;
            String value = matcher.group(1).replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
            return value.trim();
        } catch (Exception e) {
            return null;
        }
    }

    private String readManifestUrl(Path manifestFile) {
        try {
            String content = Files.readString(manifestFile);
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\"url\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"").matcher(content);
            if (!matcher.find()) return null;
            String value = matcher.group(1).replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
            return value.trim();
        } catch (Exception e) {
            return null;
        }
    }

    private String readManifestAuthor(Path manifestFile) {
        try {
            String content = Files.readString(manifestFile);
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\"author\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"").matcher(content);
            if (!matcher.find()) return null;
            String value = matcher.group(1).replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
            return value.trim();
        } catch (Exception e) {
            return null;
        }
    }

    private String readManifestCreatedAt(Path manifestFile) {
        try {
            String content = Files.readString(manifestFile);
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\"createdAt\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"").matcher(content);
            if (!matcher.find()) return null;
            String value = matcher.group(1).replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
            return value.trim();
        } catch (Exception e) {
            return null;
        }
    }

    private String readManifestVersion(Path manifestFile) {
        try {
            String content = Files.readString(manifestFile);
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\"version\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"").matcher(content);
            if (!matcher.find()) return "0.0.0";
            return matcher.group(1).trim();
        } catch (Exception e) {
            return "0.0.0";
        }
    }

    private List<String> readManifestManagedSubfolders(Path manifestFile) {
        List<String> subfolders = new ArrayList<>();
        if (manifestFile == null || !Files.exists(manifestFile) || !Files.isRegularFile(manifestFile)) {
            return null;
        }
        try {
            String content = Files.readString(manifestFile);
            java.util.regex.Matcher arrayMatcher = java.util.regex.Pattern.compile(
                    "\"managedSubfolders\"\\s*:\\s*\\[(.*?)]",
                    java.util.regex.Pattern.DOTALL
            ).matcher(content);
            if (!arrayMatcher.find()) return null;

            String arrayContent = arrayMatcher.group(1);
            java.util.regex.Matcher itemMatcher = java.util.regex.Pattern.compile(
                    "\"((?:\\\\.|[^\"\\\\])*)\""
            ).matcher(arrayContent);
            while (itemMatcher.find()) {
                String raw = itemMatcher.group(1)
                        .replace("\\n", "\n")
                        .replace("\\\"", "\"")
                        .replace("\\\\", "\\")
                        .trim();
                if (!raw.isEmpty()) subfolders.add(raw);
            }
            return subfolders;
        } catch (Exception e) {
            return null;
        }
    }

    private void deleteManagedSubfolders(Path baseDir, List<String> managedSubfolders) throws IOException {
        if (managedSubfolders == null || managedSubfolders.isEmpty()) return;
        for (String subfolder : managedSubfolders) {
            if (subfolder == null || subfolder.isBlank()) continue;
            Path relative = Paths.get(subfolder).normalize();
            if (relative.isAbsolute() || relative.startsWith("..")) {
                log.info("Skipping invalid managedSubfolder path: " + subfolder);
                continue;
            }
            Path target = baseDir.resolve(relative).normalize();
            if (!target.startsWith(baseDir)) {
                log.info("Skipping out-of-scope managedSubfolder path: " + subfolder);
                continue;
            }
            if (Files.exists(target) && Files.isDirectory(target)) {
                deleteDirectoryContents(target);
                Files.deleteIfExists(target);
                log.info("Deleted managed subfolder: " + target.toAbsolutePath());
            }
        }
    }

    private long normalizeVersion(String version) {
        if (version == null || version.isBlank()) return 0L;
        String cleaned = version.trim().replaceFirst("(?i)^v", "");
        String[] parts = cleaned.split("[.-]");
        long value = 0L;
        long multiplier = 1_000_000_000L;
        for (String part : parts) {
            if (part == null || part.isBlank()) continue;
            String digits = part.replaceAll("[^0-9]", "");
            if (digits.isEmpty()) continue;
            value += Long.parseLong(digits) * multiplier;
            multiplier /= 1000L;
        }
        return value;
    }

    public ResourcesUpdateCheckResult checkResourcesUpdate() {
        Path localManifest = RESOURCES_DIR.resolve("manifest.json");
        boolean localExists = Files.exists(localManifest) && Files.isRegularFile(localManifest);
        String localVersion = localExists ? readManifestVersion(localManifest) : "none";

        String[] branches = {"main", "master"};
        String repoUrl = DEFAULT_REPO;
        if (repoUrl.endsWith("/")) repoUrl = repoUrl.substring(0, repoUrl.length() - 1);
        String rawBase = repoUrl
                .replace("https://github.com/", "https://raw.githubusercontent.com/");

        for (String branch : branches) {
            String remoteUrl = rawBase + "/" + branch + "/manifest.json";
            try {
                InputStream in = RepositoryZipDownloader.openUrlStream(remoteUrl, "RO_CombatTextManager/1.0");
                if (in == null) continue;
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
        return new ResourcesUpdateCheckResult(localVersion, "unknown", localExists, false, false,
                "Unable to check remote manifest.");
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
