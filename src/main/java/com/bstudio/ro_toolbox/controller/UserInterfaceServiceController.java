package com.bstudio.ro_toolbox.controller;

import com.bstudio.ro_toolbox.controller.model.InstallProfileRequest;
import com.bstudio.ro_toolbox.controller.model.MessageResponse;
import com.bstudio.ro_toolbox.controller.model.PackageServiceStatusResponse;
import com.bstudio.ro_toolbox.service.textureReplacer.AvailablePackage;
import com.bstudio.ro_toolbox.service.textureReplacer.userInterface.UserInterfaceManagerService;
import com.bstudio.ro_toolbox.util.DesktopFolderOpener;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/userinterface")
@RequiredArgsConstructor
public class UserInterfaceServiceController {

    private final UserInterfaceManagerService userInterfaceManagerService;


    @GetMapping("/status")
    public PackageServiceStatusResponse status() {
        var installed = userInterfaceManagerService.getInstalledProfileInfo();
        return PackageServiceStatusResponse.builder().selectedGameBase(absoluteOrNull(userInterfaceManagerService.getSelectedGameBase())).selectedGameItemFolder(absoluteOrNull(userInterfaceManagerService.getSelectedGameItemFolder())).installedProfile(installed).downloadedProfiles(userInterfaceManagerService.listDownloadedProfiles()).availableProfiles(userInterfaceManagerService.listAvailableProfiles()).build();
    }
    @PostMapping("/download")
    public MessageResponse downloadProfiles() throws IOException {
        Path dest = userInterfaceManagerService.getResourcesDir();
        Files.createDirectories(dest);
        userInterfaceManagerService.downloadAndExtract(null, dest);
        return MessageResponse.builder().message("Profiles downloaded.").build();
    }

    @PostMapping("/install")
    public MessageResponse installProfile(@RequestBody InstallProfileRequest request) throws IOException {
        if (request == null || request.getProfileId() == null || request.getProfileId().isBlank()) {
            throw new IllegalArgumentException("profileId is required.");
        }
        userInterfaceManagerService.installProfile(request.getProfileId().trim());
        return MessageResponse.builder().message("Profile installed: " + request.getProfileId().trim()).build();
    }

    @PostMapping("/clear-resources")
    public MessageResponse clearResources() throws IOException {
        userInterfaceManagerService.clearResources();
        return MessageResponse.builder().message("Downloaded resources cleared.").build();
    }

    @PostMapping("/clear-installed")
    public MessageResponse clearInstalled() throws IOException {
        userInterfaceManagerService.clearSelectedItemFolder();
        return MessageResponse.builder().message("Installed models cleared.").build();
    }

    @GetMapping("/check-update")
    public UserInterfaceManagerService.ResourcesUpdateCheckResult checkResourcesUpdate() {
        return userInterfaceManagerService.checkResourcesUpdate();
    }

    @PostMapping("/folders/open/resources")
    public MessageResponse openResourcesFolder() throws IOException {
        Path resources = userInterfaceManagerService.getResourcesDir();
        Files.createDirectories(resources);
        DesktopFolderOpener.openInDesktop(resources);
        return MessageResponse.builder().message("Opened resources folder.").build();
    }

    @PostMapping("/folders/open/item")
    public MessageResponse openItemFolder() throws IOException {
        Path item = userInterfaceManagerService.getSelectedGameItemFolder();
        if (item == null) {
            throw new IllegalStateException("No game installation folder is selected.");
        }
        Files.createDirectories(item);
        DesktopFolderOpener.openInDesktop(item);
        return MessageResponse.builder().message("Opened item folder.").build();
    }

    private String absoluteOrNull(Path path) {
        return path == null ? null : path.toAbsolutePath().normalize().toString();
    }





    public record AvailableProfileResponse(String id, String name, String author, String description, String url, String createdAt, String version, List<String> previewImages) {
    }

    public record UserInterfaceStatusResponse(
            String selectedGameBase,
            String selectedGameItemFolder,
            AvailablePackage installedProfile,
            List<String> downloadedProfiles,
            List<AvailablePackage> availableProfiles
    ) {
    }
}
