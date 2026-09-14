package com.bstudio.ro_toolbox.controller;

import com.bstudio.ro_toolbox.RoToolboxApplication;
import com.bstudio.ro_toolbox.service.app.AppNotificationService;
import com.bstudio.ro_toolbox.service.app.TroseExecutableMonitor;
import com.bstudio.ro_toolbox.service.resourceReplacer.model.Resource;
import com.bstudio.ro_toolbox.service.resourceReplacer.service.buffs.BuffsManager;
import com.bstudio.ro_toolbox.service.resourceReplacer.service.combatText.CombatTextManager;
import com.bstudio.ro_toolbox.service.resourceReplacer.service.icons.IconsManager;
import com.bstudio.ro_toolbox.service.resourceReplacer.service.loot.LootManager;
import com.bstudio.ro_toolbox.service.resourceReplacer.service.userInterface.UserInterfaceManager;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AppStatusController {

  private final RoToolboxApplication app;
  private final LootManager lootManager;
  private final CombatTextManager combatTextManagerService;
  private final UserInterfaceManager userInterfaceManager;
  private final IconsManager iconsManager;
  private final BuffsManager buffsManager;
  private final AppNotificationService appNotificationService;
  private final TroseExecutableMonitor troseExecutableMonitor;

  @GetMapping("/status")
  public AppStatusResponse status() {
    var installedLoot = lootManager.getStatus();
    var installedCombatText = combatTextManagerService.getStatus();
    var installedUserInterface = userInterfaceManager.getStatus();
    var installedBuffIcons = iconsManager.getStatus();
    var installedBuffs = buffsManager.getStatus();
    return new AppStatusResponse(
        app.getVersion(),
        troseExecutableMonitor.isTroseRunning(),
        new PackageServiceSummary("/api/loot", installedLoot),
        new PackageServiceSummary("/api/combattext", installedCombatText),
        new PackageServiceSummary("/api/userinterface", installedUserInterface),
        new PackageServiceSummary("/api/bufficons", installedBuffIcons),
        new PackageServiceSummary("/api/buffs", installedBuffs),
        List.of(
            new ServiceEndpointResponse(
                "lootService", "/api/loot", "Loot profiles and installation"),
            new ServiceEndpointResponse(
                "combatTextService", "/api/combattext", "Combat text profiles and installation"),
            new ServiceEndpointResponse(
                "userInterfaceService",
                "/api/userinterface",
                "User interface profiles and installation"),
            new ServiceEndpointResponse(
                "buffIconsService", "/api/bufficons", "Buff icons profiles and installation"),
            new ServiceEndpointResponse(
                "buffsService",
                "/api/buffs",
                "Buffs texture replacement profiles and installation"),
            new ServiceEndpointResponse(
                "configEditorService", "/api/config-editor", "ROSE config TOML editor"),
            new ServiceEndpointResponse("settings", "/api/settings", "Generic app settings"),
            new ServiceEndpointResponse(
                "updater", "/api/update", "Backend updater checks and install")));
  }

  @GetMapping("/notifications/drain")
  public NotificationQueueResponse drainNotifications() {
    return new NotificationQueueResponse(appNotificationService.drain());
  }

  public record AppStatusResponse(
      String version,
      boolean troseRunning,
      PackageServiceSummary lootService,
      PackageServiceSummary combatTextService,
      PackageServiceSummary userInterfaceService,
      PackageServiceSummary buffIconsService,
      PackageServiceSummary buffsService,
      List<ServiceEndpointResponse> services) {}

  public record PackageServiceSummary(String endpoint, Resource activeProfile) {}

  public record ServiceEndpointResponse(String key, String endpoint, String description) {}

  public record NotificationQueueResponse(List<String> messages) {}
}
