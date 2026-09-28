package com.bstudio.ro_toolbox.controller.resourceReplacer;

import com.bstudio.ro_toolbox.controller.resourceReplacer.model.InstallPackageRequest;
import com.bstudio.ro_toolbox.controller.resourceReplacer.model.MessageResponse;
import com.bstudio.ro_toolbox.controller.resourceReplacer.model.ResourceReplacerStatusResponse;
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
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/bufficons")
@RequiredArgsConstructor
public class BuffIconsServiceController extends BaseResourceReplacerController {

  private final IconsManager iconsManager;
  private final AppConfigService appConfigService;

  @GetMapping("/status")
  public ResourceReplacerStatusResponse status() {
    var installed = iconsManager.getStatus();
    return ResourceReplacerStatusResponse.builder()
        .selectedGameBase(absoluteOrNull(appConfigService.getSelectedGameBase()))
        .selectedGameItemFolder(absoluteOrNull(iconsManager.getGameDataDir()))
        .installedProfile(installed)
        .downloadedProfiles(
            List.of(
                iconsManager.listPackages().stream().map(Resource::getName).toArray(String[]::new)))
        .availableProfiles(iconsManager.listPackages())
        .build();
  }

  @PostMapping(value = "/download/progress", produces = MediaType.APPLICATION_NDJSON_VALUE)
  public StreamingResponseBody downloadProfiles() {
    return downloadWithProgress(
        iconsManager::runUpdate,
        "Downloading buff icon packages...",
        "Buff icon packages downloaded.");
  }

  @PostMapping("/install")
  public MessageResponse installProfile(@RequestBody InstallPackageRequest request)
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
