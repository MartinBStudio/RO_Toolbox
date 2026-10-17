package com.bstudio.ro_toolbox.service.app;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AppConfigServiceTests {
  @TempDir Path tempDir;

  @Test
  void favoritesPersistByResourceTypeAndCanBeRemoved() throws IOException {
    AppConfigService config = new AppConfigService(tempDir);
    config.saveSelectedService("texture-replacer");
    assertEquals(List.of(), config.getFavoritePackages("combatText"));
    String name = "No combat text: ★ = . special";
    config.saveFavoritePackage("combatText", name, true);
    config.saveFavoritePackage("combatText", name, true);
    config.saveFavoritePackage("userInterface", name, true);

    AppConfigService reloaded = new AppConfigService(tempDir);
    assertEquals(List.of(name), reloaded.getFavoritePackages("combatText"));
    assertEquals(List.of(name), reloaded.getFavoritePackages("userInterface"));
    assertEquals(List.of(), reloaded.getFavoritePackages("loot"));
    reloaded.saveFavoritePackage("combatText", name, false);

    assertEquals(List.of(), config.getFavoritePackages("combatText"));
    assertEquals(List.of(name), config.getFavoritePackages("userInterface"));
    assertEquals("texture-replacer", config.getSelectedService());
  }

  @Test
  void rejectsUnknownResourceTypesAndEmptyNames() {
    AppConfigService config = new AppConfigService(tempDir);
    assertThrows(
        IllegalArgumentException.class,
        () -> config.saveFavoritePackage("unknown", "Example", true));
    assertThrows(
        IllegalArgumentException.class, () -> config.saveFavoritePackage("loot", " ", true));
  }

  @Test
  void favoritesOnlyPersistsSeparatelyForEveryService() throws IOException {
    AppConfigService config = new AppConfigService(tempDir);
    List<String> types = List.of("loot", "combatText", "userInterface", "buffIcons", "buffs");
    for (String type : types) {
      assertFalse(config.getFavoritesOnly(type));
      config.saveFavoritesOnly(type, true);
    }
    AppConfigService reloaded = new AppConfigService(tempDir);
    reloaded.saveFavoritesOnly("userInterface", false);
    for (String type : types) {
      assertEquals(!type.equals("userInterface"), reloaded.getFavoritesOnly(type));
    }
    reloaded.saveFavoritePackage("combatText", "Example", true);
    reloaded.saveFavoritePackage("combatText", "Example", false);
    assertTrue(new AppConfigService(tempDir).getFavoritesOnly("combatText"));
    assertThrows(IllegalArgumentException.class, () -> config.getFavoritesOnly("unknown"));
    assertThrows(IllegalArgumentException.class, () -> config.saveFavoritesOnly(null, true));
  }
}
