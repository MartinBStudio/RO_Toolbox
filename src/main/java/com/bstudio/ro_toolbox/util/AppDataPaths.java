package com.bstudio.ro_toolbox.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AppDataPaths {
    private static final Path APP_DATA_ROOT = AppDataPaths.resolveRoToolboxAppDataRoot();
    private static final Path RESOURCES_DIR = APP_DATA_ROOT.resolve("resources").resolve("buffIcons");
    private static final Path CONFIG_DIR = APP_DATA_ROOT.resolve("config");

    private AppDataPaths() {
        ensureRuntimeDirs(APP_DATA_ROOT, CONFIG_DIR, RESOURCES_DIR);
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
