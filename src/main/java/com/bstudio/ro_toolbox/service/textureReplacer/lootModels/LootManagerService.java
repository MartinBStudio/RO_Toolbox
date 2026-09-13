package com.bstudio.ro_toolbox.service.textureReplacer.lootModels;

import com.bstudio.ro_toolbox.service.app.AppConfigService;
import com.bstudio.ro_toolbox.service.common.ICommonMethods;
import com.bstudio.ro_toolbox.service.common.ResourcesUpdater;
import com.bstudio.ro_toolbox.service.textureReplacer.GameResourceService;
import com.bstudio.ro_toolbox.service.textureReplacer.ResourcePackage;
import com.bstudio.ro_toolbox.util.AppDataPaths;
import com.bstudio.ro_toolbox.util.RepositoryZipDownloader;

import java.io.*;
import java.nio.file.*;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class LootManagerService implements GameResourceService, ICommonMethods {
    private static final String DEFAULT_REPO =
            "https://github.com/MartinBStudio/RO_LootFilter_resources";
    private static final String MANIFEST_FILE_NAME = "manifestLoot.json";
    private static final Path RESOURCES_DIR =
            AppDataPaths.resolveRoToolboxAppDataRoot().resolve("resources").resolve("lootManager");
    private static final Path GAME_DATA_DIR = Paths.get("3ddata", "item");

    private final AppConfigService appConfigService;
    private final ResourcesUpdater resourcesUpdater;

    @Override
    public Path getResourcesDir() {
        return RESOURCES_DIR;
    }

    @Override
    public Path getGameDataDir() {
        Path selectedGameBase = appConfigService.getSelectedGameBase();
        return (selectedGameBase == null) ? null : selectedGameBase.resolve(GAME_DATA_DIR);
    }

    @Override
    public void downloadAndExtract() throws IOException {
        RepositoryZipDownloader.downloadAndExtract(
                DEFAULT_REPO, RESOURCES_DIR, "RO_LootManager/1.0", log::info);
    }

    @Override
    public void clearDownloadedPackages() throws IOException {
        clearResources(RESOURCES_DIR);
    }

    @Override
    public void clearInstalledPackage() throws IOException {
        Path itemFolder = getGameDataDir();
        if (itemFolder == null || !Files.exists(itemFolder) || !Files.isDirectory(itemFolder)) return;

        Path manifest = resolveManifestPath(itemFolder, MANIFEST_FILE_NAME);
        List<String> managedSubfolders = readManifestManagedSubfolders(manifest);
        if (managedSubfolders != null && !managedSubfolders.isEmpty()) {
            deleteManagedSubfolders(itemFolder, managedSubfolders);
        }
        deleteManifestFiles(itemFolder, MANIFEST_FILE_NAME);
    }

    @Override
    public List<ResourcePackage> listAvailablePackages() {
        return listAvailablePackages(
                RESOURCES_DIR, appConfigService.getSelectedGameBase(), MANIFEST_FILE_NAME);
    }

    @Override
    public void installPackage(String profileId, List<String> disabledManagedSubfolders)
            throws IOException {
        Path destination = getGameDataDir();
        if (destination == null) {
            throw new IllegalStateException("No game installation folder is selected.");
        }
        ResourcePackage selected = findSelectedProfile(profileId, listAvailablePackages());

        clearInstalledPackage();
        copyDirectoryContents(selected.getSource(), destination);

        if (disabledManagedSubfolders != null && !disabledManagedSubfolders.isEmpty()) {
            manageInstalledProfile(profileId, disabledManagedSubfolders);
        }
    }

    public void manageInstalledProfile(String profileId, List<String> disabledManagedSubfolders)
            throws IOException {
        Path destination = getGameDataDir();
        if (destination == null) {
            throw new IllegalStateException("No game installation folder is selected.");
        }

        Path manifest = resolveManifestPath(destination, MANIFEST_FILE_NAME);
        if (!Files.exists(manifest)) {
            throw new IllegalStateException("No installed profile found.");
        }

        List<String> managedSubfolders = readManifestManagedSubfolders(manifest);
        if (managedSubfolders == null || managedSubfolders.isEmpty()) {
            throw new IllegalStateException("Profile has no managed subfolders.");
        }

        disabledManagedSubfolders =
                disabledManagedSubfolders != null ? disabledManagedSubfolders : List.of();
        Set<String> normalizedDisabled =
                disabledManagedSubfolders.stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(value -> !value.isEmpty())
                        .map(value -> value.toLowerCase())
                        .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        for (String subfolder : managedSubfolders) {
            if (subfolder == null || subfolder.isBlank()) continue;
            Path relative = Paths.get(subfolder).normalize();
            if (relative.isAbsolute() || relative.startsWith("..")) continue;
            Path target = destination.resolve(relative).normalize();
            if (!target.startsWith(destination)) continue;

            Path parent = target.getParent();
            String targetName = target.getFileName().toString();
            if (parent == null) continue;

            Path disabledPath = parent.resolve("disabled_" + targetName);
            boolean shouldBeDisabled = normalizedDisabled.contains(subfolder.trim().toLowerCase());
            boolean isCurrentlyDisabled = Files.exists(disabledPath) && Files.isDirectory(disabledPath);

            if (shouldBeDisabled && !isCurrentlyDisabled) {
                // Disable: rename folder to disabled_*
                if (Files.exists(target) && Files.isDirectory(target)) {
                    moveDirectoryReplacingExisting(target, disabledPath);
                    log.info("Disabled managed subfolder: " + target.toAbsolutePath());
                }
            } else if (!shouldBeDisabled && isCurrentlyDisabled) {
                // Enable: rename folder back from disabled_*
                if (Files.exists(disabledPath) && Files.isDirectory(disabledPath)) {
                    moveDirectoryReplacingExisting(disabledPath, target);
                    log.info("Enabled managed subfolder: " + disabledPath.toAbsolutePath());
                }
            }
        }
    }

    private void moveDirectoryReplacingExisting(Path source, Path target) throws IOException {
        if (source == null || target == null) {
            return;
        }
        if (Files.exists(target)) {
            if (!Files.isDirectory(target)) {
                Files.deleteIfExists(target);
            } else {
                deleteDirectoryContents(target);
                Files.deleteIfExists(target);
            }
        }
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public ResourcePackage getInstalledPackageInfo() {
        Path itemFolder = getGameDataDir();
        if (itemFolder == null || !Files.exists(itemFolder)) {
            return null;
        }

        Path manifest = resolveManifestPath(itemFolder, MANIFEST_FILE_NAME);
        if (manifest == null || !Files.isRegularFile(manifest)) {
            return null;
        }
        ResourcePackage resourcePackage =
                ResourcePackage.builder()
                        .id(readManifestName(manifest))
                        .name(readManifestName(manifest))
                        .author(readManifestAuthor(manifest))
                        .description(readManifestDescription(manifest))
                        .url(readManifestUrl(manifest))
                        .createdAt(readManifestCreatedAt(manifest))
                        .version(readManifestVersion(manifest))
                        .previewImages(loadPreviewImages(manifest))
                        .source(manifest)
                        .managedSubfolders(readManifestManagedSubfolders(manifest))
                        .disabledManagedSubfolders(
                                readManifestDisabledManagedSubfolders(
                                        itemFolder, readManifestManagedSubfolders(manifest)))
                        .build();

        return resourcePackage;
    }

    @Override
    public ResourcesUpdater.ResourcesUpdateCheckResult checkResourcesUpdate() {
        return resourcesUpdater.checkResourcesUpdate(DEFAULT_REPO, RESOURCES_DIR);
    }
}
