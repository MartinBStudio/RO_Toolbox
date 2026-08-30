package com.bstudio.ro_toolbox.controller;

import com.bstudio.ro_toolbox.service.loginManager.LoginManagerService;
import com.bstudio.ro_toolbox.util.WindowsProcessLauncher;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

@RestController
@RequestMapping("/api/login")
@RequiredArgsConstructor
public class LoginServiceController {

    private final LoginManagerService loginManagerService;

    @GetMapping
    public List<LoginManagerService.LoginAccount> listAccounts() throws IOException {
        return loginManagerService.listAccounts();
    }

    @GetMapping("/quick")
    public List<LoginManagerService.LoginAccount> listQuickAccounts() throws IOException {
        return loginManagerService.listQuickAccounts();
    }

    @GetMapping("/export")
    public LoginManagerService.ExportAccountsResponse exportAccounts() throws IOException {
        return loginManagerService.exportAccounts();
    }

    @PostMapping("/import")
    public ImportResponse importAccounts(@RequestBody LoginManagerService.ImportAccountsRequest request) throws IOException {
        List<LoginManagerService.LoginAccount> imported = loginManagerService.importAccounts(request);
        return new ImportResponse(imported.size(), "Accounts imported.");
    }

    @PostMapping("/export/save")
    public MessageResponse saveExport(@RequestBody SaveExportRequest request) throws IOException {
        if (request == null || request.filePath() == null || request.filePath().isBlank()) {
            throw new IllegalArgumentException("Export file path is required.");
        }
        if (request.content() == null || request.content().isBlank()) {
            throw new IllegalArgumentException("Export content is required.");
        }

        Path exportPath = Path.of(request.filePath().trim()).toAbsolutePath().normalize();
        Path parent = exportPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(exportPath, request.content(), StandardCharsets.UTF_8);
        return new MessageResponse("Accounts exported.");
    }

    @PostMapping("/{id}/launch")
    public MessageResponse launchAccount(@PathVariable String id) throws IOException {
        LoginManagerService.LoginAccount account = loginManagerService.listAccounts().stream()
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
                account.password()
        );

        return new MessageResponse("Launching ROSE Online for " + account.name() + ".");
    }

    @PostMapping
    public LoginManagerService.LoginAccount createAccount(@RequestBody LoginManagerService.CreateAccountRequest request) throws IOException {
        return loginManagerService.createAccount(request);
    }

    @PutMapping("/{id}")
    public LoginManagerService.LoginAccount updateAccount(
            @PathVariable String id,
            @RequestBody LoginManagerService.UpdateAccountRequest request
    ) throws IOException {
        return loginManagerService.updateAccount(id, request);
    }

    @DeleteMapping("/{id}")
    public LoginManagerService.LoginAccount deleteAccount(@PathVariable String id) throws IOException {
        return loginManagerService.deleteAccount(id);
    }

    private Path resolveSelectedGame() throws IOException {
        String appData = System.getenv("APPDATA");
        Path configDir = appData != null && !appData.isBlank()
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

    public record MessageResponse(String message) {
    }

    public record ImportResponse(int totalAccounts, String message) {
    }

    public record SaveExportRequest(String filePath, String content) {
    }

    private void launchWindowsForeground(Path workingDirectory, String executablePath, String... arguments) throws IOException {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            WindowsProcessLauncher.launchForeground(workingDirectory, executablePath, arguments);
            return;
        }

        String[] directCommand = new String[arguments.length + 1];
        directCommand[0] = executablePath;
        System.arraycopy(arguments, 0, directCommand, 1, arguments.length);
        new ProcessBuilder(directCommand)
                .directory(workingDirectory.toFile())
                .start();
    }
}
