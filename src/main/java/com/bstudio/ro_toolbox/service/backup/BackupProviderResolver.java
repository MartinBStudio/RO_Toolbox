package com.bstudio.ro_toolbox.service.backup;

import com.bstudio.ro_toolbox.service.app.AppConfigService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class BackupProviderResolver {
  public static final String LOCAL_PROVIDER_ID = "local";
  private static final String BACKUP_FOLDER_NAME = "RO Toolbox backups";

  private final AppConfigService appConfigService;

  public BackupProviderResolver(AppConfigService appConfigService) {
    this.appConfigService = appConfigService;
  }

  public List<BackupProviderOption> listProviderOptions() {
    Path localPath = appConfigService.getBackupLocalPath();
    boolean localAvailable =
        localPath != null && Files.exists(localPath) && Files.isDirectory(localPath);
    return List.of(
        new BackupProviderOption(
            LOCAL_PROVIDER_ID,
            "Backup folder",
            localAvailable,
            localPath == null ? null : localPath.toString(),
            localAvailable));
  }

  public BackupProviderSettings readSettings() {
    return new BackupProviderSettings(
        LOCAL_PROVIDER_ID,
        Optional.ofNullable(appConfigService.getBackupLocalPath()).map(Path::toString).orElse(null),
        listProviderOptions());
  }

  public Optional<BackupProvider> resolveDefaultProvider() {
    Path localPath = appConfigService.getBackupLocalPath();
    if (localPath != null && Files.exists(localPath) && Files.isDirectory(localPath)) {
      return Optional.of(new BackupProvider(LOCAL_PROVIDER_ID, "Backup folder", localPath));
    }
    return Optional.empty();
  }

  public Path resolveBackupRoot(BackupProvider provider, String backupFolderName) {
    return resolveBackupRoot(provider).resolve(backupFolderName);
  }

  public Path resolveBackupRoot(BackupProvider provider) {
    return provider.root().resolve(BACKUP_FOLDER_NAME);
  }

  public record BackupProvider(String id, String name, Path root) {}

  public record BackupProviderOption(
      String id, String name, boolean available, String path, boolean selected) {}

  public record BackupProviderSettings(
      String selectedProviderId, String localPath, List<BackupProviderOption> providers) {}
}
