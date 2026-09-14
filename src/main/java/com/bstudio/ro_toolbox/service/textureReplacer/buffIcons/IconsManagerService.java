package com.bstudio.ro_toolbox.service.textureReplacer.buffIcons;

import com.bstudio.ro_toolbox.service.app.AppConfigService;
import com.bstudio.ro_toolbox.service.common.ICommonMethods;
import com.bstudio.ro_toolbox.service.common.PackageHandler;
import com.bstudio.ro_toolbox.service.common.PackageManifestReader;
import com.bstudio.ro_toolbox.service.common.ResourcesUpdater;
import com.bstudio.ro_toolbox.service.textureReplacer.GameResourceService;
import com.bstudio.ro_toolbox.service.textureReplacer.ResourcePackage;
import com.bstudio.ro_toolbox.util.AppDataPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class IconsManagerService implements GameResourceService, ICommonMethods {
    private static final String DEFAULT_REPO =
            "https://github.com/MartinBStudio/RO_BuffIcons_resources";
    private static final String MANIFEST_FILE_NAME = "manifestBuffIcons.json";

    private static final Path RESOURCES_DIR =
            AppDataPaths.resolveRoToolboxAppDataRoot().resolve("resources").resolve("buffIcons");

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
        return appConfigService.getSelectedGameBase();
    }

    @Override
    public void clearDownloaded() throws IOException {
        clearResources(RESOURCES_DIR);
    }

    @Override
    public void uninstallPackage() throws IOException {
        packageHandler.uninstallPackageFiles(appConfigService.getSelectedGameBase(), RESOURCES_DIR, MANIFEST_FILE_NAME);
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
    public List<ResourcePackage> listPackages() {
        return packageHandler.listAvailablePackages(
                RESOURCES_DIR, appConfigService.getSelectedGameBase(), MANIFEST_FILE_NAME);
    }

    @Override
    public ResourcePackage getStatus() {
        return packageManifestReader.readManifest(MANIFEST_FILE_NAME, getGameDataDir());
    }

    @Override
    public ResourcesUpdater.ResourcesUpdateCheckResult checkForUpdate() {
        return resourcesUpdater.checkResourcesUpdate(DEFAULT_REPO, RESOURCES_DIR);
    }

    @Override
    public void runUpdate() throws IOException {
        resourcesUpdater.runUpdate(DEFAULT_REPO, RESOURCES_DIR);
    }
}
