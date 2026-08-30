package com.bstudio.ro_toolbox.controller;

import com.bstudio.ro_toolbox.service.lootModels.LootManagerService;
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

    @PostMapping("/game-folder")
    public SaveFolderResponse saveGameFolder(@RequestBody SaveFolderRequest request) throws IOException {
        if (request == null || request.path() == null || request.path().isBlank()) {
            throw new IllegalArgumentException("Path is required.");
        }
        Path picked = Path.of(request.path().trim());
        Path base = picked.endsWith(Path.of("3ddata", "item"))
                ? picked.getParent() != null && picked.getParent().getParent() != null ? picked.getParent().getParent() : picked
                : picked;

        Files.createDirectories(base);
        lootManagerService.saveSelectedGame(base);
        Path itemFolder = base.resolve(Path.of("3ddata", "item"));
        boolean containsExpectedFolder = Files.exists(itemFolder) && Files.isDirectory(itemFolder);
        if (!containsExpectedFolder && !request.forceSave()) {
            throw new IllegalStateException("The selected folder does not contain 3ddata/item. Confirm save explicitly to continue.");
        }

        return new SaveFolderResponse(
                absoluteOrNull(base),
                absoluteOrNull(itemFolder),
                containsExpectedFolder
        );
    }

    @PostMapping("/game-folder/clear")
    public MessageResponse clearGameFolder() {
        lootManagerService.clearSelectedGame();
        return new MessageResponse("Selected game folder cleared.");
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
