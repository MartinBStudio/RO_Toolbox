package com.bstudio.ro_toolbox.controller;

import com.bstudio.ro_toolbox.service.combatText.CombatTextManagerService;
import com.bstudio.ro_toolbox.service.loginManager.LoginManagerService;
import com.bstudio.ro_toolbox.service.lootModels.LootManagerService;
import com.bstudio.ro_toolbox.service.userInterface.UserInterfaceManagerService;
import com.bstudio.ro_toolbox.util.WindowsProcessLauncher;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final LootManagerService lootManagerService;
    private final CombatTextManagerService combatTextManagerService;
    private final UserInterfaceManagerService userInterfaceManagerService;
    private final LoginManagerService loginManagerService;

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
        // Step 1: Clear installed profiles from game folder
        lootManagerService.clearSelectedItemFolder();
        combatTextManagerService.clearSelectedItemFolder();
        userInterfaceManagerService.clearSelectedItemFolder();

        // Step 2: Clear downloaded resources (.default is preserved for recovery)
        lootManagerService.clearResources();
        combatTextManagerService.clearResources();
        userInterfaceManagerService.clearResources();

        // Step 3: Clear game folder selection and app config
        lootManagerService.clearSelectedGame();
        combatTextManagerService.clearSelectedGame();
        userInterfaceManagerService.clearSelectedGame();

        lootManagerService.clearAppConfig();
        combatTextManagerService.clearAppConfig();
        userInterfaceManagerService.clearAppConfig();

        // Step 4: Clear all saved accounts
        loginManagerService.clearAccounts();

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

        Path executable = resolveQuickLaunchExecutable(gameBase);
        if (executable == null) {
            throw new IllegalStateException("No launchable ROSE executable was found in the selected game folder.");
        }

        launchWindowsForeground(gameBase, executable.toAbsolutePath().toString());

        return new MessageResponse("ROSE Online launched.");
    }

    static Path resolveQuickLaunchExecutable(Path gameBase) {
        if (gameBase == null) {
            return null;
        }

        Path normalized = gameBase.toAbsolutePath().normalize();
        Path updater = normalized.resolve("rose-updater.exe");
        if (Files.isRegularFile(updater)) {
            return updater;
        }

        Path trose = normalized.resolve("trose.exe");
        if (Files.isRegularFile(trose)) {
            return trose;
        }

        for (Path candidate = normalized; candidate != null; candidate = candidate.getParent()) {
            for (String exeName : new String[]{"rose-updater.exe", "trose.exe"}) {
                Path direct = candidate.resolve(exeName);
                if (Files.isRegularFile(direct)) {
                    return direct;
                }
            }
        }

        try (var stream = Files.walk(normalized, 3)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName() != null)
                    .filter(path -> path.getFileName().toString().equalsIgnoreCase("rose-updater.exe")
                            || path.getFileName().toString().equalsIgnoreCase("trose.exe"))
                    .findFirst()
                    .orElse(null);
        } catch (IOException ignored) {
            return null;
        }
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
        return new ReleaseNotesResponse(readReleaseNotesContent());
    }

    private String absoluteOrNull(Path path) {
        return path == null ? null : path.toAbsolutePath().normalize().toString();
    }

    private String readReleaseNotesContent() throws IOException {
        Path releaseNotes = resolveReleaseNotesPath();
        if (releaseNotes != null) {
            return Files.readString(releaseNotes);
        }

        try (InputStream resourceStream = SettingsController.class.getClassLoader().getResourceAsStream("RELEASE_NOTES.md")) {
            if (resourceStream != null) {
                return new String(resourceStream.readAllBytes(), StandardCharsets.UTF_8);
            }
        }

        throw new IllegalStateException("RELEASE_NOTES.md was not found.");
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

        return null;
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
