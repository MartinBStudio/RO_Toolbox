package com.bstudio.ro_toolbox.controller;

import com.bstudio.ro_toolbox.controller.model.InstallProfileRequest;
import com.bstudio.ro_toolbox.controller.model.MessageResponse;
import com.bstudio.ro_toolbox.service.textureReplacer.buffsAnimations.BuffsManagerService;
import com.bstudio.ro_toolbox.util.DesktopFolderOpener;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/buffs")
@RequiredArgsConstructor
public class BuffsServiceController extends BaseController {

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
        return MessageResponse.builder().message("Profiles downloaded.").build();
    }

    @PostMapping("/install")
    public MessageResponse installProfile(@RequestBody InstallProfileRequest request) throws IOException {
        if (request == null || request.getProfileId() == null || request.getProfileId().isBlank()) {
            throw new IllegalArgumentException("profileId is required.");
        }
        buffsManagerService.installProfile(request.getProfileId().trim());
        return MessageResponse.builder().message("Profile installed: " + request.getProfileId().trim()).build();
    }

    @PostMapping("/clear-resources")
    public MessageResponse clearResources() throws IOException {
        buffsManagerService.clearResources();
        return MessageResponse.builder().message("Downloaded resources cleared.").build();
    }

    @PostMapping("/clear-installed")
    public MessageResponse clearInstalled() throws IOException {
        buffsManagerService.clearSelectedItemFolder();
        return MessageResponse.builder().message("Installed buffs cleared.").build();
    }

    @GetMapping("/check-update")
    public BuffsManagerService.ResourcesUpdateCheckResult checkResourcesUpdate() {
        return buffsManagerService.checkResourcesUpdate();
    }

    @PostMapping("/folders/open/resources")
    public MessageResponse openResourcesFolder() throws IOException {
        Path resources = buffsManagerService.getResourcesDir();
        Files.createDirectories(resources);
        DesktopFolderOpener.openInDesktop(resources);
        return MessageResponse.builder().message("Opened resources folder.").build();
    }

    @PostMapping("/folders/open/item")
    public MessageResponse openItemFolder() throws IOException {
        Path item = buffsManagerService.getSelectedGameItemFolder();
        if (item == null) {
            throw new IllegalStateException("No game installation folder is selected.");
        }
        Files.createDirectories(item);
        DesktopFolderOpener.openInDesktop(item);
        return MessageResponse.builder().message("Opened item folder.").build();
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
