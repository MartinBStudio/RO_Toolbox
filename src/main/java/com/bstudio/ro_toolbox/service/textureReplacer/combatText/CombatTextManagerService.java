package com.bstudio.ro_toolbox.service.textureReplacer.combatText;

import com.bstudio.ro_toolbox.service.app.AppConfigService;
import com.bstudio.ro_toolbox.service.common.ICommonMethods;
import com.bstudio.ro_toolbox.service.common.ResourcesUpdater;
import com.bstudio.ro_toolbox.service.textureReplacer.AvailablePackage;
import com.bstudio.ro_toolbox.service.textureReplacer.GameResourceService;
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
@DependsOn("appConfigService")
public class CombatTextManagerService implements GameResourceService, ICommonMethods {
  private static final String DEFAULT_REPO =
      "https://github.com/MartinBStudio/RO_CombatText_resources";
  private static final String MANIFEST_FILE_NAME = "manifestCombatText.json";
  private static final String LEGACY_MANIFEST_FILE_NAME = "manifest.json";
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

  public void clearResources() throws IOException {
    clearResources(RESOURCES_DIR);
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

  public List<String> listDownloadedProfiles() {
    return listDownloadedProfiles(RESOURCES_DIR, MANIFEST_FILE_NAME);
  }

  public List<AvailablePackage> listAvailableProfiles() {
    return listAvailableProfiles(
        RESOURCES_DIR, appConfigService.getSelectedGameBase(), MANIFEST_FILE_NAME);
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
  }

  private AvailablePackage findAvailableProfile(String profileId) {
    String normalizedProfileId = profileId == null ? "" : profileId.trim();
    if (normalizedProfileId.isEmpty()) {
      throw new IllegalArgumentException("profileId is required.");
    }
    return listAvailableProfiles().stream()
        .filter(profile -> profile.getId().equals(normalizedProfileId))
        .findFirst()
        .orElseThrow(
            () -> new IllegalArgumentException("Profile not found: " + normalizedProfileId));
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
    return AvailablePackage.builder()
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

  private void deleteManagedSubfolders(Path baseDir, List<String> managedSubfolders)
      throws IOException {
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
