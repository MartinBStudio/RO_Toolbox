package com.bstudio.ro_toolbox.service.textureReplacer.lootModels;

import com.bstudio.ro_toolbox.service.app.AppConfigService;
import com.bstudio.ro_toolbox.service.common.ICommonMethods;
import com.bstudio.ro_toolbox.service.common.PackageHandler;
import com.bstudio.ro_toolbox.service.common.PackageManifestReader;
import com.bstudio.ro_toolbox.service.common.ResourcesUpdater;
import com.bstudio.ro_toolbox.service.textureReplacer.GameResourceService;
import com.bstudio.ro_toolbox.service.textureReplacer.ResourcePackage;
import com.bstudio.ro_toolbox.util.AppDataPaths;

import java.io.*;
import java.nio.file.*;
import java.util.List;

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
    private final PackageManifestReader packageManifestReader;
    private final PackageHandler packageHandler;

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
    public void runUpdate() throws IOException {
        resourcesUpdater.runUpdate(DEFAULT_REPO, RESOURCES_DIR);
    }

    @Override
    public void clearDownloaded() throws IOException {
        clearResources(RESOURCES_DIR);
    }

    @Override
    public void uninstallPackage() throws IOException {
        packageHandler.uninstallPackageFolders(getGameDataDir(), MANIFEST_FILE_NAME);
    }

    @Override
    public List<ResourcePackage> listPackages() {
        return packageHandler.listAvailablePackages(
                RESOURCES_DIR, appConfigService.getSelectedGameBase(), MANIFEST_FILE_NAME);
    }


    @Override
    public void installPackage(String profileId, List<String> disabledPackages) throws IOException {
        uninstallPackage();
        copyDirectoryContents(findSelectedPackage(profileId, listPackages()).getSource(), getGameDataDir());
        managePackage(profileId, disabledPackages);
    }

    @Override
    public void managePackage(String profileId, List<String> disabledManagedSubfolders)
            throws IOException {
        packageHandler.manageInstalledPackage(profileId, disabledManagedSubfolders, getGameDataDir(), MANIFEST_FILE_NAME);

    }

    @Override
    public ResourcePackage getStatus() {
        return packageManifestReader.readManifest(MANIFEST_FILE_NAME, getGameDataDir());
    }

    @Override
    public ResourcesUpdater.ResourcesUpdateCheckResult checkForUpdate() {
        return resourcesUpdater.checkResourcesUpdate(DEFAULT_REPO, RESOURCES_DIR);
    }
}
