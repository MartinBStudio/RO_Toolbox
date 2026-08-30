package com.bstudio.ro_toolbox.controller;

import com.bstudio.ro_toolbox.service.loginManager.LoginManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
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

        String[] command = new String[]{
                executable.toAbsolutePath().toString(),
                "--login",
                "--server",
                "connect.roseonlinegame.com",
                "--username",
                account.email(),
                "--password",
                account.password()
        };

        new ProcessBuilder(command)
                .directory(selectedGame.toFile())
                .start();

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
}
