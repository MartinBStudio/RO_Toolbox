package com.bstudio.ro_toolbox.controller;

import com.bstudio.ro_toolbox.controller.model.InstallProfileRequest;
import com.bstudio.ro_toolbox.controller.model.MessageResponse;
import com.bstudio.ro_toolbox.controller.model.PackageServiceStatusResponse;
import com.bstudio.ro_toolbox.service.app.AppConfigService;
import com.bstudio.ro_toolbox.service.resourceReplacer.component.ResourcesUpdater;
import com.bstudio.ro_toolbox.service.resourceReplacer.model.Resource;
import com.bstudio.ro_toolbox.service.resourceReplacer.service.icons.IconsManager;
import com.bstudio.ro_toolbox.util.DesktopFolderOpener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bufficons")
@RequiredArgsConstructor
public class BuffIconsServiceController extends BaseController {

  private final IconsManager iconsManager;
  private final AppConfigService appConfigService;

  @GetMapping("/status")
  public PackageServiceStatusResponse status() {
    var installed = iconsManager.getStatus();
    return PackageServiceStatusResponse.builder()
        .selectedGameBase(absoluteOrNull(appConfigService.getSelectedGameBase()))
        .selectedGameItemFolder(absoluteOrNull(iconsManager.getGameDataDir()))
        .installedProfile(installed)
        .downloadedProfiles(
            List.of(
                iconsManager.listPackages().stream().map(Resource::getName).toArray(String[]::new)))
        .availableProfiles(iconsManager.listPackages())
        .build();
  }

  @PostMapping("/download")
  public MessageResponse downloadProfiles() throws IOException {
    iconsManager.runUpdate();
    return MessageResponse.builder().message("Packages downloaded.").build();
  }

  @PostMapping("/install")
  public MessageResponse installProfile(@RequestBody InstallProfileRequest request)
      throws IOException {
    if (request == null || request.getProfileId() == null || request.getProfileId().isBlank()) {
      throw new IllegalArgumentException("profileId is required.");
    }
    iconsManager.installPackage(request.getProfileId().trim(), List.of());
    return MessageResponse.builder()
        .message("Package installed: " + request.getProfileId().trim())
        .build();
  }

  @PostMapping("/clear-resources")
  public MessageResponse clearResources() throws IOException {
    iconsManager.clearDownloaded();
    return MessageResponse.builder().message("Downloaded resources cleared.").build();
  }

  @PostMapping("/clear-installed")
  public MessageResponse clearInstalled() throws IOException {
    iconsManager.uninstallPackage();
    return MessageResponse.builder().message("Installed buff icons cleared.").build();
  }

  @GetMapping("/check-update")
  public ResourcesUpdater.ResourcesUpdateCheckResult checkResourcesUpdate() {
    return iconsManager.checkForUpdate();
  }

  @PostMapping("/folders/open/resources")
  public MessageResponse openResourcesFolder() throws IOException {
    Path resources = iconsManager.getResourcesDir();
    Files.createDirectories(resources);
    DesktopFolderOpener.openInDesktop(resources);
    return MessageResponse.builder().message("Opened resources folder.").build();
  }

  @PostMapping("/folders/open/item")
  public MessageResponse openItemFolder() throws IOException {
    Path item = iconsManager.getGameDataDir();
    if (item == null) {
      throw new IllegalStateException("No game installation folder is selected.");
    }
    Files.createDirectories(item);
    DesktopFolderOpener.openInDesktop(item);
    return MessageResponse.builder().message("Opened item folder.").build();
  }
}
