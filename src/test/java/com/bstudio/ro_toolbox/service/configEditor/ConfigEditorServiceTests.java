package com.bstudio.ro_toolbox.service.configEditor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConfigEditorServiceTests {

    @TempDir
    Path tempDir;

    @Test
    void statusReturnsTrackedFiles() throws IOException {
        ConfigEditorService service = new ConfigEditorService(tempDir);

        ConfigEditorService.ConfigEditorStatus status = service.readStatus();

        assertEquals(tempDir.toAbsolutePath().normalize().toString(), status.configDir());
        assertEquals(2, status.files().size());
        assertEquals("ignore", status.files().get(0).id());
        assertEquals("rose", status.files().get(1).id());
        assertFalse(status.files().get(0).exists());
        assertFalse(status.files().get(1).exists());
    }

    @Test
    void saveRejectsInvalidToml() {
        ConfigEditorService service = new ConfigEditorService(tempDir);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> service.save("ignore", "key = "));

        assertTrue(error.getMessage().contains("Invalid TOML"));
    }

    @Test
    void savePersistsValidToml() throws IOException {
        ConfigEditorService service = new ConfigEditorService(tempDir);

        ConfigEditorService.ConfigFileState state = service.save("rose", "name = \"ROSE\"\n[graphics]\nquality = \"high\"\n");

        assertTrue(state.exists());
        assertNotNull(state.parsed());
        assertNull(state.parseError());
        assertEquals("ROSE", ((String) state.parsed().get("name")));
        assertTrue(Files.exists(tempDir.resolve("rose.toml")));
    }

    @Test
    void ignoreListSupportsAddAndDelete() throws IOException {
        ConfigEditorService service = new ConfigEditorService(tempDir);

        ConfigEditorService.IgnoreListState addFirst = service.addIgnoreName("Test");
        ConfigEditorService.IgnoreListState addSecond = service.addIgnoreName("Another");
        ConfigEditorService.IgnoreListState afterDelete = service.deleteIgnoreName("test");

        assertEquals(1, addFirst.names().size());
        assertEquals(List.of("Test", "Another"), addSecond.names());
        assertEquals(List.of("Another"), afterDelete.names());
        String content = Files.readString(tempDir.resolve("ignore.toml"));
        assertTrue(content.contains("[[ignore]]"));
        assertTrue(content.contains("name = 'Another'"));
    }

    @Test
    void addIgnoreNameRejectsDuplicateEntries() throws IOException {
        ConfigEditorService service = new ConfigEditorService(tempDir);
        service.addIgnoreName("Test");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> service.addIgnoreName("test"));

        assertTrue(error.getMessage().contains("already exists"));
    }

    @Test
    void deleteIgnoreNameRejectsMissingEntry() throws IOException {
        ConfigEditorService service = new ConfigEditorService(tempDir);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> service.deleteIgnoreName("Missing"));

        assertTrue(error.getMessage().contains("not found"));
    }

    @Test
    void readIgnoreListRejectsInvalidIgnoreStructure() throws IOException {
        ConfigEditorService service = new ConfigEditorService(tempDir);
        Files.writeString(tempDir.resolve("ignore.toml"), "ignore = [\"bad\"]\n");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, service::readIgnoreList);

        assertTrue(error.getMessage().contains("each [[ignore]] entry must be a table"));
    }

    @Test
    void roseConfigStateDefaultsWhenValueMissing() throws IOException {
        ConfigEditorService service = new ConfigEditorService(tempDir);

        ConfigEditorService.RoseConfigState roseConfigState = service.readRoseConfigState();

        assertFalse(roseConfigState.roseFileExists());
        assertFalse(roseConfigState.showDroppedItemName());
    }

    @Test
    void roseConfigStateReadsNestedShowDroppedItemName() throws IOException {
        ConfigEditorService service = new ConfigEditorService(tempDir);
        Files.writeString(tempDir.resolve("rose.toml"), "[ui]\nshow_dropped_item_name = true\n");

        ConfigEditorService.RoseConfigState roseConfigState = service.readRoseConfigState();

        assertTrue(roseConfigState.roseFileExists());
        assertTrue(roseConfigState.showDroppedItemName());
    }

    @Test
    void roseConfigStateRejectsNonBooleanShowDroppedItemName() throws IOException {
        ConfigEditorService service = new ConfigEditorService(tempDir);
        Files.writeString(tempDir.resolve("rose.toml"), "show_dropped_item_name = \"yes\"\n");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, service::readRoseConfigState);

        assertTrue(error.getMessage().contains("show_dropped_item_name must be true or false"));
    }

    @Test
    void setShowDroppedItemNameCreatesRoseConfigWhenMissing() throws IOException {
        ConfigEditorService service = new ConfigEditorService(tempDir);

        ConfigEditorService.RoseConfigState roseConfigState = service.setShowDroppedItemName(false);

        assertTrue(roseConfigState.roseFileExists());
        assertFalse(roseConfigState.showDroppedItemName());
        assertEquals("show_dropped_item_name = false" + System.lineSeparator(), Files.readString(tempDir.resolve("rose.toml")));
    }

    @Test
    void setShowDroppedItemNameUpdatesExistingValueAndKeepsInlineComment() throws IOException {
        ConfigEditorService service = new ConfigEditorService(tempDir);
        Files.writeString(tempDir.resolve("rose.toml"), "[ui]\nshow_dropped_item_name = true # keep me\n");

        ConfigEditorService.RoseConfigState roseConfigState = service.setShowDroppedItemName(false);

        assertTrue(roseConfigState.roseFileExists());
        assertFalse(roseConfigState.showDroppedItemName());
        String savedContent = Files.readString(tempDir.resolve("rose.toml"));
        assertTrue(savedContent.contains("show_dropped_item_name = false # keep me"));
    }

    @Test
    void setShowDroppedItemNameRejectsNonBooleanAssignment() throws IOException {
        ConfigEditorService service = new ConfigEditorService(tempDir);
        Files.writeString(tempDir.resolve("rose.toml"), "show_dropped_item_name = \"enabled\"\n");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> service.setShowDroppedItemName(false));

        assertTrue(error.getMessage().contains("show_dropped_item_name must be true or false"));
    }
}
