package com.bstudio.ro_toolbox.controller.app;

import com.bstudio.ro_toolbox.controller.resourceReplacer.model.MessageResponse;
import com.bstudio.ro_toolbox.service.app.AppConfigBackupService;
import com.bstudio.ro_toolbox.util.DesktopFolderOpener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/app-config-backup")
@RequiredArgsConstructor
public class AppConfigBackupController {
  private final AppConfigBackupService appConfigBackupService;

  @GetMapping("/status")
  public AppConfigBackupService.BackupStatus status() {
    return appConfigBackupService.readBackupStatus();
  }

  @PostMapping
  public AppConfigBackupService.BackupResult backup() throws IOException {
    return appConfigBackupService.backup();
  }

  @PostMapping("/folder/open")
  public MessageResponse openBackupFolder() throws IOException {
    Path backupRoot = appConfigBackupService.getBackupRoot();
    Files.createDirectories(backupRoot);
    DesktopFolderOpener.openInDesktop(backupRoot);
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

  public record RestoreBackupRequest(String backupName) {}
}
