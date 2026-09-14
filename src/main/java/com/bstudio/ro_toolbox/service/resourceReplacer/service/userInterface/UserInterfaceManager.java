package com.bstudio.ro_toolbox.service.resourceReplacer.service.userInterface;

import com.bstudio.ro_toolbox.service.resourceReplacer.component.ResourcesUpdater;
import com.bstudio.ro_toolbox.service.resourceReplacer.model.Resource;
import com.bstudio.ro_toolbox.service.resourceReplacer.service.BaseIResourceReplacerResource;
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
public class UserInterfaceManager extends BaseIResourceReplacerResource {
  private static final String DEFAULT_REPO =
      "https://github.com/MartinBStudio/RO_UserInterface_resources";
  private static final String MANIFEST_FILE_NAME = "manifestUi.json";
  private static final Path RESOURCES_DIR =
      AppDataPaths.resolveRoToolboxAppDataRoot().resolve("resources").resolve("userInterface");

  @Override
  public Path getResourcesDir() {
    return RESOURCES_DIR;
  }

  @Override
  public Path getGameDataDir() {
    return appConfigService.getSelectedGameBase();
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
    packageHandler.uninstallPackageFiles(getGameDataDir(), RESOURCES_DIR, MANIFEST_FILE_NAME);
  }

  @Override
  public List<Resource> listPackages() {
    return packageHandler.listAvailablePackages(
        RESOURCES_DIR, appConfigService.getSelectedGameBase(), MANIFEST_FILE_NAME);
  }

  @Override
  public void installPackage(String profileId, List<String> disabledPackages) throws IOException {
    uninstallPackage();
    copyDirectoryContents(
        findSelectedPackage(profileId, listPackages()).getSource(), getGameDataDir());
    managePackage(profileId, disabledPackages);
  }

  @Override
  public void managePackage(String profileId, List<String> disabledManagedSubfolders)
      throws IOException {
    packageHandler.manageInstalledPackage(
        profileId, disabledManagedSubfolders, getGameDataDir(), MANIFEST_FILE_NAME);
  }

  @Override
  public Resource getStatus() {
    return packageManifestReader.readManifest(MANIFEST_FILE_NAME, getGameDataDir());
  }

  @Override
  public ResourcesUpdater.ResourcesUpdateCheckResult checkForUpdate() {
    return resourcesUpdater.checkResourcesUpdate(DEFAULT_REPO, RESOURCES_DIR);
  }
}
