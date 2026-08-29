package com.bstudio.ro_toolbox.service.configEditor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
}
