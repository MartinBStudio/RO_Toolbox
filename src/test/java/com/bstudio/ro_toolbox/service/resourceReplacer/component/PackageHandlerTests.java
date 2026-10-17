package com.bstudio.ro_toolbox.service.resourceReplacer.component;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PackageHandlerTests {
  @TempDir Path tempDir;

  private final PackageHandler handler = new PackageHandler(new PackageManifestReader());

  @Test
  void installingUiAfterUninstallPreservesCombatText() throws IOException {
    Path game = tempDir.resolve("game");
    Path resources = tempDir.resolve("resources");
    String combatFile = "3ddata/EFFECT/Particles/Texture/Critical.DDS";
    write(game.resolve(combatFile), "invisible combat text");
    write(game.resolve("3ddata/manifestCombatText.json"), "{\"name\":\"No combat text\"}");
    write(game.resolve("manifestUi.json"), "{\"name\":\"Current UI\"}");
    write(game.resolve("data/ui/theme.css"), "custom ui");
    write(resources.resolve("current/manifestUi.json"), "{\"name\":\"Current UI\"}");
    write(resources.resolve("current/.pack/FILE_LIST.txt"), "data/ui/theme.css\n");
    write(resources.resolve("old/.pack/FILE_LIST.txt"), combatFile + "\n");
    write(resources.resolve(".default/FILE_LIST.txt"), "data/ui/theme.css\n");
    write(resources.resolve(".default/data/ui/theme.css"), "default ui");

    handler.uninstallPackageFiles(game, resources, "manifestUi.json");
    assertFalse(Files.exists(game.resolve("manifestUi.json")));
    assertEquals("default ui", Files.readString(game.resolve("data/ui/theme.css")));

    // The next UI install invokes cleanup again, with no UI manifest remaining.
    handler.uninstallPackageFiles(game, resources, "manifestUi.json");

    assertEquals("invisible combat text", Files.readString(game.resolve(combatFile)));
    assertTrue(Files.exists(game.resolve("3ddata/manifestCombatText.json")));
  }

  @Test
  void noInstalledPackageDoesNotRestoreDefaultFiles() throws IOException {
    Path game = tempDir.resolve("game");
    Path resources = tempDir.resolve("resources");
    write(game.resolve("data/ui/theme.css"), "manually customized ui");
    write(resources.resolve(".default/FILE_LIST.txt"), "data/ui/theme.css\n");
    write(resources.resolve(".default/data/ui/theme.css"), "default ui");

    handler.uninstallPackageFiles(game, resources, "manifestUi.json");

    assertEquals("manually customized ui", Files.readString(game.resolve("data/ui/theme.css")));
  }

  @Test
  void installedPackageStillRestoresDefaultsWhenSourceIsMissing() throws IOException {
    Path game = tempDir.resolve("game");
    Path resources = tempDir.resolve("resources");
    write(game.resolve("manifestUi.json"), "{\"name\":\"Removed download\"}");
    write(game.resolve("data/ui/theme.css"), "custom ui");
    write(resources.resolve(".default/FILE_LIST.txt"), "data/ui/theme.css\n");
    write(resources.resolve(".default/data/ui/theme.css"), "default ui");

    handler.uninstallPackageFiles(game, resources, "manifestUi.json");

    assertEquals("default ui", Files.readString(game.resolve("data/ui/theme.css")));
    assertFalse(Files.exists(game.resolve("manifestUi.json")));
  }

  private void write(Path path, String content) throws IOException {
    Files.createDirectories(path.getParent());
    Files.writeString(path, content);
  }
}
