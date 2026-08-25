package com.bstudio.ro_toolbox.web;

import com.bstudio.ro_toolbox.RoToolboxApplication;
import com.bstudio.ro_toolbox.service.lootModels.LootManagerService;
import com.bstudio.ro_toolbox.service.updater.UpdaterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ToolboxController {

    private final RoToolboxApplication app;
    private final LootManagerService lootManagerService;
    private final UpdaterService updaterService;

    @GetMapping("/status")
    public AppStatusResponse status() {
        LootManagerService.ProfileInfo installed = lootManagerService.getInstalledProfileInfo();
        return new AppStatusResponse(
                app.getVersion(),
                absoluteOrNull(lootManagerService.getSelectedGameBase()),
                absoluteOrNull(lootManagerService.getSelectedGameItemFolder()),
                installed == null ? null : new ProfileInfoResponse(
                        installed.name, installed.author, installed.description, installed.url, installed.createdAt
                ),
                lootManagerService.listDownloadedProfiles(),
                lootManagerService.listAvailableProfiles().stream()
                        .map(profile -> new AvailableProfileResponse(
                                profile.id(),
                                profile.name(),
                                profile.author(),
                                profile.description(),
                                profile.url(),
                                profile.createdAt()
                        ))
                        .toList()
        );
    }

    @PostMapping("/settings/game-folder")
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

    @PostMapping("/settings/game-folder/clear")
    public MessageResponse clearGameFolder() {
        lootManagerService.clearSelectedGame();
        return new MessageResponse("Selected game folder cleared.");
    }

    @PostMapping("/loot/download")
    public MessageResponse downloadProfiles() throws IOException {
        Path dest = lootManagerService.getResourcesDir();
        Files.createDirectories(dest);
        lootManagerService.downloadAndExtract(null, dest);
        return new MessageResponse("Profiles downloaded.");
    }

    @PostMapping("/loot/install")
    public MessageResponse installProfile(@RequestBody InstallProfileRequest request) throws IOException {
        if (request == null || request.profileId() == null || request.profileId().isBlank()) {
            throw new IllegalArgumentException("profileId is required.");
        }
        lootManagerService.installProfile(request.profileId().trim());
        return new MessageResponse("Profile installed: " + request.profileId().trim());
    }

    @PostMapping("/loot/clear-resources")
    public MessageResponse clearResources() throws IOException {
        lootManagerService.clearResources();
        return new MessageResponse("Downloaded resources cleared.");
    }

    @PostMapping("/loot/clear-installed")
    public MessageResponse clearInstalled() throws IOException {
        lootManagerService.clearSelectedItemFolder();
        lootManagerService.setCurrentLootProfile(null);
        return new MessageResponse("Installed models cleared.");
    }

    @PostMapping("/folders/open/resources")
    public MessageResponse openResourcesFolder() throws IOException {
        Path resources = lootManagerService.getResourcesDir();
        Files.createDirectories(resources);
        openInDesktop(resources);
        return new MessageResponse("Opened resources folder.");
    }

    @PostMapping("/folders/open/item")
    public MessageResponse openItemFolder() throws IOException {
        Path item = lootManagerService.getSelectedGameItemFolder();
        if (item == null) {
            throw new IllegalStateException("No game installation folder is selected.");
        }
        Files.createDirectories(item);
        openInDesktop(item);
        return new MessageResponse("Opened item folder.");
    }

    @GetMapping("/update/check")
    public UpdaterService.UpdateCheckResult checkForUpdate() {
        return updaterService.checkForUpdate();
    }

    @PostMapping("/update/install")
    public UpdaterService.UpdateInstallResult installUpdate() {
        return updaterService.installUpdate(updaterService.checkForUpdate());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBadRequest(IllegalArgumentException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleConflict(IllegalStateException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(IOException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleIo(IOException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    private String absoluteOrNull(Path path) {
        return path == null ? null : path.toAbsolutePath().normalize().toString();
    }

    private void openInDesktop(Path path) {
        try {
            if (!Desktop.isDesktopSupported()) {
                openWithSystemCommand(path);
                return;
            }
            if (Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(path.toFile());
                return;
            }
            openWithSystemCommand(path);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to open folder: " + path.toAbsolutePath(), ex);
        }
    }

    private void openWithSystemCommand(Path path) throws IOException {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            new ProcessBuilder("explorer.exe", path.toAbsolutePath().toString()).start();
            return;
        }
        if (os.contains("mac")) {
            new ProcessBuilder("open", path.toAbsolutePath().toString()).start();
            return;
        }
        new ProcessBuilder("xdg-open", path.toAbsolutePath().toString()).start();
    }

    public record SaveFolderRequest(String path, boolean forceSave) {}
    public record SaveFolderResponse(String selectedGameBase, String selectedGameItemFolder, boolean containsExpectedItemFolder) {}
    public record InstallProfileRequest(String profileId) {}
    public record MessageResponse(String message) {}
    public record ErrorResponse(String message) {}
    public record ProfileInfoResponse(String name, String author, String description, String url, String createdAt) {}
    public record AvailableProfileResponse(String id, String name, String author, String description, String url, String createdAt) {}
    public record AppStatusResponse(
            String version,
            String selectedGameBase,
            String selectedGameItemFolder,
            ProfileInfoResponse installedProfile,
            List<String> downloadedProfiles,
            List<AvailableProfileResponse> availableProfiles
    ) {}
}
