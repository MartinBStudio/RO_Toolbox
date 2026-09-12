package com.bstudio.ro_toolbox.util;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Path;

public final class DesktopFolderOpener {
    private DesktopFolderOpener() {
    }

    public static void openInDesktop(Path path) {
        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("win")) {
                openWithSystemCommand(path);
                return;
            }
            if (!Desktop.isDesktopSupported()) {
                openWithSystemCommand(path);
                return;
            }
            if (Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(path.toFile());
                return;
            }
            openWithSystemCommand(path);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to open folder: " + path.toAbsolutePath(), ex);
        }
    }

    private static void openWithSystemCommand(Path path) throws IOException {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            WindowsProcessLauncher.openFolderForeground(path);
            return;
        }
        if (os.contains("mac")) {
            new ProcessBuilder("open", path.toAbsolutePath().toString()).start();
            return;
        }
        new ProcessBuilder("xdg-open", path.toAbsolutePath().toString()).start();
    }
}
