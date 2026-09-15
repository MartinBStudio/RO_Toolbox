package com.bstudio.ro_toolbox.controller.resourceReplacer;

import com.bstudio.ro_toolbox.controller.resourceReplacer.model.InstallPackageRequest;
import com.bstudio.ro_toolbox.controller.resourceReplacer.model.MessageResponse;
import com.bstudio.ro_toolbox.controller.resourceReplacer.model.ResourceReplacerStatusResponse;
import com.bstudio.ro_toolbox.service.app.AppConfigService;
import com.bstudio.ro_toolbox.service.resourceReplacer.component.ResourcesUpdater;
import com.bstudio.ro_toolbox.service.resourceReplacer.model.Resource;
import com.bstudio.ro_toolbox.service.resourceReplacer.service.loot.LootManager;
import com.bstudio.ro_toolbox.util.DesktopFolderOpener;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/loot")
@RequiredArgsConstructor
public class LootServiceController extends BaseResourceReplacerController {

  private final LootManager lootManager;
  private final AppConfigService appConfigService;

  @GetMapping("/status")
  public ResourceReplacerStatusResponse status() {
    var installed = lootManager.getStatus();
    return ResourceReplacerStatusResponse.builder()
        .selectedGameBase(absoluteOrNull(appConfigService.getSelectedGameBase()))
        .selectedGameItemFolder(absoluteOrNull(lootManager.getGameDataDir()))
        .installedProfile(installed)
        .downloadedProfiles(
            List.of(
                lootManager.listPackages().stream().map(Resource::getName).toArray(String[]::new)))
        .availableProfiles(lootManager.listPackages())
        .build();
  }

  @PostMapping("/download")
  public MessageResponse downloadProfiles() throws IOException {
    lootManager.runUpdate();
    return MessageResponse.builder().message("Profiles downloaded.").build();
  }

  @PostMapping("/install")
  public MessageResponse installProfile(@RequestBody InstallPackageRequest request)
      throws IOException {
    if (request == null || request.getProfileId() == null || request.getProfileId().isBlank()) {
      throw new IllegalArgumentException("profileId is required.");
    }
    lootManager.installPackage(
        request.getProfileId().trim(), request.getDisabledManagedSubfolders());
    return MessageResponse.builder()
        .message("Profile installed: " + request.getProfileId().trim())
        .build();
  }

  @PostMapping("/manage")
  public MessageResponse manageInstalledProfile(@RequestBody InstallPackageRequest request)
      throws IOException {
    if (request == null || request.getProfileId() == null || request.getProfileId().isBlank()) {
      throw new IllegalArgumentException("profileId is required.");
    }
    lootManager.managePackage(
        request.getProfileId().trim(), request.getDisabledManagedSubfolders());
    return MessageResponse.builder().message("Managed folders updated.").build();
  }

  @PostMapping("/clear-resources")
  public MessageResponse clearResources() throws IOException {
    lootManager.clearDownloaded();
    return MessageResponse.builder().message("Downloaded resources cleared.").build();
  }

  @PostMapping("/clear-installed")
  public MessageResponse clearInstalled() throws IOException {
    lootManager.uninstallPackage();
    return MessageResponse.builder().message("Installed models cleared.").build();
  }

  @GetMapping("/check-update")
  public ResourcesUpdater.ResourcesUpdateCheckResult checkResourcesUpdate() {
    return lootManager.checkForUpdate();
  }

  @PostMapping("/folders/open/resources")
  public MessageResponse openResourcesFolder() throws IOException {
    Path resources = lootManager.getResourcesDir();
    Files.createDirectories(resources);
    DesktopFolderOpener.openInDesktop(resources);
    return MessageResponse.builder().message("Opened resources folder.").build();
  }

  @PostMapping("/folders/open/item")
  public MessageResponse openItemFolder() throws IOException {
    Path item = lootManager.getGameDataDir();
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
        resourceLoader
            .getResource("classpath:static/loot-model-folder-dictionary.json")
            .getInputStream()
            .readAllBytes(),
        StandardCharsets.UTF_8);
  }

  @GetMapping(value = {"/item-previews", "/previews"}, produces = MediaType.APPLICATION_JSON_VALUE)
  public Map<String, List<String>> getItemPreviews() throws IOException {
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    org.springframework.core.io.Resource[] resources =
        resolver.getResources("classpath*:static/ITEM_previews/**/*");

    Map<String, List<String>> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    for (org.springframework.core.io.Resource resource : resources) {
      if (!resource.isReadable()) {
        continue;
      }
      String filename = resource.getFilename();
      if (filename == null) {
        continue;
      }
      String lower = filename.toLowerCase();
      if (!(lower.endsWith(".png")
          || lower.endsWith(".jpg")
          || lower.endsWith(".jpeg")
          || lower.endsWith(".gif")
          || lower.endsWith(".webp"))) {
        continue;
      }

      String uriPath = resource.getURI().toString().replace('\\', '/');
      int idx = uriPath.indexOf("/ITEM_previews/");
      if (idx != -1) {
        String subPath = uriPath.substring(idx + "/ITEM_previews/".length());
        int slash = subPath.indexOf('/');
        if (slash != -1) {
          String folderName = subPath.substring(0, slash);
          String itemUrl = "/ITEM_previews/" + subPath;
          map.computeIfAbsent(folderName, k -> new ArrayList<>()).add(itemUrl);
        }
      }
    }

    map.values().forEach(list -> list.sort(String.CASE_INSENSITIVE_ORDER));
    return map;
  }

  @GetMapping(value = "/item-previews/{folder}", produces = MediaType.APPLICATION_JSON_VALUE)
  public List<String> getItemPreviewsForFolder(@PathVariable String folder) throws IOException {
    Map<String, List<String>> all = getItemPreviews();
    return all.getOrDefault(folder, List.of());
  }
}
