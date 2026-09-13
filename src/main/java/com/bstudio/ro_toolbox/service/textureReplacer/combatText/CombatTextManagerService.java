package com.bstudio.ro_toolbox.service.textureReplacer.combatText;

import com.bstudio.ro_toolbox.service.common.ICommonMethods;
import com.bstudio.ro_toolbox.service.common.ResourcesUpdater;
import com.bstudio.ro_toolbox.service.textureReplacer.AvailablePackage;
import com.bstudio.ro_toolbox.service.textureReplacer.GameResourceService;
import com.bstudio.ro_toolbox.service.app.AppConfigService;
import com.bstudio.ro_toolbox.util.AppDataPaths;
import com.bstudio.ro_toolbox.util.RepositoryZipDownloader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@DependsOn("appConfigService")
public class CombatTextManagerService implements GameResourceService, ICommonMethods {
    private static final String DEFAULT_REPO = "https://github.com/MartinBStudio/RO_CombatText_resources";
    private static final String MANIFEST_FILE_NAME = "manifestCombatText.json";
    private static final String LEGACY_MANIFEST_FILE_NAME = "manifest.json";
    private static final Path APP_DATA_ROOT = AppDataPaths.resolveRoToolboxAppDataRoot();
    private static final Path RESOURCES_DIR = APP_DATA_ROOT.resolve("resources").resolve("combatText");
    private static final Path GAME_SUFFIX = Paths.get("3ddata");


    private final AppConfigService appConfigService;
    private final ResourcesUpdater resourcesUpdater;
    private volatile String currentCombatTextProfile = null;

    public Path getResourcesDir() {
        return RESOURCES_DIR;
    }



    public Path getGameDataDir() {
        Path selectedGameBase = appConfigService.getSelectedGameBase();
        return (selectedGameBase == null) ? null : selectedGameBase.resolve(GAME_SUFFIX);
    }

    public void setCurrentCombatTextProfile(String profile) {
        currentCombatTextProfile = (profile == null || profile.isBlank()) ? null : profile;
    }

    public void downloadAndExtract() throws IOException {
        RepositoryZipDownloader.downloadAndExtract(
                DEFAULT_REPO,
                RESOURCES_DIR,
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
        Path itemFolder = getGameDataDir();
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

    public List<String> listDownloadedProfiles() {
        return listDownloadedProfiles(RESOURCES_DIR, MANIFEST_FILE_NAME);
    }

    public List<AvailablePackage> listAvailableProfiles() {
        return listAvailableProfiles(RESOURCES_DIR, appConfigService.getSelectedGameBase(), MANIFEST_FILE_NAME);
    }

    public void installProfile(String profileId) throws IOException {
        Path destination = getGameDataDir();
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
        Path itemFolder = getGameDataDir();
        if (itemFolder == null || !Files.exists(itemFolder)) {
            return null;
        }

        Path manifest = resolveManifestPath(itemFolder);
        if (!Files.isRegularFile(manifest)) {
            return null;
        }
        return AvailablePackage.builder().id(readManifestName(manifest)).name(readManifestName(manifest)).author(readManifestAuthor(manifest)).description(readManifestDescription(manifest)).url(readManifestUrl(manifest)).createdAt(readManifestCreatedAt(manifest)).version(readManifestVersion(manifest)).previewImages(loadPreviewImages(manifest)).source(manifest).managedSubfolders(readManifestManagedSubfolders(manifest)).disabledManagedSubfolders(readManifestDisabledManagedSubfolders(itemFolder, readManifestManagedSubfolders(manifest))).build();
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

    public ResourcesUpdater.ResourcesUpdateCheckResult checkResourcesUpdate() {
        return resourcesUpdater.checkResourcesUpdate(DEFAULT_REPO, RESOURCES_DIR);
    }

}
