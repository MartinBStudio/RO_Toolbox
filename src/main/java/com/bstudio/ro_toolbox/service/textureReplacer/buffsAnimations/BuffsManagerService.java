package com.bstudio.ro_toolbox.service.textureReplacer.buffsAnimations;

import com.bstudio.ro_toolbox.service.common.ICommonMethods;
import com.bstudio.ro_toolbox.service.textureReplacer.AvailablePackage;
import com.bstudio.ro_toolbox.service.textureReplacer.GameResourceService;
import com.bstudio.ro_toolbox.service.app.AppConfigService;
import com.bstudio.ro_toolbox.util.AppDataPaths;
import com.bstudio.ro_toolbox.util.RepositoryZipDownloader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class BuffsManagerService implements GameResourceService, ICommonMethods {
    private static final String DEFAULT_REPO = "https://github.com/MartinBStudio/RO_BuffAnimations_Resources.git";
    private static final String MANIFEST_FILE_NAME = "manifestBuffAnimations.json";
    private static final String LEGACY_MANIFEST_FILE_NAME = "manifest.json";
    private static final String LEGACY_BUFFS_MANIFEST_FILE_NAME = "manifestBuffs.json";
    private static final Path RESOURCES_DIR = AppDataPaths.resolveRoToolboxAppDataRoot().resolve("resources").resolve("buffs");
    private static final Path GAME_SUFFIX = Paths.get("");

    private final AppConfigService appConfigService;
    private volatile String currentBuffsProfile = null;


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


    public void setCurrentBuffsProfile(String profile) {
        currentBuffsProfile = (profile == null || profile.isBlank()) ? null : profile;
    }

    public void downloadAndExtract(String repoUrl, Path destDir) throws IOException {
        RepositoryZipDownloader.downloadAndExtract(
                repoUrl,
                DEFAULT_REPO,
                destDir,
                "RO_BuffsManager/1.0",
                log::info
        );
    }

    private void copyDirectoryContents(Path src, Path dst) throws IOException {
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
        Path gameBase = getSelectedGameBase();
        if (gameBase == null || !Files.exists(gameBase) || !Files.isDirectory(gameBase)) {
            return;
        }

        deleteManifestFiles(gameBase, MANIFEST_FILE_NAME, LEGACY_MANIFEST_FILE_NAME, LEGACY_BUFFS_MANIFEST_FILE_NAME);
        setCurrentBuffsProfile(null);

        Path defaultProfile = RESOURCES_DIR.resolve(".default");
        if (!Files.exists(defaultProfile) || !Files.isDirectory(defaultProfile)) {
            return;
        }

        List<Path> managedFiles = readDefaultFileList(defaultProfile.resolve("FILE_LIST.txt"));
        for (Path relativeFile : managedFiles) {
            Path defaultFile = defaultProfile.resolve(relativeFile);
            Path target = resolveManagedFile(gameBase, relativeFile);
            if (Files.isDirectory(target)) {
                continue;
            }
            Files.deleteIfExists(target);
            if (Files.isRegularFile(defaultFile)) {
                Files.createDirectories(target.getParent());
                Files.copy(defaultFile, target, StandardCopyOption.REPLACE_EXISTING);
                log.info("Restored default file: " + target.toAbsolutePath());
            } else {
                log.info("Deleted managed file (no default available): " + target.toAbsolutePath());
            }
        }
    }

    private void removeInstalledProfileFiles(Path destination) throws IOException {
        Path manifest = resolveProfileManifestPath(destination);
        if (manifest != null) {
            List<String> managedSubfolders = readManifestManagedSubfolders(manifest);
            if (managedSubfolders != null && !managedSubfolders.isEmpty()) {
                deleteManagedSubfolders(destination, managedSubfolders);
            }
        }
        deleteManifestFiles(destination, MANIFEST_FILE_NAME, LEGACY_MANIFEST_FILE_NAME, LEGACY_BUFFS_MANIFEST_FILE_NAME);
    }

    private void normalizeInstalledManifest(Path destination) throws IOException {
        Path currentManifest = destination.resolve(MANIFEST_FILE_NAME);
        Path legacyManifest = destination.resolve(LEGACY_MANIFEST_FILE_NAME);
        Path legacyBuffsManifest = destination.resolve(LEGACY_BUFFS_MANIFEST_FILE_NAME);

        Path preferredManifest = Files.exists(currentManifest) && Files.isRegularFile(currentManifest)
                ? currentManifest
                : null;
        if (preferredManifest == null && Files.exists(legacyManifest) && Files.isRegularFile(legacyManifest)) {
            preferredManifest = legacyManifest;
        }
        if (preferredManifest == null && Files.exists(legacyBuffsManifest) && Files.isRegularFile(legacyBuffsManifest)) {
            preferredManifest = legacyBuffsManifest;
        }

        if (preferredManifest != null) {
            if (!preferredManifest.equals(currentManifest)) {
                Files.move(preferredManifest, currentManifest, StandardCopyOption.REPLACE_EXISTING);
            }
        }

        if (Files.exists(legacyManifest) && Files.isRegularFile(legacyManifest) && !legacyManifest.equals(currentManifest)) {
            Files.deleteIfExists(legacyManifest);
        }
        if (Files.exists(legacyBuffsManifest) && Files.isRegularFile(legacyBuffsManifest) && !legacyBuffsManifest.equals(currentManifest)) {
            Files.deleteIfExists(legacyBuffsManifest);
        }
    }

    private Path resolveManifestPath(Path directory) {
        if (directory == null) {
            return null;
        }
        for (String manifestName : new String[]{MANIFEST_FILE_NAME, LEGACY_MANIFEST_FILE_NAME, LEGACY_BUFFS_MANIFEST_FILE_NAME}) {
            Path manifest = directory.resolve(manifestName);
            if (Files.exists(manifest) && Files.isRegularFile(manifest)) {
                return manifest;
            }
        }
        return null;
    }

    private Path resolveProfileManifestPath(Path directory) {
        return resolveManifestPath(directory);
    }

    private boolean hasProfileAssets(Path directory) {
        if (directory == null || !Files.isDirectory(directory)) {
            return false;
        }
        return resolveProfileManifestPath(directory) != null;
    }

    private void deleteManifestFiles(Path directory, String... manifestNames) throws IOException {
        for (String manifestName : manifestNames) {
            if (manifestName == null || manifestName.isBlank()) {
                continue;
            }
            Files.deleteIfExists(directory.resolve(manifestName));
        }
    }

    private List<Path> readDefaultFileList(Path fileListPath) throws IOException {
        if (fileListPath == null || !Files.exists(fileListPath) || !Files.isRegularFile(fileListPath)) {
            return Collections.emptyList();
        }
        List<Path> files = new ArrayList<>();
        for (String line : Files.readAllLines(fileListPath)) {
            if (line == null) {
                continue;
            }
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            Path relative = Paths.get(trimmed.replace("\\", "/")).normalize();
            if (relative.isAbsolute() || relative.startsWith("..")) {
                log.info("Skipping invalid FILE_LIST entry: " + trimmed);
                continue;
            }
            files.add(relative);
        }
        return files;
    }

    private Path resolveManagedFile(Path root, Path relative) {
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalStateException("Resolved path escapes root for FILE_LIST entry: " + relative);
        }
        return target;
    }


    public List<String> listDownloadedProfiles() {
        List<String> profiles = new ArrayList<>();
        if (RESOURCES_DIR == null || !Files.exists(RESOURCES_DIR) || !Files.isDirectory(RESOURCES_DIR)) {
            return profiles;
        }
        try (var stream = Files.list(RESOURCES_DIR)) {
            stream.filter(Files::isDirectory)
                    .forEach(p -> {
                        String name = p.getFileName().toString();
                        if (name.startsWith(".")) {
                            return;
                        }
                        if (hasProfileAssets(p)) {
                            profiles.add(p.getFileName().toString());
                        }
                    });
        } catch (IOException ignored) {
        }
        profiles.sort(String::compareToIgnoreCase);
        return profiles;
    }

    public List<AvailablePackage> listAvailableProfiles() {
        return listAvailableProfiles(RESOURCES_DIR, appConfigService.getSelectedGameBase(), MANIFEST_FILE_NAME);
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
        setCurrentBuffsProfile(selected.getId());
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
        AvailablePackage availablePackage = AvailablePackage.builder().id(readManifestName(manifest)).name(readManifestName(manifest)).author(readManifestAuthor(manifest)).description(readManifestDescription(manifest)).url(readManifestUrl(manifest)).createdAt(readManifestCreatedAt(manifest)).version(readManifestVersion(manifest)).previewImages(loadPreviewImages(manifest)).source(manifest).managedSubfolders(readManifestManagedSubfolders(manifest)).build();

        return availablePackage;
    }



    private void deleteManagedSubfolders(Path baseDir, List<String> managedSubfolders) throws IOException {
        if (managedSubfolders == null || managedSubfolders.isEmpty()) {
            return;
        }
        for (String subfolder : managedSubfolders) {
            if (subfolder == null || subfolder.isBlank()) {
                continue;
            }
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
        Path localManifest = Files.exists(RESOURCES_DIR.resolve(MANIFEST_FILE_NAME))
                ? RESOURCES_DIR.resolve(MANIFEST_FILE_NAME)
                : RESOURCES_DIR.resolve(LEGACY_MANIFEST_FILE_NAME);
        boolean localExists = Files.exists(localManifest) && Files.isRegularFile(localManifest);
        String localVersion = localExists ? readManifestVersion(localManifest) : "none";

        String[] branches = {"main", "master"};
        String repoUrl = DEFAULT_REPO;
        if (repoUrl.endsWith("/")) {
            repoUrl = repoUrl.substring(0, repoUrl.length() - 1);
        }
        if (repoUrl.endsWith(".git")) {
            repoUrl = repoUrl.substring(0, repoUrl.length() - 4);
        }
        String rawBase = repoUrl.replace("https://github.com/", "https://raw.githubusercontent.com/");

        String bestRemoteVersion = null;
        for (String branch : branches) {
            String remoteUrl = rawBase + "/" + branch + "/manifest.json?cb=" + System.currentTimeMillis();
            try {
                InputStream in = RepositoryZipDownloader.openUrlStream(remoteUrl, "RO_BuffsManager/1.0");
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
                if (bestRemoteVersion == null || normalizeVersion(remoteVersion) > normalizeVersion(bestRemoteVersion)) {
                    bestRemoteVersion = remoteVersion;
                }
            } catch (Exception e) {
                log.info("Remote manifest check failed for branch " + branch + ": " + e.getMessage());
            }
        }
        if (bestRemoteVersion != null) {
            boolean updateAvailable = !localExists || normalizeVersion(bestRemoteVersion) > normalizeVersion(localVersion);
            String message = updateAvailable
                    ? "New resources available: v" + bestRemoteVersion + (localExists ? " (local: v" + localVersion + ")" : " (not downloaded)")
                    : "Resources are up to date (v" + localVersion + ").";
            return new ResourcesUpdateCheckResult(localVersion, bestRemoteVersion, localExists, updateAvailable, true, message);
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
