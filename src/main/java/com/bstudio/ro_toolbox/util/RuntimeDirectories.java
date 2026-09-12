package com.bstudio.ro_toolbox.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class RuntimeDirectories {
    private RuntimeDirectories() {
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
