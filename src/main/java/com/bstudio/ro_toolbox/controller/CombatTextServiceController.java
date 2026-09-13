package com.bstudio.ro_toolbox.controller;

import com.bstudio.ro_toolbox.controller.model.InstallProfileRequest;
import com.bstudio.ro_toolbox.controller.model.MessageResponse;
import com.bstudio.ro_toolbox.controller.model.PackageServiceStatusResponse;
import com.bstudio.ro_toolbox.service.app.AppConfigService;
import com.bstudio.ro_toolbox.service.common.ResourcesUpdater;
import com.bstudio.ro_toolbox.service.textureReplacer.ResourcePackage;
import com.bstudio.ro_toolbox.service.textureReplacer.combatText.CombatTextManagerService;
import com.bstudio.ro_toolbox.util.DesktopFolderOpener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/combattext")
@RequiredArgsConstructor
public class CombatTextServiceController extends BaseController {

  private final CombatTextManagerService combatTextManagerService;
  private final AppConfigService appConfigService;

  @GetMapping("/status")
  public PackageServiceStatusResponse status() {
    var installed = combatTextManagerService.getInstalledPackageInfo();
    return PackageServiceStatusResponse.builder()
        .selectedGameBase(absoluteOrNull(appConfigService.getSelectedGameBase()))
        .selectedGameItemFolder(absoluteOrNull(combatTextManagerService.getGameDataDir()))
        .installedProfile(installed)
        .downloadedProfiles(combatTextManagerService.listDownloadedProfiles())
        .availableProfiles(combatTextManagerService.listAvailableProfiles())
        .build();
  }

  @PostMapping("/download")
  public MessageResponse downloadProfiles() throws IOException {
    combatTextManagerService.downloadAndExtract();
    return MessageResponse.builder().message("Profiles downloaded.").build();
  }

  @PostMapping("/install")
  public MessageResponse installProfile(@RequestBody InstallProfileRequest request)
      throws IOException {
    if (request == null || request.getProfileId() == null || request.getProfileId().isBlank()) {
      throw new IllegalArgumentException("profileId is required.");
    }
    combatTextManagerService.installPackage(request.getProfileId().trim(), List.of());
    return MessageResponse.builder()
        .message("Profile installed: " + request.getProfileId().trim())
        .build();
  }

  @PostMapping("/clear-resources")
  public MessageResponse clearResources() throws IOException {
    combatTextManagerService.clearDownloadedPackages();
    return MessageResponse.builder().message("Downloaded resources cleared.").build();
  }

  @PostMapping("/clear-installed")
  public MessageResponse clearInstalled() throws IOException {
    combatTextManagerService.clearInstalledPackage();
    return MessageResponse.builder().message("Installed models cleared.").build();
  }

  @GetMapping("/check-update")
  public ResourcesUpdater.ResourcesUpdateCheckResult checkResourcesUpdate() {
    return combatTextManagerService.checkResourcesUpdate();
  }

  @PostMapping("/folders/open/resources")
  public MessageResponse openResourcesFolder() throws IOException {
    Path resources = combatTextManagerService.getResourcesDir();
    Files.createDirectories(resources);
    DesktopFolderOpener.openInDesktop(resources);
    return MessageResponse.builder().message("Opened resources folder.").build();
  }

  @PostMapping("/folders/open/item")
  public MessageResponse openItemFolder() throws IOException {
    Path item = combatTextManagerService.getGameDataDir();
    if (item == null) {
      throw new IllegalStateException("No game installation folder is selected.");
    }
    Files.createDirectories(item);
    DesktopFolderOpener.openInDesktop(item);
    return MessageResponse.builder().message("Opened item folder.").build();
  }

  public record AvailableProfileResponse(
      String id,
      String name,
      String author,
      String description,
      String url,
      String createdAt,
      String version,
      List<String> previewImages) {}

  public record CombatTextStatusResponse(
      String selectedGameBase,
      String selectedGameItemFolder,
      ResourcePackage installedProfile,
      List<String> downloadedProfiles,
      List<ResourcePackage> availableProfiles) {}
}
