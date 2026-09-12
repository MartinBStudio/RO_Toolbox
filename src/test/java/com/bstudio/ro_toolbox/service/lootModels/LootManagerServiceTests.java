package com.bstudio.ro_toolbox.service.lootModels;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LootManagerServiceTests {

    @Test
    void clearSelectedItemFolderRemovesDisabledManagedSubfolders(@TempDir Path tempDir) throws Exception {
        LootManagerService service = new LootManagerService();
        Path gameBase = tempDir.resolve("game");
        Path itemFolder = gameBase.resolve(Path.of("3ddata", "item"));
        Files.createDirectories(itemFolder.resolve("body"));
        Files.createDirectories(itemFolder.resolve("disabled_head"));
        Files.createDirectories(itemFolder.resolve(Path.of("body", "disabled_sub")));
        Files.createDirectories(itemFolder.resolve("disabled_keep"));
        Files.writeString(
                itemFolder.resolve("manifestLoot.json"),
                """
                {
                  "managedSubfolders": ["head", "body/sub"]
                }
                """
        );

        setSelectedGameBase(service, gameBase);

        service.clearSelectedItemFolder();

        assertFalse(Files.exists(itemFolder.resolve("disabled_head")));
        assertFalse(Files.exists(itemFolder.resolve(Path.of("body", "disabled_sub"))));
        assertFalse(Files.exists(itemFolder.resolve("manifestLoot.json")));
        assertTrue(Files.exists(itemFolder.resolve("disabled_keep")));
    }

    private static void setSelectedGameBase(LootManagerService service, Path selectedGameBase) throws NoSuchFieldException, IllegalAccessException {
        Field field = LootManagerService.class.getDeclaredField("selectedGameBase");
        field.setAccessible(true);
        field.set(service, selectedGameBase.toAbsolutePath().normalize());
    }
}
