package com.bstudio.ro_toolbox.controller;

import com.bstudio.ro_toolbox.service.combatText.CombatTextManagerService;
import com.bstudio.ro_toolbox.service.loginManager.LoginManagerService;
import com.bstudio.ro_toolbox.service.lootModels.LootManagerService;
import com.bstudio.ro_toolbox.service.userInterface.UserInterfaceManagerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SettingsControllerTests {

    @Test
    void resolvesGameBaseFromItemFolder(@TempDir Path tempDir) throws IOException {
        Path root = tempDir.resolve("ROSE Online");
        Files.createDirectories(root.resolve("3ddata").resolve("item"));
        Files.createFile(root.resolve("trose.exe"));

        assertEquals(root, SettingsController.resolveGameBase(root.resolve("3ddata").resolve("item")));
        assertEquals(root, SettingsController.resolveGameBase(root));
    }

    @Test
    void resolvesGameBaseFromNestedPathInsideGameFolder(@TempDir Path tempDir) throws IOException {
        Path root = tempDir.resolve("Games").resolve("ROSE Online").resolve("ROSE Online");
        Path nested = root.resolve("data").resolve("nested");
        Files.createDirectories(root.resolve("3ddata").resolve("item"));
        Files.createFile(root.resolve("trose.exe"));
        Files.createDirectories(nested);

        assertEquals(root, SettingsController.resolveGameBase(nested));
    }

    @Test
    void prefersSelectedRootWhenItAlreadyContainsTroseExecutable(@TempDir Path tempDir) throws IOException {
        Path parent = tempDir.resolve("ROSE Online");
        Path root = parent.resolve("ROSE Online");
        Files.createDirectories(root.resolve("3ddata").resolve("item"));
        Files.createFile(parent.resolve("trose.exe"));
        Files.createFile(root.resolve("trose.exe"));

        assertEquals(root, SettingsController.resolveGameBase(root));
    }

    @Test
    void rejectsFolderWithoutTroseExecutable(@TempDir Path tempDir) throws IOException {
        Path root = tempDir.resolve("ROSE Online");
        Files.createDirectories(root.resolve("3ddata").resolve("item"));

        SettingsController controller = new SettingsController(
                mock(LootManagerService.class),
                mock(CombatTextManagerService.class),
                mock(UserInterfaceManagerService.class),
                mock(LoginManagerService.class)
        );
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> controller.saveGameFolder(new SettingsController.SaveFolderRequest(root.toString(), false))
        );

        assertEquals("The selected folder is not valid. It must contain trose.exe.", ex.getMessage());
    }

    @Test
    void factoryResetAlsoClearsSavedAccounts() throws IOException {
        LootManagerService loot = mock(LootManagerService.class);
        CombatTextManagerService combat = mock(CombatTextManagerService.class);
        UserInterfaceManagerService ui = mock(UserInterfaceManagerService.class);
        LoginManagerService login = mock(LoginManagerService.class);

        SettingsController controller = new SettingsController(loot, combat, ui, login);
        controller.factoryReset();

        verify(login).clearAccounts();
    }
}
