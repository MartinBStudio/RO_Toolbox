package com.bstudio.ro_toolbox.controller;

import com.bstudio.ro_toolbox.service.buffIcons.BuffIconsManagerService;
import com.bstudio.ro_toolbox.util.DesktopFolderOpener;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/bufficons")
@RequiredArgsConstructor
public class BuffIconsServiceController {

    private final BuffIconsManagerService buffIconsManagerService;

    @GetMapping("/status")
    public BuffIconsStatusResponse status() {
        BuffIconsManagerService.ProfileInfo installed = buffIconsManagerService.getInstalledProfileInfo();
        return new BuffIconsStatusResponse(
                absoluteOrNull(buffIconsManagerService.getSelectedGameBase()),
                absoluteOrNull(buffIconsManagerService.getSelectedGameItemFolder()),
                installed == null ? null : new ProfileInfoResponse(
                        installed.name, installed.author, installed.description, installed.url, installed.createdAt, installed.version
                ),
                buffIconsManagerService.listDownloadedProfiles(),
                buffIconsManagerService.listAvailableProfiles().stream()
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
        Path dest = buffIconsManagerService.getResourcesDir();
        Files.createDirectories(dest);
        buffIconsManagerService.downloadAndExtract(null, dest);
        return new MessageResponse("Profiles downloaded.");
    }

    @PostMapping("/install")
    public MessageResponse installProfile(@RequestBody InstallProfileRequest request) throws IOException {
        if (request == null || request.profileId() == null || request.profileId().isBlank()) {
            throw new IllegalArgumentException("profileId is required.");
        }
        buffIconsManagerService.installProfile(request.profileId().trim());
        return new MessageResponse("Profile installed: " + request.profileId().trim());
    }

    @PostMapping("/clear-resources")
    public MessageResponse clearResources() throws IOException {
        buffIconsManagerService.clearResources();
        return new MessageResponse("Downloaded resources cleared.");
    }

    @PostMapping("/clear-installed")
    public MessageResponse clearInstalled() throws IOException {
        buffIconsManagerService.clearSelectedItemFolder();
        return new MessageResponse("Installed buff icons cleared.");
    }

    @GetMapping("/check-update")
    public BuffIconsManagerService.ResourcesUpdateCheckResult checkResourcesUpdate() {
        return buffIconsManagerService.checkResourcesUpdate();
    }

    @PostMapping("/folders/open/resources")
    public MessageResponse openResourcesFolder() throws IOException {
        Path resources = buffIconsManagerService.getResourcesDir();
        Files.createDirectories(resources);
        DesktopFolderOpener.openInDesktop(resources);
        return new MessageResponse("Opened resources folder.");
    }

    @PostMapping("/folders/open/item")
    public MessageResponse openItemFolder() throws IOException {
        Path item = buffIconsManagerService.getSelectedGameItemFolder();
        if (item == null) {
            throw new IllegalStateException("No game installation folder is selected.");
        }
        Files.createDirectories(item);
        DesktopFolderOpener.openInDesktop(item);
        return new MessageResponse("Opened item folder.");
    }

    private String absoluteOrNull(Path path) {
        return path == null ? null : path.toAbsolutePath().normalize().toString();
    }

    public record InstallProfileRequest(String profileId) {
    }

    public record MessageResponse(String message) {
    }

    public record ProfileInfoResponse(String name, String author, String description, String url, String createdAt, String version) {
    }

    public record AvailableProfileResponse(String id, String name, String author, String description, String url, String createdAt, String version, List<String> previewImages) {
    }

    public record BuffIconsStatusResponse(
            String selectedGameBase,
            String selectedGameItemFolder,
            ProfileInfoResponse installedProfile,
            List<String> downloadedProfiles,
            List<AvailableProfileResponse> availableProfiles
    ) {
    }
}
