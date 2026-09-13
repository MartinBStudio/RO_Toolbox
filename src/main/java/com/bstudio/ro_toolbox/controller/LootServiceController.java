package com.bstudio.ro_toolbox.controller;

import com.bstudio.ro_toolbox.controller.model.InstallProfileRequest;
import com.bstudio.ro_toolbox.controller.model.MessageResponse;
import com.bstudio.ro_toolbox.controller.model.PackageServiceStatusResponse;
import com.bstudio.ro_toolbox.service.app.AppConfigService;
import com.bstudio.ro_toolbox.service.common.ResourcesUpdater;
import com.bstudio.ro_toolbox.service.textureReplacer.lootModels.LootManagerService;
import com.bstudio.ro_toolbox.util.DesktopFolderOpener;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/loot")
@RequiredArgsConstructor
public class LootServiceController extends BaseController {

    private final LootManagerService lootManagerService;
    private final AppConfigService appConfigService;

    @GetMapping("/status")
    public PackageServiceStatusResponse status() {
        var installed = lootManagerService.getInstalledProfileInfo();
        return PackageServiceStatusResponse.builder().selectedGameBase(absoluteOrNull(appConfigService.getSelectedGameBase())).selectedGameItemFolder(absoluteOrNull(lootManagerService.getGameDataDir())).installedProfile(installed).downloadedProfiles(lootManagerService.listDownloadedProfiles()).availableProfiles(lootManagerService.listAvailableProfiles()).build();
    }
    @PostMapping("/download")
    public MessageResponse downloadProfiles() throws IOException {
        lootManagerService.downloadAndExtract();
        return MessageResponse.builder().message("Profiles downloaded.").build();
    }

    @PostMapping("/install")
    public MessageResponse installProfile(@RequestBody InstallProfileRequest request) throws IOException {
        if (request == null || request.getProfileId() == null || request.getProfileId().isBlank()) {
            throw new IllegalArgumentException("profileId is required.");
        }
        lootManagerService.installProfile(request.getProfileId().trim(), request.getDisabledManagedSubfolders());
        return MessageResponse.builder().message("Profile installed: " + request.getProfileId().trim()).build();
    }

    @PostMapping("/manage")
    public MessageResponse manageInstalledProfile(@RequestBody InstallProfileRequest request) throws IOException {
        if (request == null || request.getProfileId() == null || request.getProfileId().isBlank()) {
            throw new IllegalArgumentException("profileId is required.");
        }
        lootManagerService.manageInstalledProfile(request.getProfileId().trim(), request.getDisabledManagedSubfolders());
        return MessageResponse.builder().message("Managed folders updated.").build();
    }

    @PostMapping("/clear-resources")
    public MessageResponse clearResources() throws IOException {
        lootManagerService.clearResources();
        return MessageResponse.builder().message("Downloaded resources cleared.").build();
    }

    @PostMapping("/clear-installed")
    public MessageResponse clearInstalled() throws IOException {
        lootManagerService.clearSelectedItemFolder();
        return MessageResponse.builder().message("Installed models cleared.").build();
    }

    @GetMapping("/check-update")
    public ResourcesUpdater.ResourcesUpdateCheckResult checkResourcesUpdate() {
        return lootManagerService.checkResourcesUpdate();
    }

    @PostMapping("/folders/open/resources")
    public MessageResponse openResourcesFolder() throws IOException {
        Path resources = lootManagerService.getResourcesDir();
        Files.createDirectories(resources);
        DesktopFolderOpener.openInDesktop(resources);
        return MessageResponse.builder().message("Opened resources folder.").build();
    }

    @PostMapping("/folders/open/item")
    public MessageResponse openItemFolder() throws IOException {
        Path item = lootManagerService.getGameDataDir();
        if (item == null) {
            throw new IllegalStateException("No game installation folder is selected.");
        }
        Files.createDirectories(item);
        DesktopFolderOpener.openInDesktop(item);
        return MessageResponse.builder().message("Opened item folder.").build();
    }
    private final ResourceLoader resourceLoader;
    @GetMapping(value = "/dictionary", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getLootDictionary() throws IOException {
        return new String(
                resourceLoader.getResource("classpath:static/loot-model-folder-dictionary.json").getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
        );
    }


}
