package com.bstudio.ro_toolbox.service.textureReplacer.combatText;

import com.bstudio.ro_toolbox.service.app.AppConfigService;
import com.bstudio.ro_toolbox.service.common.ICommonMethods;
import com.bstudio.ro_toolbox.service.common.ResourcesUpdater;
import com.bstudio.ro_toolbox.service.textureReplacer.GameResourceService;
import com.bstudio.ro_toolbox.service.textureReplacer.ResourcePackage;
import com.bstudio.ro_toolbox.util.AppDataPaths;
import com.bstudio.ro_toolbox.util.RepositoryZipDownloader;
import java.io.*;
import java.nio.file.*;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CombatTextManagerService implements GameResourceService, ICommonMethods {
  private static final String DEFAULT_REPO =
      "https://github.com/MartinBStudio/RO_CombatText_resources";
  private static final String MANIFEST_FILE_NAME = "manifestCombatText.json";
  private static final Path APP_DATA_ROOT = AppDataPaths.resolveRoToolboxAppDataRoot();
  private static final Path RESOURCES_DIR =
      APP_DATA_ROOT.resolve("resources").resolve("combatText");
  private static final Path GAME_SUFFIX = Paths.get("3ddata");

  private final AppConfigService appConfigService;
  private final ResourcesUpdater resourcesUpdater;

  public Path getResourcesDir() {
    return RESOURCES_DIR;
  }

  public Path getGameDataDir() {
    Path selectedGameBase = appConfigService.getSelectedGameBase();
    return (selectedGameBase == null) ? null : selectedGameBase.resolve(GAME_SUFFIX);
  }

  public void downloadAndExtract() throws IOException {
    RepositoryZipDownloader.downloadAndExtract(
        DEFAULT_REPO, RESOURCES_DIR, "RO_CombatTextManager/1.0", log::info);
  }

  public void clearDownloadedPackages() throws IOException {
    clearResources(RESOURCES_DIR);
  }

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

  public List<String> listDownloadedProfiles() {
    return listDownloadedProfiles(RESOURCES_DIR, MANIFEST_FILE_NAME);
  }

  public List<ResourcePackage> listAvailableProfiles() {
    return listAvailableProfiles(
        RESOURCES_DIR, appConfigService.getSelectedGameBase(), MANIFEST_FILE_NAME);
  }

  public void installPackage(String profileId, List<String> disabledPackages) throws IOException {
    Path destination = getGameDataDir();
    if (destination == null) {
      throw new IllegalStateException("No game installation folder is selected.");
    }
    ResourcePackage selected = findSelectedProfile(profileId, listAvailableProfiles());

    clearInstalledPackage();
    copyDirectoryContents(selected.getSource(), destination);
  }

  public ResourcePackage getInstalledPackageInfo() {
    Path itemFolder = getGameDataDir();
    if (itemFolder == null || !Files.exists(itemFolder)) {
      return null;
    }

    Path manifest = resolveManifestPath(itemFolder, MANIFEST_FILE_NAME);
    if (!Files.isRegularFile(manifest)) {
      return null;
    }
    return ResourcePackage.builder()
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
  }

  public ResourcesUpdater.ResourcesUpdateCheckResult checkResourcesUpdate() {
    return resourcesUpdater.checkResourcesUpdate(DEFAULT_REPO, RESOURCES_DIR);
  }
}
