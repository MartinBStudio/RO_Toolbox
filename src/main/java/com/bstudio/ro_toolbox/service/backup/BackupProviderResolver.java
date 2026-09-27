package com.bstudio.ro_toolbox.service.backup;

import com.bstudio.ro_toolbox.service.app.AppConfigService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BackupProviderResolver {
  public static final String AUTO_PROVIDER_ID = "auto";
  public static final String LOCAL_PROVIDER_ID = "local";

  private static final List<ProviderCandidate> CLOUD_PROVIDER_CANDIDATES =
      List.of(
          new ProviderCandidate(
              "onedrive",
              "OneDrive",
              List.of("OneDrive", "OneDriveConsumer", "OneDriveCommercial"),
              List.of()),
          new ProviderCandidate("dropbox", "Dropbox", List.of("Dropbox"), List.of("Dropbox")),
          new ProviderCandidate(
              "google-drive",
              "Google Drive",
              List.of("GoogleDrive", "GOOGLE_DRIVE"),
              List.of("Google Drive", "My Drive")),
          new ProviderCandidate(
              "icloud-drive",
              "iCloud Drive",
              List.of("iCloudDrive", "ICLOUD_DRIVE"),
              List.of("iCloudDrive")));

  private final AppConfigService appConfigService;

  public BackupProviderResolver(AppConfigService appConfigService) {
    this.appConfigService = appConfigService;
  }

  public List<BackupProviderOption> listProviderOptions() {
    List<BackupProviderOption> options = new ArrayList<>();
    String selectedProviderId = appConfigService.getBackupProviderId();
    options.add(
        new BackupProviderOption(
            AUTO_PROVIDER_ID,
            "Auto detect",
            true,
            null,
            AUTO_PROVIDER_ID.equals(selectedProviderId)));
    for (ProviderCandidate candidate : CLOUD_PROVIDER_CANDIDATES) {
      Optional<Path> root = resolveCandidateRoot(candidate);
      options.add(
          new BackupProviderOption(
              candidate.id(),
              candidate.name(),
              root.isPresent(),
              root.map(Path::toString).orElse(null),
              candidate.id().equals(selectedProviderId)));
    }

    Path localPath = appConfigService.getBackupLocalPath();
    boolean localAvailable =
        localPath != null && Files.exists(localPath) && Files.isDirectory(localPath);
    options.add(
        new BackupProviderOption(
            LOCAL_PROVIDER_ID,
            "Custom folder",
            localAvailable,
            localPath == null ? null : localPath.toString(),
            LOCAL_PROVIDER_ID.equals(selectedProviderId)));
    return options;
  }

  public BackupProviderSettings readSettings() {
    return new BackupProviderSettings(
        appConfigService.getBackupProviderId(),
        Optional.ofNullable(appConfigService.getBackupLocalPath()).map(Path::toString).orElse(null),
        listProviderOptions());
  }

  public Optional<BackupProvider> resolveDefaultProvider() {
    String selectedProviderId = appConfigService.getBackupProviderId();
    if (LOCAL_PROVIDER_ID.equals(selectedProviderId)) {
      Path localPath = appConfigService.getBackupLocalPath();
      if (localPath != null && Files.exists(localPath) && Files.isDirectory(localPath)) {
        return Optional.of(new BackupProvider(LOCAL_PROVIDER_ID, "Custom folder", localPath));
      }
      return Optional.empty();
    }

    if (!AUTO_PROVIDER_ID.equals(selectedProviderId)) {
      return CLOUD_PROVIDER_CANDIDATES.stream()
          .filter(candidate -> candidate.id().equals(selectedProviderId))
          .findFirst()
          .flatMap(
              candidate ->
                  resolveCandidateRoot(candidate).map(root -> toProvider(candidate, root)));
    }

    for (ProviderCandidate candidate : CLOUD_PROVIDER_CANDIDATES) {
      Optional<Path> root = resolveCandidateRoot(candidate);
      if (root.isPresent()) {
        return Optional.of(toProvider(candidate, root.get()));
      }
    }
    return Optional.empty();
  }

  public Path resolveBackupRoot(BackupProvider provider, String backupFolderName) {
    return provider.root().resolve("RO_Toolbox").resolve("Backups").resolve(backupFolderName);
  }

  private BackupProvider toProvider(ProviderCandidate candidate, Path root) {
    return new BackupProvider(candidate.id(), candidate.name(), root);
  }

  private Optional<Path> resolveCandidateRoot(ProviderCandidate candidate) {
    Optional<Path> envPath = resolveFirstExistingEnvPath(candidate.environmentVariables());
    if (envPath.isPresent()) {
      return envPath;
    }
    return candidate.relativeHomePaths().stream()
        .map(relativePath -> Paths.get(System.getProperty("user.home", ""), relativePath))
        .map(path -> path.toAbsolutePath().normalize())
        .filter(path -> Files.exists(path) && Files.isDirectory(path))
        .findFirst();
  }

  private Optional<Path> resolveFirstExistingEnvPath(List<String> environmentVariables) {
    return environmentVariables.stream()
        .map(System::getenv)
        .filter(value -> value != null && !value.isBlank())
        .map(Paths::get)
        .map(path -> path.toAbsolutePath().normalize())
        .filter(path -> Files.exists(path) && Files.isDirectory(path))
        .findFirst();
  }

  public record BackupProvider(String id, String name, Path root) {}

  public record BackupProviderOption(
      String id, String name, boolean available, String path, boolean selected) {}

  public record BackupProviderSettings(
      String selectedProviderId, String localPath, List<BackupProviderOption> providers) {}

  private record ProviderCandidate(
      String id, String name, List<String> environmentVariables, List<String> relativeHomePaths) {}
}
