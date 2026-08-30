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

        launchWindowsForeground(gameBase, executable.toAbsolutePath().toString());

        return new MessageResponse("ROSE Online launched.");
    }

    @GetMapping("/selected-service")
    public SelectedServiceResponse getSelectedService() throws IOException {
        return new SelectedServiceResponse(lootManagerService.getSelectedService());
    }

    @PostMapping("/selected-service")
    public SelectedServiceResponse saveSelectedService(@RequestBody SelectedServiceRequest request) throws IOException {
        if (request == null || request.serviceId() == null || request.serviceId().isBlank()) {
            throw new IllegalArgumentException("Service id is required.");
        }
        String serviceId = request.serviceId().trim();
        lootManagerService.saveSelectedService(serviceId);
        return new SelectedServiceResponse(serviceId);
    }

    @GetMapping("/release-notes")
    public ReleaseNotesResponse getReleaseNotes() throws IOException {
        Path releaseNotes = resolveReleaseNotesPath();
        return new ReleaseNotesResponse(Files.readString(releaseNotes));
    }

    private String absoluteOrNull(Path path) {
        return path == null ? null : path.toAbsolutePath().normalize().toString();
    }

    private Path resolveReleaseNotesPath() {
        Path workingDir = Path.of("").toAbsolutePath().normalize();
        Path releaseNotes = workingDir.resolve("RELEASE_NOTES.md");
        if (Files.isRegularFile(releaseNotes)) {
            return releaseNotes;
        }

        if (workingDir.getParent() != null) {
            Path parentReleaseNotes = workingDir.getParent().resolve("RELEASE_NOTES.md");
            if (Files.isRegularFile(parentReleaseNotes)) {
                return parentReleaseNotes;
            }
        }

        throw new IllegalStateException("RELEASE_NOTES.md was not found.");
    }

    public record SaveFolderRequest(String path, boolean forceSave) {
    }

    public record SaveFolderResponse(String selectedGameBase, String selectedGameItemFolder, boolean containsExpectedItemFolder) {
    }

    public record MessageResponse(String message) {
    }

    public record ReleaseNotesResponse(String content) {
    }

    public record SelectedServiceRequest(String serviceId) {
    }

    public record SelectedServiceResponse(String serviceId) {
    }

    private void launchWindowsForeground(Path workingDirectory, String executablePath, String... arguments) throws IOException {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            StringBuilder command = new StringBuilder();
            command.append("start \"\" \"").append(executablePath).append("\"");
            for (String argument : arguments) {
                command.append(" \"").append(argument).append("\"");
            }
            new ProcessBuilder("cmd.exe", "/c", command.toString())
                    .directory(workingDirectory.toFile())
                    .start();
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
