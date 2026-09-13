package com.bstudio.ro_toolbox.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AppDataPaths {
    private AppDataPaths() {
    }

    public static Path resolveRoToolboxAppDataRoot() {
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) {
            return Path.of(appData, "RO_Toolbox");
        }
        return Path.of(System.getProperty("user.home"), ".ro_toolbox");
    }
    public static void ensureRuntimeDirs(Path appDataRoot, Path configDir, Path resourcesDir) {
        try {
            Files.createDirectories(appDataRoot);
            Files.createDirectories(configDir);
            Files.createDirectories(resourcesDir);
        } catch (IOException ignored) {
        }
    }
}
