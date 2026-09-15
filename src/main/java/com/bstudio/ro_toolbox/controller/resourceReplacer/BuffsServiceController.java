package com.bstudio.ro_toolbox.controller.resourceReplacer;

import com.bstudio.ro_toolbox.controller.resourceReplacer.model.InstallPackageRequest;
import com.bstudio.ro_toolbox.controller.resourceReplacer.model.MessageResponse;
import com.bstudio.ro_toolbox.controller.resourceReplacer.model.ResourceReplacerStatusResponse;
import com.bstudio.ro_toolbox.service.app.AppConfigService;
import com.bstudio.ro_toolbox.service.resourceReplacer.component.ResourcesUpdater;
import com.bstudio.ro_toolbox.service.resourceReplacer.model.Resource;
import com.bstudio.ro_toolbox.service.resourceReplacer.service.buffs.BuffsManager;
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
public class BuffsServiceController extends BaseResourceReplacerController {

  private final BuffsManager buffsManager;
  private final AppConfigService appConfigService;

  @GetMapping("/status")
  public ResourceReplacerStatusResponse status() {
    var installed = buffsManager.getStatus();
    return ResourceReplacerStatusResponse.builder()
        .selectedGameBase(absoluteOrNull(appConfigService.getSelectedGameBase()))
        .selectedGameItemFolder(absoluteOrNull(buffsManager.getGameDataDir()))
        .installedProfile(installed)
        .downloadedProfiles(
            List.of(
                buffsManager.listPackages().stream().map(Resource::getName).toArray(String[]::new)))
        .availableProfiles(buffsManager.listPackages())
        .build();
  }

  @PostMapping("/download")
  public MessageResponse downloadProfiles() throws IOException {
    buffsManager.runUpdate();
    return MessageResponse.builder().message("Profiles downloaded.").build();
  }

  @PostMapping("/install")
  public MessageResponse installProfile(@RequestBody InstallPackageRequest request)
      throws IOException {
    if (request == null || request.getProfileId() == null || request.getProfileId().isBlank()) {
      throw new IllegalArgumentException("profileId is required.");
    }
    buffsManager.installPackage(request.getProfileId().trim(), List.of());
    return MessageResponse.builder()
        .message("Profile installed: " + request.getProfileId().trim())
        .build();
  }

  @PostMapping("/clear-resources")
  public MessageResponse clearResources() throws IOException {
    buffsManager.clearDownloaded();
    return MessageResponse.builder().message("Downloaded resources cleared.").build();
  }

  @PostMapping("/clear-installed")
  public MessageResponse clearInstalled() throws IOException {
    buffsManager.uninstallPackage();
    return MessageResponse.builder().message("Installed buffs cleared.").build();
  }

  @GetMapping("/check-update")
  public ResourcesUpdater.ResourcesUpdateCheckResult checkResourcesUpdate() {
    return buffsManager.checkForUpdate();
  }

  @PostMapping("/folders/open/resources")
  public MessageResponse openResourcesFolder() throws IOException {
    Path resources = buffsManager.getResourcesDir();
    Files.createDirectories(resources);
    DesktopFolderOpener.openInDesktop(resources);
    return MessageResponse.builder().message("Opened resources folder.").build();
  }

  @PostMapping("/folders/open/item")
  public MessageResponse openItemFolder() throws IOException {
    Path item = buffsManager.getGameDataDir();
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
      Resource installedProfile,
      List<String> downloadedProfiles,
      List<Resource> availableProfiles) {}
}
