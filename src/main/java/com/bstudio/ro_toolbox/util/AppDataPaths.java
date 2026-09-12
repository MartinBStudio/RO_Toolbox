package com.bstudio.ro_toolbox.util;

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
}
