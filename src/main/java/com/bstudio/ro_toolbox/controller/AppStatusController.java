package com.bstudio.ro_toolbox.controller;

import com.bstudio.ro_toolbox.RoToolboxApplication;
import com.bstudio.ro_toolbox.service.app.TroseExecutableMonitor;
import com.bstudio.ro_toolbox.service.textureReplacer.AvailablePackage;
import com.bstudio.ro_toolbox.service.textureReplacer.buffIcons.IconsManagerService;
import com.bstudio.ro_toolbox.service.textureReplacer.buffsAnimations.BuffsManagerService;
import com.bstudio.ro_toolbox.service.textureReplacer.combatText.CombatTextManagerService;
import com.bstudio.ro_toolbox.service.textureReplacer.lootModels.LootManagerService;
import com.bstudio.ro_toolbox.service.textureReplacer.userInterface.UserInterfaceManagerService;
import com.bstudio.ro_toolbox.service.app.AppNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AppStatusController {

    private final RoToolboxApplication app;
    private final LootManagerService lootManagerService;
    private final CombatTextManagerService combatTextManagerService;
    private final UserInterfaceManagerService userInterfaceManagerService;
    private final IconsManagerService iconsManagerService;
    private final BuffsManagerService buffsManagerService;
    private final AppNotificationService appNotificationService;
    private final TroseExecutableMonitor troseExecutableMonitor;

    @GetMapping("/status")
    public AppStatusResponse status() {
        var installedLoot = lootManagerService.getInstalledProfileInfo();
        var installedCombatText = combatTextManagerService.getInstalledProfileInfo();
        var installedUserInterface = userInterfaceManagerService.getInstalledProfileInfo();
        var installedBuffIcons = iconsManagerService.getInstalledProfileInfo();
        var installedBuffs = buffsManagerService.getInstalledProfileInfo();
        return new AppStatusResponse(
                app.getVersion(),
                troseExecutableMonitor.isTroseRunning(),
                new PackageServiceSummary(
                        "/api/loot",
                        installedLoot
                ),
                new PackageServiceSummary(
                        "/api/combattext",
                        installedCombatText
                ),
                new PackageServiceSummary(
                        "/api/userinterface",
                        installedUserInterface
                ),
                new PackageServiceSummary(
                        "/api/bufficons",
                        installedBuffIcons
                ),
                new PackageServiceSummary(
                        "/api/buffs",
                        installedBuffs
                ),
                List.of(
                        new ServiceEndpointResponse("lootService", "/api/loot", "Loot profiles and installation"),
                        new ServiceEndpointResponse("combatTextService", "/api/combattext", "Combat text profiles and installation"),
                        new ServiceEndpointResponse("userInterfaceService", "/api/userinterface", "User interface profiles and installation"),
                        new ServiceEndpointResponse("buffIconsService", "/api/bufficons", "Buff icons profiles and installation"),
                        new ServiceEndpointResponse("buffsService", "/api/buffs", "Buffs texture replacement profiles and installation"),
                        new ServiceEndpointResponse("configEditorService", "/api/config-editor", "ROSE config TOML editor"),
                        new ServiceEndpointResponse("settings", "/api/settings", "Generic app settings"),
                        new ServiceEndpointResponse("updater", "/api/update", "Backend updater checks and install")
                )
        );
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
            List<ServiceEndpointResponse> services
    ) {
    }

    public record PackageServiceSummary(String endpoint, AvailablePackage activeProfile) {
    }

    public record ServiceEndpointResponse(String key, String endpoint, String description) {
    }

    public record NotificationQueueResponse(List<String> messages) {
    }
}
