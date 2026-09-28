package com.bstudio.ro_toolbox.service.app;

import com.bstudio.ro_toolbox.service.backup.BackupProviderResolver;
import com.bstudio.ro_toolbox.service.backup.BackupProviderResolver.BackupProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class AppConfigBackupService {
  private static final String CONFIG_FILE_NAME = "config.properties";
  private static final DateTimeFormatter BACKUP_TIMESTAMP_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

  private final AppConfigService appConfigService;
  private final BackupProviderResolver backupProviderResolver;

  public AppConfigBackupService(AppConfigService appConfigService) {
    this.appConfigService = appConfigService;
    this.backupProviderResolver = new BackupProviderResolver(appConfigService);
  }

  public BackupStatus readBackupStatus() {
    return backupProviderResolver
        .resolveDefaultProvider()
        .map(
            provider -> {
              Path backupRoot = resolveProviderBackupRoot(provider);
              return new BackupStatus(
                  true,
                  provider.name(),
                  provider.root().toString(),
                  backupRoot.toString(),
                  readBackups(backupRoot));
            })
        .orElseGet(() -> new BackupStatus(false, null, null, null, List.of()));
  }

  public Path getBackupRoot() {
    BackupProvider provider =
        backupProviderResolver
            .resolveDefaultProvider()
            .orElseThrow(() -> new IllegalStateException("No backup provider was detected."));
    return resolveProviderBackupRoot(provider);
  }

  public BackupResult backup() throws IOException {
    BackupProvider provider =
        backupProviderResolver
            .resolveDefaultProvider()
            .orElseThrow(() -> new IllegalStateException("No backup provider was detected."));
    Path backupRoot = resolveProviderBackupRoot(provider);
    Path configFile = appConfigService.getConfigFile();
    if (!Files.exists(configFile) || !Files.isRegularFile(configFile)) {
      return new BackupResult(backupRoot.toString(), 0, List.of());
    }

    Path backupDir = backupRoot.resolve(LocalDateTime.now().format(BACKUP_TIMESTAMP_FORMATTER));
    Files.createDirectories(backupDir);
    Files.copy(
        configFile,
        backupDir.resolve(CONFIG_FILE_NAME),
        StandardCopyOption.REPLACE_EXISTING,
        StandardCopyOption.COPY_ATTRIBUTES);

    return new BackupResult(backupDir.toString(), 1, List.of(CONFIG_FILE_NAME));
  }

  public BackupCleanupResult deleteAllButLatestBackup() throws IOException {
    BackupProvider provider =
        backupProviderResolver
            .resolveDefaultProvider()
            .orElseThrow(() -> new IllegalStateException("No backup provider was detected."));
    Path backupRoot = resolveProviderBackupRoot(provider);
    List<BackupEntry> backups = readBackups(backupRoot);
    int deleted = 0;
    for (int index = 1; index < backups.size(); index++) {
      deleteRecursively(Path.of(backups.get(index).path()));
      deleted++;
    }
    return new BackupCleanupResult(deleted, backups.isEmpty() ? null : backups.getFirst().name());
  }

  public BackupCleanupResult deleteAllBackups() throws IOException {
    BackupProvider provider =
        backupProviderResolver
            .resolveDefaultProvider()
            .orElseThrow(() -> new IllegalStateException("No backup provider was detected."));
    Path backupRoot = resolveProviderBackupRoot(provider);
    List<BackupEntry> backups = readBackups(backupRoot);
    int deleted = 0;
    for (BackupEntry backup : backups) {
      deleteRecursively(Path.of(backup.path()));
      deleted++;
    }
    return new BackupCleanupResult(deleted, null);
  }

  public RestoreResult restore(String backupName) throws IOException {
    String normalizedBackupName = normalizeBackupName(backupName);
    BackupProvider provider =
        backupProviderResolver
            .resolveDefaultProvider()
            .orElseThrow(() -> new IllegalStateException("No backup provider was detected."));
    Path backupRoot = resolveProviderBackupRoot(provider);
    Path backupDir = backupRoot.resolve(normalizedBackupName).toAbsolutePath().normalize();
    if (!backupDir.startsWith(backupRoot.toAbsolutePath().normalize())
        || !Files.exists(backupDir)
        || !Files.isDirectory(backupDir)) {
      throw new IllegalArgumentException("Selected app config backup was not found.");
    }

    Path backupConfigFile = backupDir.resolve(CONFIG_FILE_NAME);
    if (!Files.exists(backupConfigFile) || !Files.isRegularFile(backupConfigFile)) {
      throw new IllegalStateException("Selected backup has no app config file.");
    }

    Path safetyBackupDir =
        backupRoot.resolve("pre-restore-" + LocalDateTime.now().format(BACKUP_TIMESTAMP_FORMATTER));
    List<String> safetyFiles = backupExistingConfigFile(safetyBackupDir);

    Files.createDirectories(appConfigService.getConfigDir());
    Files.copy(
        backupConfigFile,
        appConfigService.getConfigFile(),
        StandardCopyOption.REPLACE_EXISTING,
        StandardCopyOption.COPY_ATTRIBUTES);

    return new RestoreResult(
        backupDir.toString(),
        1,
        List.of(CONFIG_FILE_NAME),
        safetyFiles.isEmpty() ? null : safetyBackupDir.toString(),
        safetyFiles);
  }

  public RestoreResult restoreLatestBackup() throws IOException {
    BackupProvider provider =
        backupProviderResolver
            .resolveDefaultProvider()
            .orElseThrow(() -> new IllegalStateException("No backup provider was detected."));
    Path backupRoot = resolveProviderBackupRoot(provider);
    List<BackupEntry> backups = readBackups(backupRoot);
    if (backups.isEmpty()) {
      throw new IllegalStateException("No RO Toolbox config backups were found.");
    }
    return restore(backups.getFirst().name());
  }

  private Path resolveProviderBackupRoot(BackupProvider provider) {
    return backupProviderResolver.resolveBackupRoot(provider, "RO Toolbox Config");
  }

  private List<BackupEntry> readBackups(Path backupRoot) {
    if (!Files.exists(backupRoot) || !Files.isDirectory(backupRoot)) {
      return List.of();
    }
    try (var entries = Files.list(backupRoot)) {
      return entries
          .filter(Files::isDirectory)
          .filter(path -> !path.getFileName().toString().startsWith("pre-restore-"))
          .map(this::readBackupEntry)
          .filter(Optional::isPresent)
          .map(Optional::get)
          .sorted((first, second) -> second.name().compareTo(first.name()))
          .toList();
    } catch (IOException ex) {
      return List.of();
    }
  }

  private Optional<BackupEntry> readBackupEntry(Path backupDir) {
    Path file = backupDir.resolve(CONFIG_FILE_NAME);
    if (!Files.isRegularFile(file)) {
      return Optional.empty();
    }
    return Optional.of(
        new BackupEntry(
            backupDir.getFileName().toString(), backupDir.toString(), List.of(CONFIG_FILE_NAME)));
  }

  private List<String> backupExistingConfigFile(Path safetyBackupDir) throws IOException {
    Path configFile = appConfigService.getConfigFile();
    if (!Files.exists(configFile) || !Files.isRegularFile(configFile)) {
      return List.of();
    }

    Files.createDirectories(safetyBackupDir);
    Files.copy(
        configFile,
        safetyBackupDir.resolve(CONFIG_FILE_NAME),
        StandardCopyOption.REPLACE_EXISTING,
        StandardCopyOption.COPY_ATTRIBUTES);
    return List.of(CONFIG_FILE_NAME);
  }

  private void deleteRecursively(Path path) throws IOException {
    if (!Files.exists(path)) {
      return;
    }
    try (var stream = Files.walk(path)) {
      for (Path entry : stream.sorted(Comparator.reverseOrder()).toList()) {
        try {
          Files.deleteIfExists(entry);
        } catch (IOException ex) {
          if (Files.exists(entry)) {
            throw ex;
          }
        }
      }
    }
  }

  private String normalizeBackupName(String backupName) {
    if (backupName == null || backupName.isBlank()) {
      throw new IllegalArgumentException("backupName is required.");
    }
    String normalized = backupName.trim();
    if (normalized.contains("/") || normalized.contains("\\") || normalized.equals("..")) {
      throw new IllegalArgumentException("backupName is invalid.");
    }
    return normalized;
  }

  public record BackupStatus(
      boolean available,
      String providerName,
      String oneDrivePath,
      String backupRootPath,
      List<BackupEntry> backups) {}

  public record BackupEntry(String name, String path, List<String> files) {}

  public record BackupResult(String backupPath, int copiedFiles, List<String> files) {}

  public record BackupCleanupResult(int deletedBackups, String keptBackupName) {}

  public record RestoreResult(
      String restoredFrom,
      int restoredFiles,
      List<String> files,
      String safetyBackupPath,
      List<String> safetyBackupFiles) {}
}
