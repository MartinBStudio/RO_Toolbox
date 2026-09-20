package com.bstudio.ro_toolbox.service.resourceReplacer.service.loot;

import com.bstudio.ro_toolbox.service.resourceReplacer.component.ResourcesUpdater;
import com.bstudio.ro_toolbox.service.resourceReplacer.model.Resource;
import com.bstudio.ro_toolbox.service.resourceReplacer.service.BaseIResourceReplacerResource;
import com.bstudio.ro_toolbox.util.AppDataPaths;
import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class LootManager extends BaseIResourceReplacerResource {
  private static final String DEFAULT_REPO =
      "https://github.com/MartinBStudio/RO_LootFilter_resources";
  private static final String MANIFEST_FILE_NAME = "manifestLoot.json";
  private static final Path RESOURCES_DIR =
      AppDataPaths.resolveRoToolboxAppDataRoot().resolve("resources").resolve("lootManager");
  private static final Path GAME_DATA_DIR = Paths.get("3ddata", "item");

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

  public int scaleManagedModelFolder(String managedSubfolder, float factor) throws IOException {
    if (managedSubfolder == null || managedSubfolder.isBlank()) {
      throw new IllegalArgumentException("folder is required.");
    }
    if (factor <= 0.0f || !Float.isFinite(factor)) {
      throw new IllegalArgumentException("factor must be a positive finite number.");
    }

    Path itemFolder = getGameDataDir();
    if (itemFolder == null) {
      throw new IllegalStateException("No game installation folder is selected.");
    }

    Path scanFolder = resolveManagedOrDisabledFolder(itemFolder, managedSubfolder);
    if (scanFolder == null || !Files.isDirectory(scanFolder)) {
      throw new IllegalArgumentException("Managed folder is not installed: " + managedSubfolder);
    }
    return ZmsScaleScanner.scaleFolder(scanFolder, factor);
  }

  public int scaleManagedModelFolderToOriginalRatio(String managedSubfolder, float targetRatio)
      throws IOException {
    if (managedSubfolder == null || managedSubfolder.isBlank()) {
      throw new IllegalArgumentException("folder is required.");
    }
    if (targetRatio <= 0.0f || !Float.isFinite(targetRatio)) {
      throw new IllegalArgumentException("target ratio must be a positive finite number.");
    }

    Path itemFolder = getGameDataDir();
    if (itemFolder == null) {
      throw new IllegalStateException("No game installation folder is selected.");
    }

    Path scanFolder = resolveManagedOrDisabledFolder(itemFolder, managedSubfolder);
    if (scanFolder == null || !Files.isDirectory(scanFolder)) {
      throw new IllegalArgumentException("Managed folder is not installed: " + managedSubfolder);
    }

    Path originalPackageFolder = resolveInstalledPackageSource();
    Path originalFolder = resolveOriginalManagedFolder(originalPackageFolder, managedSubfolder);
    if (originalFolder == null || !Files.isDirectory(originalFolder)) {
      throw new IllegalArgumentException(
          "Original package folder was not found for: " + managedSubfolder);
    }

    LootModelScaleReport.File currentFile =
        findLargestReasonableScaleFile(ZmsScaleScanner.scanFolder(scanFolder, itemFolder));
    LootModelScaleReport.File originalFile =
        findLargestReasonableScaleFile(
            ZmsScaleScanner.scanFolder(originalFolder, originalPackageFolder));
    float currentSize = currentFile.vertexBounds().largestAxis();
    float originalSize = originalFile.vertexBounds().largestAxis();
    if (currentSize <= 0.0f || originalSize <= 0.0f) {
      throw new IllegalStateException("Unable to calculate current model scale.");
    }

    float currentRatio = currentSize / originalSize;
    float factor = targetRatio / currentRatio;
    return scaleManagedModelFolder(managedSubfolder, factor);
  }

  public int resetManagedModelFolderScale(String managedSubfolder) throws IOException {
    if (managedSubfolder == null || managedSubfolder.isBlank()) {
      throw new IllegalArgumentException("folder is required.");
    }

    Path itemFolder = getGameDataDir();
    if (itemFolder == null) {
      throw new IllegalStateException("No game installation folder is selected.");
    }

    Path targetFolder = resolveManagedOrDisabledFolder(itemFolder, managedSubfolder);
    if (targetFolder == null || !Files.isDirectory(targetFolder)) {
      throw new IllegalArgumentException("Managed folder is not installed: " + managedSubfolder);
    }

    Path originalPackageFolder = resolveInstalledPackageSource();
    Path originalFolder = resolveOriginalManagedFolder(originalPackageFolder, managedSubfolder);
    if (originalFolder == null || !Files.isDirectory(originalFolder)) {
      throw new IllegalArgumentException(
          "Original package folder was not found for: " + managedSubfolder);
    }

    try (var files = Files.walk(originalFolder)) {
      List<Path> zmsFiles =
          files
              .filter(Files::isRegularFile)
              .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".zms"))
              .sorted(Comparator.comparing(path -> path.toString().toLowerCase()))
              .toList();
      int updated = 0;
      for (Path originalFile : zmsFiles) {
        Path relative = originalFolder.relativize(originalFile);
        Path targetFile = targetFolder.resolve(relative).normalize();
        if (!targetFile.startsWith(targetFolder)) {
          continue;
        }
        if (Files.isRegularFile(targetFile)) {
          Path backup = targetFile.resolveSibling(targetFile.getFileName() + ".bak");
          if (!Files.exists(backup)) {
            Files.copy(targetFile, backup);
          }
        }
        Files.createDirectories(targetFile.getParent());
        Files.copy(originalFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
        updated++;
      }
      return updated;
    }
  }

  public LootModelScaleReport scanInstalledModelScales() throws IOException {
    Path itemFolder = getGameDataDir();
    if (itemFolder == null) {
      throw new IllegalStateException("No game installation folder is selected.");
    }
    if (!Files.isDirectory(itemFolder)) {
      return new LootModelScaleReport(itemFolder.toAbsolutePath().toString(), List.of());
    }

    Path manifest = packageManifestReader.resolveManifestPath(itemFolder, MANIFEST_FILE_NAME);
    List<String> managedSubfolders = packageManifestReader.readManifestManagedSubfolders(manifest);
    if (managedSubfolders != null && !managedSubfolders.isEmpty()) {
      Path originalPackageFolder = resolveInstalledPackageSource();
      List<LootModelScaleReport.Folder> folders = new ArrayList<>();
      for (String managedSubfolder : managedSubfolders) {
        if (managedSubfolder == null || managedSubfolder.isBlank()) {
          continue;
        }
        folders.add(scanManagedScaleFolder(itemFolder, originalPackageFolder, managedSubfolder));
      }
      return new LootModelScaleReport(itemFolder.toAbsolutePath().toString(), folders);
    }

    try (var children = Files.list(itemFolder)) {
      List<LootModelScaleReport.Folder> folders =
          children
              .filter(Files::isDirectory)
              .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase()))
              .map(folder -> scanScaleFolder(itemFolder, folder))
              .filter(folder -> !folder.files().isEmpty())
              .toList();
      return new LootModelScaleReport(itemFolder.toAbsolutePath().toString(), folders);
    }
  }

  private LootModelScaleReport.Folder scanManagedScaleFolder(
      Path itemFolder, Path originalPackageFolder, String managedSubfolder) {
    Path scanFolder = resolveManagedOrDisabledFolder(itemFolder, managedSubfolder);
    if (scanFolder == null || !Files.isDirectory(scanFolder)) {
      return emptyScaleFolder(
          managedSubfolder, scanOriginalScaleFolder(originalPackageFolder, managedSubfolder));
    }

    try {
      return new LootModelScaleReport.Folder(
          managedSubfolder,
          ZmsScaleScanner.scanFolder(scanFolder, itemFolder),
          scanOriginalScaleFolder(originalPackageFolder, managedSubfolder));
    } catch (IOException ex) {
      return new LootModelScaleReport.Folder(
          managedSubfolder,
          List.of(
              new LootModelScaleReport.File(
                  managedSubfolder,
                  managedSubfolder,
                  null,
                  null,
                  0,
                  0,
                  null,
                  null,
                  ex.getMessage())),
          scanOriginalScaleFolder(originalPackageFolder, managedSubfolder));
    }
  }

  private List<LootModelScaleReport.File> scanOriginalScaleFolder(
      Path originalPackageFolder, String managedSubfolder) {
    Path originalFolder = resolveOriginalManagedFolder(originalPackageFolder, managedSubfolder);
    if (originalFolder == null || !Files.isDirectory(originalFolder)) {
      return List.of();
    }
    try {
      return ZmsScaleScanner.scanFolder(originalFolder, originalPackageFolder);
    } catch (IOException ex) {
      return List.of(
          new LootModelScaleReport.File(
              managedSubfolder, managedSubfolder, null, null, 0, 0, null, null, ex.getMessage()));
    }
  }

  private LootModelScaleReport.File findLargestReasonableScaleFile(
      List<LootModelScaleReport.File> files) {
    return files.stream()
        .filter(file -> file.error() == null)
        .filter(file -> file.vertexBounds() != null)
        .filter(file -> isReasonableSizeValue(file.vertexBounds().largestAxis()))
        .max(Comparator.comparingDouble(file -> file.vertexBounds().largestAxis()))
        .orElseThrow(() -> new IllegalStateException("No readable model files were found."));
  }

  private boolean isReasonableSizeValue(float value) {
    return Float.isFinite(value) && value >= 0.0f && value < 10_000.0f;
  }

  private Path resolveInstalledPackageSource() {
    Resource installed = getStatus();
    if (installed == null) {
      return null;
    }
    String installedName = installed.getName() == null ? "" : installed.getName().trim();
    return listPackages().stream()
        .filter(resource -> matchesInstalledPackage(resource, installedName))
        .map(Resource::getSource)
        .filter(path -> path != null && Files.isDirectory(path))
        .findFirst()
        .orElse(null);
  }

  private boolean matchesInstalledPackage(Resource resource, String installedName) {
    if (resource == null || installedName.isBlank()) {
      return false;
    }
    return installedName.equalsIgnoreCase(nullToBlank(resource.getName()))
        || installedName.equalsIgnoreCase(nullToBlank(resource.getId()))
        || installedName.equalsIgnoreCase(
            resource.getSource() == null ? "" : resource.getSource().getFileName().toString());
  }

  private String nullToBlank(String value) {
    return value == null ? "" : value.trim();
  }

  private Path resolveOriginalManagedFolder(Path originalPackageFolder, String managedSubfolder) {
    if (originalPackageFolder == null) {
      return null;
    }
    Path relative = Paths.get(managedSubfolder).normalize();
    if (relative.isAbsolute() || relative.startsWith("..")) {
      return null;
    }
    Path originalFolder = originalPackageFolder.resolve(relative).normalize();
    if (!originalFolder.startsWith(originalPackageFolder)) {
      return null;
    }
    return originalFolder;
  }

  private LootModelScaleReport.Folder scanScaleFolder(Path itemFolder, Path folder) {
    String folderName = normalizeManagedFolderName(folder.getFileName().toString());
    try {
      return new LootModelScaleReport.Folder(
          folderName, ZmsScaleScanner.scanFolder(folder, itemFolder), List.of());
    } catch (IOException ex) {
      return new LootModelScaleReport.Folder(
          folderName,
          List.of(
              new LootModelScaleReport.File(
                  folderName, folderName, null, null, 0, 0, null, null, ex.getMessage())),
          List.of());
    }
  }

  private LootModelScaleReport.Folder emptyScaleFolder(String folderName) {
    return new LootModelScaleReport.Folder(folderName, List.of(), List.of());
  }

  private LootModelScaleReport.Folder emptyScaleFolder(
      String folderName, List<LootModelScaleReport.File> originalFiles) {
    return new LootModelScaleReport.Folder(folderName, List.of(), originalFiles);
  }

  private Path resolveManagedOrDisabledFolder(Path itemFolder, String managedSubfolder) {
    Path relative = Paths.get(managedSubfolder).normalize();
    if (relative.isAbsolute() || relative.startsWith("..")) {
      return null;
    }

    Path target = itemFolder.resolve(relative).normalize();
    if (!target.startsWith(itemFolder)) {
      return null;
    }

    if (Files.isDirectory(target)) {
      return target;
    }
    return resolveDisabledManagedFolder(target);
  }

  private Path resolveDisabledManagedFolder(Path target) {
    Path parent = target.getParent();
    if (parent == null || target.getFileName() == null) {
      return null;
    }
    return parent.resolve("disabled_" + target.getFileName());
  }

  private String normalizeManagedFolderName(String folderName) {
    if (folderName != null && folderName.toLowerCase().startsWith("disabled_")) {
      return folderName.substring("disabled_".length());
    }
    return folderName;
  }
}
