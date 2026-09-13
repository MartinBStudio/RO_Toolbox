package com.bstudio.ro_toolbox.controller;

import com.bstudio.ro_toolbox.controller.model.InstallProfileRequest;
import com.bstudio.ro_toolbox.controller.model.MessageResponse;
import com.bstudio.ro_toolbox.controller.model.PackageServiceStatusResponse;
import com.bstudio.ro_toolbox.service.app.AppConfigService;
import com.bstudio.ro_toolbox.service.common.ResourcesUpdater;
import com.bstudio.ro_toolbox.service.textureReplacer.buffIcons.IconsManagerService;
import com.bstudio.ro_toolbox.util.DesktopFolderOpener;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/bufficons")
@RequiredArgsConstructor
public class BuffIconsServiceController extends BaseController {

    private final IconsManagerService iconsManagerService;
    private final AppConfigService appConfigService;

    @GetMapping("/status")
    public PackageServiceStatusResponse status() {
        var installed = iconsManagerService.getInstalledProfileInfo();
        return PackageServiceStatusResponse.builder().selectedGameBase(absoluteOrNull(appConfigService.getSelectedGameBase())).selectedGameItemFolder(absoluteOrNull(iconsManagerService.getGameDataDir())).installedProfile(installed).downloadedProfiles(iconsManagerService.listDownloadedProfiles()).availableProfiles(iconsManagerService.listAvailableProfiles()).build();
    }

    @PostMapping("/download")
    public MessageResponse downloadProfiles() throws IOException {
        iconsManagerService.downloadAndExtract();
        return MessageResponse.builder().message("Profiles downloaded.").build();
    }

    @PostMapping("/install")
    public MessageResponse installProfile(@RequestBody InstallProfileRequest request) throws IOException {
        if (request == null || request.getProfileId() == null || request.getProfileId().isBlank()) {
            throw new IllegalArgumentException("profileId is required.");
        }
        iconsManagerService.installProfile(request.getProfileId().trim());
        return MessageResponse.builder().message("Profile installed: " + request.getProfileId().trim()).build();
    }

    @PostMapping("/clear-resources")
    public MessageResponse clearResources() throws IOException {
        iconsManagerService.clearResources();
        return MessageResponse.builder().message("Downloaded resources cleared.").build();
    }

    @PostMapping("/clear-installed")
    public MessageResponse clearInstalled() throws IOException {
        iconsManagerService.clearSelectedItemFolder();
        return MessageResponse.builder().message("Installed buff icons cleared.").build();
    }

    @GetMapping("/check-update")
    public ResourcesUpdater.ResourcesUpdateCheckResult checkResourcesUpdate() {
        return iconsManagerService.checkResourcesUpdate();
    }

    @PostMapping("/folders/open/resources")
    public MessageResponse openResourcesFolder() throws IOException {
        Path resources = iconsManagerService.getResourcesDir();
        Files.createDirectories(resources);
        DesktopFolderOpener.openInDesktop(resources);
        return MessageResponse.builder().message("Opened resources folder.").build();
    }

    @PostMapping("/folders/open/item")
    public MessageResponse openItemFolder() throws IOException {
        Path item = iconsManagerService.getGameDataDir();
        if (item == null) {
            throw new IllegalStateException("No game installation folder is selected.");
        }
        Files.createDirectories(item);
        DesktopFolderOpener.openInDesktop(item);
        return MessageResponse.builder().message("Opened item folder.").build();
    }


}
