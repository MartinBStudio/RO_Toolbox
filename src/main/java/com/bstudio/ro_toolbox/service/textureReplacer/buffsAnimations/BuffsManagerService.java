package com.bstudio.ro_toolbox.service.textureReplacer.buffsAnimations;

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
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@DependsOn("appConfigService")
public class BuffsManagerService implements GameResourceService, ICommonMethods {
    private static final String DEFAULT_REPO = "https://github.com/MartinBStudio/RO_BuffAnimations_Resources.git";
    private static final String MANIFEST_FILE_NAME = "manifestBuffAnimations.json";
    private static final String LEGACY_MANIFEST_FILE_NAME = "manifest.json";
    private static final String LEGACY_BUFFS_MANIFEST_FILE_NAME = "manifestBuffs.json";
    private static final Path RESOURCES_DIR = AppDataPaths.resolveRoToolboxAppDataRoot().resolve("resources").resolve("buffs");
    private static final Path GAME_SUFFIX = Paths.get("");

    private final AppConfigService appConfigService;
    private final ResourcesUpdater resourcesUpdater;
    private volatile String currentBuffsProfile = null;


    public Path getResourcesDir() {
        return RESOURCES_DIR;
    }


    public Path getGameDataDir() {
        Path selectedGameBase = appConfigService.getSelectedGameBase();
        return (selectedGameBase == null) ? null : selectedGameBase.resolve(GAME_SUFFIX);
    }


    public void setCurrentBuffsProfile(String profile) {
        currentBuffsProfile = (profile == null || profile.isBlank()) ? null : profile;
    }

    public void downloadAndExtract() throws IOException {
        RepositoryZipDownloader.downloadAndExtract(
                DEFAULT_REPO,
                RESOURCES_DIR,
                "RO_BuffsManager/1.0",
                log::info
        );
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
        Path manifest = resolveManifestPath(destination, MANIFEST_FILE_NAME);
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
        Path itemFolder = getGameDataDir();
        if (itemFolder == null || !Files.exists(itemFolder)) {
            return null;
        }

        Path manifest = resolveManifestPath(itemFolder,MANIFEST_FILE_NAME);
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



    public ResourcesUpdater.ResourcesUpdateCheckResult checkResourcesUpdate() {
        return resourcesUpdater.checkResourcesUpdate(DEFAULT_REPO, RESOURCES_DIR);
    }

}
