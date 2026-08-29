package com.bstudio.ro_toolbox.controller;

import com.bstudio.ro_toolbox.RoToolboxApplication;
import com.bstudio.ro_toolbox.service.combatText.CombatTextManagerService;
import com.bstudio.ro_toolbox.service.lootModels.LootManagerService;
import com.bstudio.ro_toolbox.service.userInterface.UserInterfaceManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AppStatusController {

    private final RoToolboxApplication app;
    private final LootManagerService lootManagerService;
    private final CombatTextManagerService combatTextManagerService;
    private final UserInterfaceManagerService userInterfaceManagerService;

    @GetMapping("/status")
    public AppStatusResponse status() {
        LootManagerService.ProfileInfo installed = lootManagerService.getInstalledProfileInfo();
        CombatTextManagerService.ProfileInfo installedCombatText = combatTextManagerService.getInstalledProfileInfo();
        UserInterfaceManagerService.ProfileInfo installedUserInterface = userInterfaceManagerService.getInstalledProfileInfo();
        return new AppStatusResponse(
                app.getVersion(),
                new LootServiceSummaryResponse(
                        "/api/loot",
                        installed == null ? null : new ProfileInfoResponse(
                                installed.name, installed.author, installed.description, installed.url, installed.createdAt, installed.version
                        )
                ),
                new CombatTextServiceSummaryResponse(
                        "/api/combattext",
                        installedCombatText == null ? null : new ProfileInfoResponse(
                                installedCombatText.name, installedCombatText.author, installedCombatText.description, installedCombatText.url, installedCombatText.createdAt, installedCombatText.version
                        )
                ),
                new UserInterfaceServiceSummaryResponse(
                        "/api/userinterface",
                        installedUserInterface == null ? null : new ProfileInfoResponse(
                                installedUserInterface.name, installedUserInterface.author, installedUserInterface.description, installedUserInterface.url, installedUserInterface.createdAt, installedUserInterface.version
                        )
                ),
                List.of(
                        new ServiceEndpointResponse("lootService", "/api/loot", "Loot profiles and installation"),
                        new ServiceEndpointResponse("combatTextService", "/api/combattext", "Combat text profiles and installation"),
                        new ServiceEndpointResponse("userInterfaceService", "/api/userinterface", "User interface profiles and installation"),
                        new ServiceEndpointResponse("settings", "/api/settings", "Generic app settings"),
                        new ServiceEndpointResponse("updater", "/api/update", "Backend updater checks and install")
                )
        );
    }

    public record AppStatusResponse(
            String version,
            LootServiceSummaryResponse lootService,
            CombatTextServiceSummaryResponse combatTextService,
            UserInterfaceServiceSummaryResponse userInterfaceService,
            List<ServiceEndpointResponse> services
    ) {
    }

    public record LootServiceSummaryResponse(String endpoint, ProfileInfoResponse activeProfile) {
    }

    public record CombatTextServiceSummaryResponse(String endpoint, ProfileInfoResponse activeProfile) {
    }

    public record UserInterfaceServiceSummaryResponse(String endpoint, ProfileInfoResponse activeProfile) {
    }

    public record ServiceEndpointResponse(String key, String endpoint, String description) {
    }

    public record ProfileInfoResponse(String name, String author, String description, String url, String createdAt, String version) {
    }
}
