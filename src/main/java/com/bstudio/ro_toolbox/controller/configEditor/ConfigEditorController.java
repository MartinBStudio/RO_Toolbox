package com.bstudio.ro_toolbox.controller.configEditor;

import com.bstudio.ro_toolbox.controller.resourceReplacer.BaseResourceReplacerController;
import com.bstudio.ro_toolbox.service.configEditor.ConfigEditorService;
import com.bstudio.ro_toolbox.util.DesktopFolderOpener;
import com.bstudio.ro_toolbox.util.WindowsProcessLauncher;
import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/config-editor")
@RequiredArgsConstructor
public class ConfigEditorController extends BaseResourceReplacerController {

  private final ConfigEditorService configEditorService;

  @GetMapping("/status")
  public ConfigEditorService.ConfigEditorStatus status() throws IOException {
    return configEditorService.readStatus();
  }

  @PostMapping("/files/{fileId}")
  public ConfigEditorService.ConfigFileState saveFile(
      @PathVariable String fileId, @RequestBody SaveFileRequest request) throws IOException {
    if (request == null || request.content() == null) {
      throw new IllegalArgumentException("content is required.");
    }
    return configEditorService.save(fileId, request.content());
  }

  @GetMapping("/ignore")
  public ConfigEditorService.IgnoreListState readIgnoreList() throws IOException {
    return configEditorService.readIgnoreList();
  }

  @GetMapping("/rose-settings")
  public ConfigEditorService.RoseConfigState readRoseConfig() throws IOException {
    return configEditorService.readRoseConfigState();
  }

  @PostMapping("/rose-settings/show-dropped-item-name")
  public ConfigEditorService.RoseConfigState setShowDroppedItemName(
      @RequestBody SetShowDroppedItemNameRequest request) throws IOException {
    if (request == null || request.enabled() == null) {
      throw new IllegalArgumentException("enabled is required.");
    }
    return configEditorService.setShowDroppedItemName(request.enabled());
  }

  @PostMapping("/ignore")
  public ConfigEditorService.IgnoreListState addIgnoreEntry(@RequestBody IgnoreEntryRequest request)
      throws IOException {
    if (request == null || request.name() == null) {
      throw new IllegalArgumentException("name is required.");
    }
    return configEditorService.addIgnoreName(request.name());
  }

  @DeleteMapping("/ignore")
  public ConfigEditorService.IgnoreListState deleteIgnoreEntry(
      @RequestBody IgnoreEntryRequest request) throws IOException {
    if (request == null || request.name() == null) {
      throw new IllegalArgumentException("name is required.");
    }
    return configEditorService.deleteIgnoreName(request.name());
  }

  @PostMapping("/folders/open")
  public MessageResponse openConfigFolder() throws IOException {
    Path configDir = configEditorService.getConfigDir();
    Files.createDirectories(configDir);
    openInDesktop(configDir);
    return new MessageResponse("Opened config folder.");
  }

  @PostMapping("/onedrive-backup")
  public ConfigEditorService.OneDriveBackupResult backupToOneDrive() throws IOException {
    return configEditorService.backupToOneDrive();
  }

  @PostMapping("/onedrive-backup/folder/open")
  public MessageResponse openOneDriveBackupFolder() throws IOException {
    Path backupRoot = configEditorService.getOneDriveBackupRoot();
    Files.createDirectories(backupRoot);
    DesktopFolderOpener.openInDesktop(backupRoot);
    return new MessageResponse("Opened OneDrive backup folder.");
  }

  @PostMapping("/onedrive-backup/restore")
  public ConfigEditorService.OneDriveRestoreResult restoreFromOneDrive(
      @RequestBody RestoreOneDriveBackupRequest request) throws IOException {
    if (request == null || request.backupName() == null) {
      throw new IllegalArgumentException("backupName is required.");
    }
    return configEditorService.restoreFromOneDriveBackup(request.backupName());
  }

  @PostMapping("/onedrive-backup/restore/latest")
  public ConfigEditorService.OneDriveRestoreResult restoreLatestFromOneDrive() throws IOException {
    return configEditorService.restoreLatestBackup();
  }

  @PostMapping("/onedrive-backup/cleanup")
  public ConfigEditorService.BackupCleanupResult cleanupOneDriveBackups() throws IOException {
    return configEditorService.deleteAllButLatestBackup();
  }

  @PostMapping("/onedrive-backup/cleanup/all")
  public ConfigEditorService.BackupCleanupResult cleanupAllOneDriveBackups() throws IOException {
    return configEditorService.deleteAllBackups();
  }

  private void openInDesktop(Path path) {
    try {
      String os = System.getProperty("os.name", "").toLowerCase();
      if (os.contains("win")) {
        openWithSystemCommand(path);
        return;
      }
      if (!Desktop.isDesktopSupported()) {
        openWithSystemCommand(path);
        return;
      }
      if (Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
        Desktop.getDesktop().open(path.toFile());
        return;
      }
      openWithSystemCommand(path);
    } catch (Exception ex) {
      throw new IllegalStateException("Unable to open folder: " + path.toAbsolutePath(), ex);
    }
  }

  private void openWithSystemCommand(Path path) throws IOException {
    String os = System.getProperty("os.name", "").toLowerCase();
    if (os.contains("win")) {
      WindowsProcessLauncher.openFolderForeground(path);
      return;
    }
    if (os.contains("mac")) {
      new ProcessBuilder("open", path.toAbsolutePath().toString()).start();
      return;
    }
    new ProcessBuilder("xdg-open", path.toAbsolutePath().toString()).start();
  }

  public record SaveFileRequest(String content) {}

  public record IgnoreEntryRequest(String name) {}

  public record SetShowDroppedItemNameRequest(Boolean enabled) {}

  public record RestoreOneDriveBackupRequest(String backupName) {}

  public record MessageResponse(String message) {}
}
