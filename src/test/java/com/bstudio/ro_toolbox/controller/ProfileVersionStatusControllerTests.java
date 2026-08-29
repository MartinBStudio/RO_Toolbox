package com.bstudio.ro_toolbox.controller;

import com.bstudio.ro_toolbox.service.combatText.CombatTextManagerService;
import com.bstudio.ro_toolbox.service.lootModels.LootManagerService;
import com.bstudio.ro_toolbox.service.userInterface.UserInterfaceManagerService;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProfileVersionStatusControllerTests {

    @Test
    void lootStatusIncludesInstalledAndAvailableProfileVersions() {
        LootManagerService service = mock(LootManagerService.class);
        when(service.getSelectedGameBase()).thenReturn(Path.of("/game"));
        when(service.getSelectedGameItemFolder()).thenReturn(Path.of("/game/3ddata/item"));
        when(service.getInstalledProfileInfo()).thenReturn(
                new LootManagerService.ProfileInfo("Loot", "Alice", "Desc", "https://example.com", "2026-08-29", "1.2.3")
        );
        when(service.listDownloadedProfiles()).thenReturn(List.of("loot"));
        when(service.listAvailableProfiles()).thenReturn(List.of(
                new LootManagerService.AvailableProfile("loot", "Loot", "Alice", "Desc", "https://example.com", "2026-08-29", "1.2.3", 1002003L, Path.of("/profiles/loot"))
        ));

        LootServiceController.LootStatusResponse response = new LootServiceController(service).status();

        assertNotNull(response.installedProfile());
        assertEquals("1.2.3", response.installedProfile().version());
        assertEquals("1.2.3", response.availableProfiles().getFirst().version());
    }

    @Test
    void combatTextStatusIncludesInstalledAndAvailableProfileVersions() {
        CombatTextManagerService service = mock(CombatTextManagerService.class);
        when(service.getSelectedGameBase()).thenReturn(Path.of("/game"));
        when(service.getSelectedGameItemFolder()).thenReturn(Path.of("/game/3ddata"));
        when(service.getInstalledProfileInfo()).thenReturn(
                new CombatTextManagerService.ProfileInfo("Combat", "Bob", "Desc", "https://example.com", "2026-08-29", "2.3.4")
        );
        when(service.listDownloadedProfiles()).thenReturn(List.of("combat"));
        when(service.listAvailableProfiles()).thenReturn(List.of(
                new CombatTextManagerService.AvailableProfile("combat", "Combat", "Bob", "Desc", "https://example.com", "2026-08-29", "2.3.4", 2003004L, Path.of("/profiles/combat"))
        ));

        CombatTextServiceController.CombatTextStatusResponse response = new CombatTextServiceController(service).status();

        assertNotNull(response.installedProfile());
        assertEquals("2.3.4", response.installedProfile().version());
        assertEquals("2.3.4", response.availableProfiles().getFirst().version());
    }

    @Test
    void userInterfaceStatusIncludesInstalledAndAvailableProfileVersions() {
        UserInterfaceManagerService service = mock(UserInterfaceManagerService.class);
        when(service.getSelectedGameBase()).thenReturn(Path.of("/game"));
        when(service.getSelectedGameItemFolder()).thenReturn(Path.of("/game"));
        when(service.getInstalledProfileInfo()).thenReturn(
                new UserInterfaceManagerService.ProfileInfo("UI", "Carol", "Desc", "https://example.com", "2026-08-29", "3.4.5")
        );
        when(service.listDownloadedProfiles()).thenReturn(List.of("ui"));
        when(service.listAvailableProfiles()).thenReturn(List.of(
                new UserInterfaceManagerService.AvailableProfile("ui", "UI", "Carol", "Desc", "https://example.com", "2026-08-29", "3.4.5", 3004005L, Path.of("/profiles/ui"))
        ));

        UserInterfaceServiceController.UserInterfaceStatusResponse response = new UserInterfaceServiceController(service).status();

        assertNotNull(response.installedProfile());
        assertEquals("3.4.5", response.installedProfile().version());
        assertEquals("3.4.5", response.availableProfiles().getFirst().version());
    }
}
