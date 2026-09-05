package com.bstudio.ro_toolbox.controller;

import com.bstudio.ro_toolbox.service.lootModels.LootManagerService;
import com.bstudio.ro_toolbox.util.WindowsProcessLauncher;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/loot")
@RequiredArgsConstructor
public class LootServiceController {

    private final LootManagerService lootManagerService;

    @GetMapping("/status")
    public LootStatusResponse status() {
        LootManagerService.ProfileInfo installed = lootManagerService.getInstalledProfileInfo();
        return new LootStatusResponse(
                absoluteOrNull(lootManagerService.getSelectedGameBase()),
                absoluteOrNull(lootManagerService.getSelectedGameItemFolder()),
                installed == null ? null : new ProfileInfoResponse(
                        installed.name, installed.author, installed.description, installed.url, installed.createdAt, installed.version,
                        installed.managedSubfolders, installed.disabledManagedSubfolders
                ),
                lootManagerService.listDownloadedProfiles(),
                lootManagerService.listAvailableProfiles().stream()
                        .map(profile -> new AvailableProfileResponse(
                                profile.id(),
                                profile.name(),
                                profile.author(),
                                profile.description(),
                                profile.url(),
                                profile.createdAt(),
                                profile.version(),
                                profile.managedSubfolders(),
                                profile.previewImages()
                        ))
                        .toList()
        );
    }

    @PostMapping("/download")
    public MessageResponse downloadProfiles() throws IOException {
        Path dest = lootManagerService.getResourcesDir();
        Files.createDirectories(dest);
        lootManagerService.downloadAndExtract(null, dest);
        return new MessageResponse("Profiles downloaded.");
    }

    @PostMapping("/install")
    public MessageResponse installProfile(@RequestBody InstallProfileRequest request) throws IOException {
        if (request == null || request.profileId() == null || request.profileId().isBlank()) {
            throw new IllegalArgumentException("profileId is required.");
        }
        lootManagerService.installProfile(request.profileId().trim(), request.disabledManagedSubfolders());
        return new MessageResponse("Profile installed: " + request.profileId().trim());
    }

    @PostMapping("/manage")
    public MessageResponse manageInstalledProfile(@RequestBody InstallProfileRequest request) throws IOException {
        if (request == null || request.profileId() == null || request.profileId().isBlank()) {
            throw new IllegalArgumentException("profileId is required.");
        }
        lootManagerService.manageInstalledProfile(request.profileId().trim(), request.disabledManagedSubfolders());
        return new MessageResponse("Managed folders updated.");
    }

    @PostMapping("/clear-resources")
    public MessageResponse clearResources() throws IOException {
        lootManagerService.clearResources();
        return new MessageResponse("Downloaded resources cleared.");
    }

    @PostMapping("/clear-installed")
    public MessageResponse clearInstalled() throws IOException {
        lootManagerService.clearSelectedItemFolder();
        lootManagerService.setCurrentLootProfile(null);
        return new MessageResponse("Installed models cleared.");
    }

    @GetMapping("/check-update")
    public LootManagerService.ResourcesUpdateCheckResult checkResourcesUpdate() {
        return lootManagerService.checkResourcesUpdate();
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

    private String absoluteOrNull(Path path) {
        return path == null ? null : path.toAbsolutePath().normalize().toString();
    }

    private void openInDesktop(Path path) {
        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("win")) {
                openWithSystemCommand(path);
                return;
            }
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
            WindowsProcessLauncher.openFolderForeground(path);
            return;
        }
        if (os.contains("mac")) {
            new ProcessBuilder("open", path.toAbsolutePath().toString()).start();
            return;
        }
        new ProcessBuilder("xdg-open", path.toAbsolutePath().toString()).start();
    }

    public record InstallProfileRequest(String profileId, List<String> disabledManagedSubfolders) {
    }

    public record MessageResponse(String message) {
    }

    public record ProfileInfoResponse(String name, String author, String description, String url, String createdAt, String version, List<String> managedSubfolders, List<String> disabledManagedSubfolders) {
    }

    public record AvailableProfileResponse(String id, String name, String author, String description, String url, String createdAt, String version, List<String> managedSubfolders, List<String> previewImages) {
    }

    public record LootStatusResponse(
            String selectedGameBase,
            String selectedGameItemFolder,
            ProfileInfoResponse installedProfile,
            List<String> downloadedProfiles,
            List<AvailableProfileResponse> availableProfiles
    ) {
    }
}
