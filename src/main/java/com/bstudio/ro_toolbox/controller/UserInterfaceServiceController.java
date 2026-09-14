package com.bstudio.ro_toolbox.controller;

import com.bstudio.ro_toolbox.controller.model.InstallProfileRequest;
import com.bstudio.ro_toolbox.controller.model.MessageResponse;
import com.bstudio.ro_toolbox.controller.model.PackageServiceStatusResponse;
import com.bstudio.ro_toolbox.service.app.AppConfigService;
import com.bstudio.ro_toolbox.service.resourceReplacer.component.ResourcesUpdater;
import com.bstudio.ro_toolbox.service.resourceReplacer.model.Resource;
import com.bstudio.ro_toolbox.service.resourceReplacer.service.userInterface.UserInterfaceManager;
import com.bstudio.ro_toolbox.util.DesktopFolderOpener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.DependsOn;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/userinterface")
@RequiredArgsConstructor
@DependsOn("appConfigService")
public class UserInterfaceServiceController {

  private final UserInterfaceManager userInterfaceManager;
  private final AppConfigService appConfigService;

  @GetMapping("/status")
  public PackageServiceStatusResponse status() {
    var installed = userInterfaceManager.getStatus();
    return PackageServiceStatusResponse.builder()
        .selectedGameBase(absoluteOrNull(appConfigService.getSelectedGameBase()))
        .selectedGameItemFolder(absoluteOrNull(userInterfaceManager.getGameDataDir()))
        .installedProfile(installed)
        .downloadedProfiles(
            List.of(
                userInterfaceManager.listPackages().stream()
                    .map(Resource::getName)
                    .toArray(String[]::new)))
        .availableProfiles(userInterfaceManager.listPackages())
        .build();
  }

  @PostMapping("/download")
  public MessageResponse downloadProfiles() throws IOException {
    userInterfaceManager.runUpdate();
    return MessageResponse.builder().message("Profiles downloaded.").build();
  }

  @PostMapping("/install")
  public MessageResponse installProfile(@RequestBody InstallProfileRequest request)
      throws IOException {
    if (request == null || request.getProfileId() == null || request.getProfileId().isBlank()) {
      throw new IllegalArgumentException("profileId is required.");
    }
    userInterfaceManager.installPackage(request.getProfileId().trim(), List.of());
    return MessageResponse.builder()
        .message("Profile installed: " + request.getProfileId().trim())
        .build();
  }

  @PostMapping("/clear-resources")
  public MessageResponse clearResources() throws IOException {
    userInterfaceManager.clearDownloaded();
    return MessageResponse.builder().message("Downloaded resources cleared.").build();
  }

  @PostMapping("/clear-installed")
  public MessageResponse clearInstalled() throws IOException {
    userInterfaceManager.uninstallPackage();
    return MessageResponse.builder().message("Installed models cleared.").build();
  }

  @GetMapping("/check-update")
  public ResourcesUpdater.ResourcesUpdateCheckResult checkResourcesUpdate() {
    return userInterfaceManager.checkForUpdate();
  }

  @PostMapping("/folders/open/resources")
  public MessageResponse openResourcesFolder() throws IOException {
    Path resources = userInterfaceManager.getResourcesDir();
    Files.createDirectories(resources);
    DesktopFolderOpener.openInDesktop(resources);
    return MessageResponse.builder().message("Opened resources folder.").build();
  }

  @PostMapping("/folders/open/item")
  public MessageResponse openItemFolder() throws IOException {
    Path item = userInterfaceManager.getGameDataDir();
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
}
