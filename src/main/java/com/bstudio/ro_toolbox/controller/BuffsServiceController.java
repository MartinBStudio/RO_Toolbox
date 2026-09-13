package com.bstudio.ro_toolbox.controller;

import com.bstudio.ro_toolbox.controller.model.InstallProfileRequest;
import com.bstudio.ro_toolbox.controller.model.MessageResponse;
import com.bstudio.ro_toolbox.controller.model.PackageServiceStatusResponse;
import com.bstudio.ro_toolbox.service.app.AppConfigService;
import com.bstudio.ro_toolbox.service.common.ResourcesUpdater;
import com.bstudio.ro_toolbox.service.textureReplacer.AvailablePackage;
import com.bstudio.ro_toolbox.service.textureReplacer.buffsAnimations.BuffsManagerService;
import com.bstudio.ro_toolbox.util.DesktopFolderOpener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/buffs")
@RequiredArgsConstructor
public class BuffsServiceController extends BaseController {

  private final BuffsManagerService buffsManagerService;
  private final AppConfigService appConfigService;

  @GetMapping("/status")
  public PackageServiceStatusResponse status() {
    var installed = buffsManagerService.getInstalledProfileInfo();
    return PackageServiceStatusResponse.builder()
        .selectedGameBase(absoluteOrNull(appConfigService.getSelectedGameBase()))
        .selectedGameItemFolder(absoluteOrNull(buffsManagerService.getGameDataDir()))
        .installedProfile(installed)
        .downloadedProfiles(buffsManagerService.listDownloadedProfiles())
        .availableProfiles(buffsManagerService.listAvailableProfiles())
        .build();
  }

  @PostMapping("/download")
  public MessageResponse downloadProfiles() throws IOException {
    buffsManagerService.downloadAndExtract();
    return MessageResponse.builder().message("Profiles downloaded.").build();
  }

  @PostMapping("/install")
  public MessageResponse installProfile(@RequestBody InstallProfileRequest request)
      throws IOException {
    if (request == null || request.getProfileId() == null || request.getProfileId().isBlank()) {
      throw new IllegalArgumentException("profileId is required.");
    }
    buffsManagerService.installProfile(request.getProfileId().trim());
    return MessageResponse.builder()
        .message("Profile installed: " + request.getProfileId().trim())
        .build();
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
  public ResourcesUpdater.ResourcesUpdateCheckResult checkResourcesUpdate() {
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
    Path item = buffsManagerService.getGameDataDir();
    if (item == null) {
      throw new IllegalStateException("No game installation folder is selected.");
    }
    Files.createDirectories(item);
    DesktopFolderOpener.openInDesktop(item);
    return MessageResponse.builder().message("Opened item folder.").build();
  }

  public record BuffsStatusResponse(
      String selectedGameBase,
      String selectedGameItemFolder,
      AvailablePackage installedProfile,
      List<String> downloadedProfiles,
      List<AvailablePackage> availableProfiles) {}
}
