package com.bstudio.ro_toolbox.service.textureReplacer.lootModels;

import com.bstudio.ro_toolbox.service.app.AppConfigService;
import com.bstudio.ro_toolbox.service.common.ICommonMethods;
import com.bstudio.ro_toolbox.service.common.ResourcesUpdater;
import com.bstudio.ro_toolbox.service.textureReplacer.AvailablePackage;
import com.bstudio.ro_toolbox.service.textureReplacer.GameResourceService;
import com.bstudio.ro_toolbox.util.AppDataPaths;
import com.bstudio.ro_toolbox.util.RepositoryZipDownloader;
import java.io.*;
import java.nio.file.*;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
@DependsOn("appConfigService")
public class LootManagerService implements GameResourceService, ICommonMethods {
  private static final String DEFAULT_REPO =
      "https://github.com/MartinBStudio/RO_LootFilter_resources";
  private static final String MANIFEST_FILE_NAME = "manifestLoot.json";
  private static final String LEGACY_MANIFEST_FILE_NAME = "manifest.json";
  private static final Path APP_DATA_ROOT = AppDataPaths.resolveRoToolboxAppDataRoot();
  private static final Path RESOURCES_DIR =
      APP_DATA_ROOT.resolve("resources").resolve("lootManager");
  private static final Path GAME_SUFFIX = Paths.get("3ddata", "item");

  private static final Path CONFIG_DIR = APP_DATA_ROOT.resolve("config");
  private static final Path CONFIG_FILE = CONFIG_DIR.resolve("config.properties");
  private static final String IGNORE_CONFIG_WARNINGS_KEY = "ignoreConfigWarnings";

  private final AppConfigService appConfigService;
  private final ResourcesUpdater resourcesUpdater;
  private volatile String currentLootProfile = null;

  public Path getResourcesDir() {
    return RESOURCES_DIR;
  }

  public Path getSelectedGameBase() {
    return appConfigService.getSelectedGameBase();
  }

  public Path getGameDataDir() {
    Path selectedGameBase = getSelectedGameBase();
    return (selectedGameBase == null) ? null : selectedGameBase.resolve(GAME_SUFFIX);
  }

  public boolean getIgnoreConfigWarnings() throws IOException {
    if (!Files.exists(CONFIG_FILE)) {
      return false;
    }
    Properties prop = new Properties();
    try (InputStream in = Files.newInputStream(CONFIG_FILE)) {
      prop.load(in);
    }
    return Boolean.parseBoolean(prop.getProperty(IGNORE_CONFIG_WARNINGS_KEY, "false").trim());
  }

  public void saveIgnoreConfigWarnings(boolean enabled) throws IOException {
    Files.createDirectories(CONFIG_DIR);
    Properties prop = new Properties();
    if (Files.exists(CONFIG_FILE)) {
      try (InputStream in = Files.newInputStream(CONFIG_FILE)) {
        prop.load(in);
      }
    }
    prop.setProperty(IGNORE_CONFIG_WARNINGS_KEY, String.valueOf(enabled));
    try (OutputStream out = Files.newOutputStream(CONFIG_FILE)) {
      prop.store(out, "RO LootManager config");
    }
  }

  public void downloadAndExtract() throws IOException {
    RepositoryZipDownloader.downloadAndExtract(
        DEFAULT_REPO, RESOURCES_DIR, "RO_LootManager/1.0", log::info);
  }

  public void clearResources() throws IOException {
    clearResources(RESOURCES_DIR);
  }

  public void clearSelectedItemFolder() throws IOException {
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

  public List<AvailablePackage> listAvailableProfiles() {
    return listAvailableProfiles(
        RESOURCES_DIR, appConfigService.getSelectedGameBase(), MANIFEST_FILE_NAME);
  }

  public void installProfile(String profileId, List<String> disabledManagedSubfolders)
      throws IOException {
    Path destination = getGameDataDir();
    if (destination == null) {
      throw new IllegalStateException("No game installation folder is selected.");
    }
    AvailablePackage selected = findAvailableProfile(profileId);

    clearSelectedItemFolder();
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

    Path manifest = resolveManifestPath(itemFolder, MANIFEST_FILE_NAME);
    if (manifest == null || !Files.isRegularFile(manifest)) {
      return null;
    }
    AvailablePackage availablePackage =
        AvailablePackage.builder()
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

    return availablePackage;
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

      Path disabledTarget = resolveDisabledManagedSubfolderPath(target);
      if (disabledTarget != null
          && Files.exists(disabledTarget)
          && Files.isDirectory(disabledTarget)) {
        deleteDirectoryContents(disabledTarget);
        Files.deleteIfExists(disabledTarget);
        log.info("Deleted disabled managed subfolder: " + disabledTarget.toAbsolutePath());
      }
    }
  }

  public ResourcesUpdater.ResourcesUpdateCheckResult checkResourcesUpdate() {
    return resourcesUpdater.checkResourcesUpdate(DEFAULT_REPO, RESOURCES_DIR);
  }
}
