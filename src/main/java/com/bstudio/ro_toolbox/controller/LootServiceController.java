package com.bstudio.ro_toolbox.controller;

import com.bstudio.ro_toolbox.controller.model.InstallProfileRequest;
import com.bstudio.ro_toolbox.controller.model.MessageResponse;
import com.bstudio.ro_toolbox.service.lootModels.LootManagerService;
import com.bstudio.ro_toolbox.util.DesktopFolderOpener;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/loot")
@RequiredArgsConstructor
public class LootServiceController extends BaseController {

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
        lootManagerService.setCurrentLootProfile(null);
        return MessageResponse.builder().message("Installed models cleared.").build();
    }

    @GetMapping("/check-update")
    public LootManagerService.ResourcesUpdateCheckResult checkResourcesUpdate() {
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
        Path item = lootManagerService.getSelectedGameItemFolder();
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
