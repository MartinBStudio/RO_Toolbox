package com.bstudio.ro_toolbox.util;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class WindowsProcessLauncher {
    private WindowsProcessLauncher() {
    }

    public static void launchForeground(Path workingDirectory, String executablePath, String... arguments) throws IOException {
        List<String> command = new ArrayList<>();
        command.add("cmd.exe");
        command.add("/c");
        command.add("start");
        command.add("");
        command.add(executablePath);
        if (arguments != null) {
            for (String argument : arguments) {
                command.add(argument);
            }
        }

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(workingDirectory.toAbsolutePath().normalize().toFile());
        processBuilder.start();
    }

    public static void openFolderForeground(Path folderPath) throws IOException {
        List<String> command = new ArrayList<>();
        command.add("cmd.exe");
        command.add("/c");
        command.add("start");
        command.add("");
        command.add("explorer.exe");
        command.add(folderPath.toAbsolutePath().normalize().toString());

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(folderPath.toAbsolutePath().normalize().getParent().toFile());
        processBuilder.start();
    }
}
