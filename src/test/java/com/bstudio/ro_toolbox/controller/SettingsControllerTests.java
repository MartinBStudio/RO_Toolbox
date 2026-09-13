package com.bstudio.ro_toolbox.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bstudio.ro_toolbox.service.app.AppConfigService;
import com.bstudio.ro_toolbox.service.loginManager.LoginManagerService;
import com.bstudio.ro_toolbox.service.textureReplacer.buffIcons.IconsManagerService;
import com.bstudio.ro_toolbox.service.textureReplacer.buffsAnimations.BuffsManagerService;
import com.bstudio.ro_toolbox.service.textureReplacer.combatText.CombatTextManagerService;
import com.bstudio.ro_toolbox.service.textureReplacer.lootModels.LootManagerService;
import com.bstudio.ro_toolbox.service.textureReplacer.userInterface.UserInterfaceManagerService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
  void prefersSelectedRootWhenItAlreadyContainsTroseExecutable(@TempDir Path tempDir)
      throws IOException {
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

    SettingsController controller =
        new SettingsController(
            mock(LootManagerService.class),
            mock(CombatTextManagerService.class),
            mock(UserInterfaceManagerService.class),
            mock(IconsManagerService.class),
            mock(BuffsManagerService.class),
            mock(AppConfigService.class),
            mock(LoginManagerService.class));
    IllegalStateException ex =
        assertThrows(
            IllegalStateException.class,
            () ->
                controller.saveGameFolder(
                    new SettingsController.SaveFolderRequest(root.toString(), false)));

    assertEquals("The selected folder is not valid. It must contain trose.exe.", ex.getMessage());
  }

  @Test
  void resolvesQuickLaunchExecutablePreferringUpdaterBeforeTrose(@TempDir Path tempDir)
      throws IOException {
    Path root = tempDir.resolve("ROSE Online");
    Files.createDirectories(root);
    Files.createFile(root.resolve("rose-updater.exe"));
    Files.createFile(root.resolve("trose.exe"));

    assertEquals(
        root.resolve("rose-updater.exe"), SettingsController.resolveQuickLaunchExecutable(root));
  }

  @Test
  void fallsBackToTroseExecutableWhenUpdaterIsMissing(@TempDir Path tempDir) throws IOException {
    Path root = tempDir.resolve("ROSE Online");
    Files.createDirectories(root);
    Files.createFile(root.resolve("trose.exe"));

    assertEquals(root.resolve("trose.exe"), SettingsController.resolveQuickLaunchExecutable(root));
  }

  @Test
  void findsLauncherExecutableInNestedFolderWhenRootIsNotExact(@TempDir Path tempDir)
      throws IOException {
    Path root = tempDir.resolve("ROSE Online");
    Path nested = root.resolve("launcher");
    Files.createDirectories(nested);
    Files.createFile(nested.resolve("rose-updater.exe"));

    assertEquals(
        nested.resolve("rose-updater.exe"), SettingsController.resolveQuickLaunchExecutable(root));
  }

  @Test
  void factoryResetAlsoClearsSavedAccounts() throws IOException {
    LootManagerService loot = mock(LootManagerService.class);
    CombatTextManagerService combat = mock(CombatTextManagerService.class);
    UserInterfaceManagerService ui = mock(UserInterfaceManagerService.class);
    IconsManagerService buff = mock(IconsManagerService.class);
    BuffsManagerService buffs = mock(BuffsManagerService.class);
    AppConfigService appConfig = mock(AppConfigService.class);
    LoginManagerService login = mock(LoginManagerService.class);

    SettingsController controller =
        new SettingsController(loot, combat, ui, buff, buffs, appConfig, login);
    controller.factoryReset();

    verify(buff).clearSelectedItemFolder();
    verify(buff).clearResources();
    verify(buffs).clearSelectedItemFolder();
    verify(buffs).clearResources();
    verify(appConfig).clearSelectedGameBase();
    verify(appConfig).clearAppConfig();
    verify(login).clearAccounts();
  }

  @Test
  void readsReleaseNotesFromClasspathWhenWorkingDirectoryDoesNotContainFile(@TempDir Path tempDir)
      throws IOException {
    SettingsController controller =
        new SettingsController(
            mock(LootManagerService.class),
            mock(CombatTextManagerService.class),
            mock(UserInterfaceManagerService.class),
            mock(IconsManagerService.class),
            mock(BuffsManagerService.class),
            mock(AppConfigService.class),
            mock(LoginManagerService.class));

    String previousUserDir = System.getProperty("user.dir");
    System.setProperty("user.dir", tempDir.toString());
    try {
      SettingsController.ReleaseNotesResponse response = controller.getReleaseNotes();
      assertFalse(response.content().isBlank());
    } finally {
      if (previousUserDir == null) {
        System.clearProperty("user.dir");
      } else {
        System.setProperty("user.dir", previousUserDir);
      }
    }
  }

  @Test
  void savesQuickLaunchOnlyModeSetting() throws IOException {
    LootManagerService loot = mock(LootManagerService.class);
    AppConfigService appConfig = mock(AppConfigService.class);
    SettingsController controller =
        new SettingsController(
            loot,
            mock(CombatTextManagerService.class),
            mock(UserInterfaceManagerService.class),
            mock(IconsManagerService.class),
            mock(BuffsManagerService.class),
            appConfig,
            mock(LoginManagerService.class));

    SettingsController.QuickLaunchOnlyModeResponse response =
        controller.saveQuickLaunchOnlyMode(new SettingsController.QuickLaunchOnlyModeRequest(true));

    verify(appConfig).saveQuickLaunchOnlyMode(true);
    assertTrue(response.enabled());
  }

  @Test
  void readsQuickLaunchOnlyModeSetting() throws IOException {
    LootManagerService loot = mock(LootManagerService.class);
    AppConfigService appConfig = mock(AppConfigService.class);
    when(appConfig.getQuickLaunchOnlyMode()).thenReturn(true);
    SettingsController controller =
        new SettingsController(
            loot,
            mock(CombatTextManagerService.class),
            mock(UserInterfaceManagerService.class),
            mock(IconsManagerService.class),
            mock(BuffsManagerService.class),
            appConfig,
            mock(LoginManagerService.class));

    SettingsController.QuickLaunchOnlyModeResponse response = controller.getQuickLaunchOnlyMode();

    assertTrue(response.enabled());
  }

  @Test
  void savesUsefulStuffCollapsedSetting() throws IOException {
    LootManagerService loot = mock(LootManagerService.class);
    SettingsController controller =
        new SettingsController(
            loot,
            mock(CombatTextManagerService.class),
            mock(UserInterfaceManagerService.class),
            mock(IconsManagerService.class),
            mock(BuffsManagerService.class),
            mock(AppConfigService.class),
            mock(LoginManagerService.class));

    SettingsController.UsefulStuffCollapsedResponse response =
        controller.saveUsefulStuffCollapsed(
            new SettingsController.UsefulStuffCollapsedRequest(true));

    verify(loot).saveUsefulStuffCollapsed(true);
    assertTrue(response.collapsed());
  }

  @Test
  void readsUsefulStuffCollapsedSetting() throws IOException {
    AppConfigService appConfig = mock(AppConfigService.class);
    when(appConfig.getUsefulStuffCollapsed()).thenReturn(true);
    SettingsController controller =
        new SettingsController(
            mock(LootManagerService.class),
            mock(CombatTextManagerService.class),
            mock(UserInterfaceManagerService.class),
            mock(IconsManagerService.class),
            mock(BuffsManagerService.class),
            appConfig,
            mock(LoginManagerService.class));

    SettingsController.UsefulStuffCollapsedResponse response = controller.getUsefulStuffCollapsed();

    assertTrue(response.collapsed());
  }

  @Test
  void savesIgnoreConfigWarningsSetting() throws IOException {
    LootManagerService loot = mock(LootManagerService.class);
    SettingsController controller =
        new SettingsController(
            loot,
            mock(CombatTextManagerService.class),
            mock(UserInterfaceManagerService.class),
            mock(IconsManagerService.class),
            mock(BuffsManagerService.class),
            mock(AppConfigService.class),
            mock(LoginManagerService.class));

    SettingsController.IgnoreConfigWarningsResponse response =
        controller.saveIgnoreConfigWarnings(
            new SettingsController.IgnoreConfigWarningsRequest(true));

    verify(loot).saveIgnoreConfigWarnings(true);
    assertTrue(response.enabled());
  }

  @Test
  void readsIgnoreConfigWarningsSetting() throws IOException {
    LootManagerService loot = mock(LootManagerService.class);
    when(loot.getIgnoreConfigWarnings()).thenReturn(true);
    SettingsController controller =
        new SettingsController(
            loot,
            mock(CombatTextManagerService.class),
            mock(UserInterfaceManagerService.class),
            mock(IconsManagerService.class),
            mock(BuffsManagerService.class),
            mock(AppConfigService.class),
            mock(LoginManagerService.class));

    SettingsController.IgnoreConfigWarningsResponse response = controller.getIgnoreConfigWarnings();

    assertTrue(response.enabled());
  }
}
