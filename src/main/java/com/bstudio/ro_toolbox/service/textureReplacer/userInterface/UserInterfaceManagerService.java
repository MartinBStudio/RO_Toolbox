package com.bstudio.ro_toolbox.service.textureReplacer.userInterface;

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
public class UserInterfaceManagerService implements GameResourceService, ICommonMethods {
  private static final String DEFAULT_REPO =
      "https://github.com/MartinBStudio/RO_UserInterface_resources";
  private static final String MANIFEST_FILE_NAME = "manifestUi.json";
  private static final String LEGACY_MANIFEST_FILE_NAME = "manifest.json";
  private static final Path APP_DATA_ROOT = AppDataPaths.resolveRoToolboxAppDataRoot();
  private static final Path RESOURCES_DIR =
      APP_DATA_ROOT.resolve("resources").resolve("userInterface");
  private static final Path GAME_SUFFIX = Paths.get("");

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
        DEFAULT_REPO, RESOURCES_DIR, "RO_UserInterfaceManager/1.0", log::info);
  }

  public void clearDownloadedPackages() throws IOException {
    clearResources(RESOURCES_DIR);
  }

  public void clearInstalledPackage() throws IOException {
    Path gameBase = appConfigService.getSelectedGameBase();
    if (gameBase == null || !Files.exists(gameBase) || !Files.isDirectory(gameBase)) return;

    // Always clear the installed manifest so the app no longer shows the profile as installed
    deleteManifestFiles(gameBase, MANIFEST_FILE_NAME, LEGACY_MANIFEST_FILE_NAME);

    // Restore original files from .default if available
    Path defaultProfile = RESOURCES_DIR.resolve(".default");
    if (!Files.exists(defaultProfile) || !Files.isDirectory(defaultProfile)) return;

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

  private Path resolveManifestPath(Path directory) {
    return directory.resolve(MANIFEST_FILE_NAME);
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

    Path manifest = resolveManifestPath(itemFolder);
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
            .build();

    return resourcePackage;
  }

  public ResourcesUpdater.ResourcesUpdateCheckResult checkResourcesUpdate() {
    return resourcesUpdater.checkResourcesUpdate(DEFAULT_REPO, RESOURCES_DIR);
  }
}
