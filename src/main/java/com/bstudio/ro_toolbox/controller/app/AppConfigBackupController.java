package com.bstudio.ro_toolbox.controller.app;

import com.bstudio.ro_toolbox.controller.resourceReplacer.model.MessageResponse;
import com.bstudio.ro_toolbox.service.app.AppConfigBackupService;
import com.bstudio.ro_toolbox.service.configEditor.ConfigEditorService;
import com.bstudio.ro_toolbox.service.loginManager.LoginManagerService;
import com.bstudio.ro_toolbox.util.DesktopFolderOpener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/app-config-backup")
@RequiredArgsConstructor
public class AppConfigBackupController {
  private final AppConfigBackupService appConfigBackupService;
  private final LoginManagerService loginManagerService;
  private final ConfigEditorService configEditorService;

  @GetMapping("/status")
  public AppConfigBackupService.BackupStatus status() {
    return appConfigBackupService.readBackupStatus();
  }

  @PostMapping
  public AppConfigBackupService.BackupResult backup(
      @RequestBody(required = false) CreateBackupRequest request) throws IOException {
    return appConfigBackupService.backup(request == null ? null : request.backupName());
  }

  @PostMapping("/folder/open")
  public MessageResponse openBackupFolder() throws IOException {
    Path backupRoot = appConfigBackupService.getBackupRoot();
    Files.createDirectories(backupRoot);
    DesktopFolderOpener.openInDesktop(backupRoot);
    return MessageResponse.builder().message("Opened backup folder.").build();
  }

  @PostMapping("/folder/open-backup")
  public MessageResponse openBackupSetFolder(@RequestBody BackupSetRequest request)
      throws IOException {
    if (request == null || request.backupName() == null) {
      throw new IllegalArgumentException("backupName is required.");
    }
    Path backupDir = appConfigBackupService.getBackupSetFolder(request.backupName());
    if (!Files.exists(backupDir) || !Files.isDirectory(backupDir)) {
      throw new IllegalArgumentException("Selected backup was not found.");
    }
    DesktopFolderOpener.openInDesktop(backupDir);
    return MessageResponse.builder().message("Opened backup folder.").build();
  }

  @PostMapping("/restore")
  public AppConfigBackupService.RestoreResult restore(@RequestBody RestoreBackupRequest request)
      throws IOException {
    if (request == null || request.backupName() == null) {
      throw new IllegalArgumentException("backupName is required.");
    }
    return appConfigBackupService.restore(request.backupName());
  }

  @PostMapping("/restore/backup")
  public BackupSetRestoreResult restoreBackupSet(@RequestBody BackupSetRequest request)
      throws IOException {
    if (request == null || request.backupName() == null) {
      throw new IllegalArgumentException("backupName is required.");
    }

    Path backupDir = appConfigBackupService.getBackupSetFolder(request.backupName());
    if (!Files.exists(backupDir) || !Files.isDirectory(backupDir)) {
      throw new IllegalArgumentException("Selected backup was not found.");
    }

    List<String> restoredSections = new ArrayList<>();
    if (Files.isDirectory(backupDir.resolve("Login Manager"))) {
      loginManagerService.restoreFromOneDriveBackup(request.backupName());
      restoredSections.add("Accounts");
    }
    if (Files.isDirectory(backupDir.resolve("ROSE Online Config"))) {
      configEditorService.restoreFromOneDriveBackup(request.backupName());
      restoredSections.add("ROSE config");
    }
    if (Files.isDirectory(backupDir.resolve("RO Toolbox Config"))) {
      appConfigBackupService.restore(request.backupName());
      restoredSections.add("RO Toolbox config");
    }

    if (restoredSections.isEmpty()) {
      throw new IllegalStateException("Selected backup has no restorable folders.");
    }

    return new BackupSetRestoreResult(request.backupName(), restoredSections);
  }

  @PostMapping("/restore/latest")
  public AppConfigBackupService.RestoreResult restoreLatest() throws IOException {
    return appConfigBackupService.restoreLatestBackup();
  }

  @PostMapping("/cleanup")
  public AppConfigBackupService.BackupCleanupResult cleanup() throws IOException {
    return appConfigBackupService.deleteAllButLatestBackup();
  }

  @PostMapping("/cleanup/all")
  public AppConfigBackupService.BackupCleanupResult cleanupAll() throws IOException {
    return appConfigBackupService.deleteAllBackups();
  }

  @PostMapping("/cleanup/backup")
  public AppConfigBackupService.BackupSetDeleteResult deleteBackupSet(
      @RequestBody BackupSetRequest request) throws IOException {
    if (request == null || request.backupName() == null) {
      throw new IllegalArgumentException("backupName is required.");
    }
    return appConfigBackupService.deleteBackupSet(request.backupName());
  }

  public record RestoreBackupRequest(String backupName) {}

  public record CreateBackupRequest(String backupName) {}

  public record BackupSetRequest(String backupName) {}

  public record BackupSetRestoreResult(String backupName, List<String> restoredSections) {}
}
