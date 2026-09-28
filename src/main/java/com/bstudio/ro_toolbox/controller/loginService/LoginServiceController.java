package com.bstudio.ro_toolbox.controller.loginService;

import com.bstudio.ro_toolbox.controller.resourceReplacer.model.MessageResponse;
import com.bstudio.ro_toolbox.service.app.AppNotificationService;
import com.bstudio.ro_toolbox.service.loginManager.LoginManagerService;
import com.bstudio.ro_toolbox.util.DesktopFolderOpener;
import com.bstudio.ro_toolbox.util.WindowsProcessLauncher;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/login")
@RequiredArgsConstructor
public class LoginServiceController {

  private final LoginManagerService loginManagerService;
  private final AppNotificationService appNotificationService;

  @GetMapping
  public List<LoginManagerService.LoginAccount> listAccounts() throws IOException {
    return loginManagerService.listAccounts();
  }

  @GetMapping("/quick")
  public List<LoginManagerService.LoginAccount> listQuickAccounts() throws IOException {
    return loginManagerService.listQuickAccounts();
  }

  @GetMapping("/onedrive-backup/status")
  public LoginManagerService.OneDriveBackupStatus oneDriveBackupStatus() {
    return loginManagerService.readOneDriveBackupStatus();
  }

  @PostMapping("/onedrive-backup")
  public LoginManagerService.OneDriveBackupResult backupToOneDrive() throws IOException {
    return loginManagerService.backupToOneDrive();
  }

  @PostMapping("/onedrive-backup/folder/open")
  public MessageResponse openOneDriveBackupFolder() throws IOException {
    Path backupRoot = loginManagerService.getOneDriveBackupRoot();
    Files.createDirectories(backupRoot);
    DesktopFolderOpener.openInDesktop(backupRoot);
    return MessageResponse.builder().message("Opened OneDrive backup folder.").build();
  }

  @PostMapping("/onedrive-backup/restore")
  public LoginManagerService.OneDriveRestoreResult restoreFromOneDrive(
      @RequestBody RestoreOneDriveBackupRequest request) throws IOException {
    if (request == null || request.backupName() == null) {
      throw new IllegalArgumentException("backupName is required.");
    }
    return loginManagerService.restoreFromOneDriveBackup(request.backupName());
  }

  @PostMapping("/onedrive-backup/restore/latest")
  public LoginManagerService.OneDriveRestoreResult restoreLatestFromOneDrive() throws IOException {
    return loginManagerService.restoreLatestBackup();
  }

  @PostMapping("/onedrive-backup/cleanup")
  public LoginManagerService.BackupCleanupResult cleanupOneDriveBackups() throws IOException {
    return loginManagerService.deleteAllButLatestBackup();
  }

  @PostMapping("/onedrive-backup/cleanup/all")
  public LoginManagerService.BackupCleanupResult cleanupAllOneDriveBackups() throws IOException {
    return loginManagerService.deleteAllBackups();
  }

  @PostMapping("/{id}/launch")
  public MessageResponse launchAccount(
      @PathVariable String id,
      @RequestHeader(value = "X-RO-Toolbox-Source", required = false) String source)
      throws IOException {
    LoginManagerService.LoginAccount account =
        loginManagerService.listAccounts().stream()
            .filter(item -> id.equals(item.id()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Account not found: " + id));

    Path selectedGame = resolveSelectedGame();
    Path executable = selectedGame.resolve("trose.exe");
    if (!Files.exists(executable) || !Files.isRegularFile(executable)) {
      throw new IllegalStateException("ROSE executable was not found in the selected game folder.");
    }

    launchWindowsForeground(
        selectedGame,
        executable.toAbsolutePath().toString(),
        "--login",
        "--server",
        "connect.roseonlinegame.com",
        "--username",
        account.email(),
        "--password",
        account.password());

    if ("taskbar".equalsIgnoreCase(source)) {
      appNotificationService.enqueue("ROSE Online launched for " + account.name() + ".");
    }

    return MessageResponse.builder()
        .message("Launching ROSE Online for " + account.name() + ".")
        .build();
  }

  @PostMapping
  public LoginManagerService.LoginAccount createAccount(
      @RequestBody LoginManagerService.CreateAccountRequest request) throws IOException {
    return loginManagerService.createAccount(request);
  }

  @PutMapping("/{id}")
  public LoginManagerService.LoginAccount updateAccount(
      @PathVariable String id, @RequestBody LoginManagerService.UpdateAccountRequest request)
      throws IOException {
    return loginManagerService.updateAccount(id, request);
  }

  @DeleteMapping("/{id}")
  public LoginManagerService.LoginAccount deleteAccount(@PathVariable String id)
      throws IOException {
    return loginManagerService.deleteAccount(id);
  }

  private Path resolveSelectedGame() throws IOException {
    String appData = System.getenv("APPDATA");
    Path configDir =
        appData != null && !appData.isBlank()
            ? Path.of(appData, "RO_Toolbox", "config")
            : Path.of(System.getProperty("user.home"), ".ro_toolbox", "config");

    Path configFile = configDir.resolve("config.properties");
    if (!Files.exists(configFile)) {
      throw new IllegalStateException("No game installation folder is selected.");
    }

    Properties properties = new Properties();
    try (InputStream input = Files.newInputStream(configFile)) {
      properties.load(input);
    }

    String selectedGame = properties.getProperty("selectedGame");
    if (selectedGame == null || selectedGame.isBlank()) {
      throw new IllegalStateException("No game installation folder is selected.");
    }

    Path selected = Path.of(selectedGame).normalize();
    if (Files.exists(selected) && Files.isDirectory(selected)) {
      return selected;
    }

    throw new IllegalStateException("No valid game installation folder is selected.");
  }

  public record RestoreOneDriveBackupRequest(String backupName) {}

  private void launchWindowsForeground(
      Path workingDirectory, String executablePath, String... arguments) throws IOException {
    String os = System.getProperty("os.name", "").toLowerCase();
    if (os.contains("win")) {
      WindowsProcessLauncher.launchForeground(workingDirectory, executablePath, arguments);
      return;
    }

    String[] directCommand = new String[arguments.length + 1];
    directCommand[0] = executablePath;
    System.arraycopy(arguments, 0, directCommand, 1, arguments.length);
    new ProcessBuilder(directCommand).directory(workingDirectory.toFile()).start();
  }
}
