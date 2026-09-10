package com.bstudio.ro_toolbox.controller;

import com.bstudio.ro_toolbox.service.buffs.BuffsManagerService;
import com.bstudio.ro_toolbox.util.WindowsProcessLauncher;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/buffs")
@RequiredArgsConstructor
public class BuffsServiceController {

    private final BuffsManagerService buffsManagerService;

    @GetMapping("/status")
    public BuffsStatusResponse status() {
        BuffsManagerService.ProfileInfo installed = buffsManagerService.getInstalledProfileInfo();
        return new BuffsStatusResponse(
                absoluteOrNull(buffsManagerService.getSelectedGameBase()),
                absoluteOrNull(buffsManagerService.getSelectedGameItemFolder()),
                installed == null ? null : new ProfileInfoResponse(
                        installed.name, installed.author, installed.description, installed.url, installed.createdAt, installed.version
                ),
                buffsManagerService.listDownloadedProfiles(),
                buffsManagerService.listAvailableProfiles().stream()
                        .map(profile -> new AvailableProfileResponse(
                                profile.id(),
                                profile.name(),
                                profile.author(),
                                profile.description(),
                                profile.url(),
                                profile.createdAt(),
                                profile.version(),
                                profile.previewImages()
                        ))
                        .toList()
        );
    }

    @PostMapping("/download")
    public MessageResponse downloadProfiles() throws IOException {
        Path dest = buffsManagerService.getResourcesDir();
        Files.createDirectories(dest);
        buffsManagerService.downloadAndExtract(null, dest);
        return new MessageResponse("Profiles downloaded.");
    }

    @PostMapping("/install")
    public MessageResponse installProfile(@RequestBody InstallProfileRequest request) throws IOException {
        if (request == null || request.profileId() == null || request.profileId().isBlank()) {
            throw new IllegalArgumentException("profileId is required.");
        }
        buffsManagerService.installProfile(request.profileId().trim());
        return new MessageResponse("Profile installed: " + request.profileId().trim());
    }

    @PostMapping("/clear-resources")
    public MessageResponse clearResources() throws IOException {
        buffsManagerService.clearResources();
        return new MessageResponse("Downloaded resources cleared.");
    }

    @PostMapping("/clear-installed")
    public MessageResponse clearInstalled() throws IOException {
        buffsManagerService.clearSelectedItemFolder();
        return new MessageResponse("Installed buffs cleared.");
    }

    @GetMapping("/check-update")
    public BuffsManagerService.ResourcesUpdateCheckResult checkResourcesUpdate() {
        return buffsManagerService.checkResourcesUpdate();
    }

    @PostMapping("/folders/open/resources")
    public MessageResponse openResourcesFolder() throws IOException {
        Path resources = buffsManagerService.getResourcesDir();
        Files.createDirectories(resources);
        openInDesktop(resources);
        return new MessageResponse("Opened resources folder.");
    }

    @PostMapping("/folders/open/item")
    public MessageResponse openItemFolder() throws IOException {
        Path item = buffsManagerService.getSelectedGameItemFolder();
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

    public record InstallProfileRequest(String profileId) {
    }

    public record MessageResponse(String message) {
    }

    public record ProfileInfoResponse(String name, String author, String description, String url, String createdAt, String version) {
    }

    public record AvailableProfileResponse(String id, String name, String author, String description, String url, String createdAt, String version, List<String> previewImages) {
    }

    public record BuffsStatusResponse(
            String selectedGameBase,
            String selectedGameItemFolder,
            ProfileInfoResponse installedProfile,
            List<String> downloadedProfiles,
            List<AvailableProfileResponse> availableProfiles
    ) {
    }
}
