package com.bstudio.ro_toolbox.controller;

import com.bstudio.ro_toolbox.service.combatText.CombatTextManagerService;
import com.bstudio.ro_toolbox.service.lootModels.LootManagerService;
import com.bstudio.ro_toolbox.service.userInterface.UserInterfaceManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final LootManagerService lootManagerService;
    private final CombatTextManagerService combatTextManagerService;
    private final UserInterfaceManagerService userInterfaceManagerService;

    @PostMapping("/game-folder")
    public SaveFolderResponse saveGameFolder(@RequestBody SaveFolderRequest request) throws IOException {
        if (request == null || request.path() == null || request.path().isBlank()) {
            throw new IllegalArgumentException("Path is required.");
        }
        Path picked = Path.of(request.path().trim());
        Path base = resolveGameBase(picked);

        Path itemFolder = base.resolve(Path.of("3ddata", "item"));
        Path troseExecutable = base.resolve("trose.exe");
        boolean containsExpectedFolder = Files.exists(itemFolder) && Files.isDirectory(itemFolder);
        boolean containsGameExecutable = Files.exists(troseExecutable) && Files.isRegularFile(troseExecutable);
        if (!containsGameExecutable) {
            throw new IllegalStateException("The selected folder is not valid. It must contain trose.exe.");
        }

        Files.createDirectories(base);
        lootManagerService.saveSelectedGame(base);
        combatTextManagerService.saveSelectedGame(base);
        userInterfaceManagerService.saveSelectedGame(base);

        return new SaveFolderResponse(
                absoluteOrNull(base),
                absoluteOrNull(itemFolder),
                containsExpectedFolder
        );
    }

    static Path resolveGameBase(Path picked) {
        Path normalized = picked == null ? null : picked.toAbsolutePath().normalize();
        if (normalized == null) {
            return null;
        }

        if (Files.isRegularFile(normalized.resolve("trose.exe"))) {
            return normalized;
        }

        for (Path candidate = normalized.getParent(); candidate != null; candidate = candidate.getParent()) {
            if (Files.isRegularFile(candidate.resolve("trose.exe"))) {
                return candidate;
            }
        }

        return normalized;
    }

    @PostMapping("/game-folder/clear")
    public MessageResponse clearGameFolder() {
        lootManagerService.clearSelectedGame();
        combatTextManagerService.clearSelectedGame();
        userInterfaceManagerService.clearSelectedGame();
        return new MessageResponse("Selected game folder cleared.");
    }

    @PostMapping("/factory-reset")
    public MessageResponse factoryReset() throws IOException {
        if (lootManagerService.getSelectedGameBase() != null) {
            lootManagerService.clearSelectedItemFolder();
        }
        if (combatTextManagerService.getSelectedGameBase() != null) {
            combatTextManagerService.clearSelectedItemFolder();
        }
        if (userInterfaceManagerService.getSelectedGameBase() != null) {
            userInterfaceManagerService.clearSelectedItemFolder();
        }

        lootManagerService.clearResources();
        combatTextManagerService.clearResources();
        userInterfaceManagerService.clearResources();

        lootManagerService.clearSelectedGame();
        combatTextManagerService.clearSelectedGame();
        userInterfaceManagerService.clearSelectedGame();

        lootManagerService.clearAppConfig();
        combatTextManagerService.clearAppConfig();
        userInterfaceManagerService.clearAppConfig();

        return new MessageResponse("Factory reset complete. RO_Toolbox app state was cleared.");
    }

    @PostMapping("/quick-launch")
    public MessageResponse quickLaunch() throws IOException {
        Path gameBase = lootManagerService.getSelectedGameBase();
        if (gameBase == null) {
            throw new IllegalStateException("No game installation folder is selected.");
        }

        String os = System.getProperty("os.name", "").toLowerCase();
        if (!os.contains("win")) {
            throw new IllegalStateException("Quick launch is supported only on Windows.");
        }

        Path executable = gameBase.resolve("rose-updater.exe");
        if (!Files.exists(executable) || !Files.isRegularFile(executable)) {
            throw new IllegalStateException("rose-updater.exe was not found in the selected game folder.");
        }

        new ProcessBuilder(executable.toAbsolutePath().toString())
                .directory(gameBase.toFile())
                .start();

        return new MessageResponse("ROSE Online launched.");
    }

    private String absoluteOrNull(Path path) {
        return path == null ? null : path.toAbsolutePath().normalize().toString();
    }

    public record SaveFolderRequest(String path, boolean forceSave) {
    }

    public record SaveFolderResponse(String selectedGameBase, String selectedGameItemFolder, boolean containsExpectedItemFolder) {
    }

    public record MessageResponse(String message) {
    }
}
