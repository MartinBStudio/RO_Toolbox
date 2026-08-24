package com.bstudio.ro_toolbox.service.updater;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class UpdaterBootstrap {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("UpdaterBootstrap requires: <current-jar> <new-jar>");
            return;
        }

        Path currentJar = Path.of(args[0]);
        Path newJar = Path.of(args[1]);

        long deadline = System.currentTimeMillis() + 15000;
        while (System.currentTimeMillis() < deadline) {
            try {
                if (Files.exists(currentJar)) {
                    Path backup = currentJar.resolveSibling(currentJar.getFileName() + ".bak");
                    if (Files.exists(backup)) {
                        Files.delete(backup);
                    }
                    Files.move(currentJar, backup, StandardCopyOption.REPLACE_EXISTING);
                    Files.move(newJar, currentJar, StandardCopyOption.REPLACE_EXISTING);
                    break;
                }
                Thread.sleep(500);
            } catch (IOException | InterruptedException ex) {
                Thread.sleep(1000);
            }
        }

        if (!Files.exists(currentJar)) {
            throw new IllegalStateException("Updated jar was not installed successfully.");
        }

        ProcessBuilder pb = new ProcessBuilder(
                Path.of(System.getProperty("java.home"), "bin", isWindows() ? "java.exe" : "java").toString(),
                "-jar",
                currentJar.toString()
        );
        pb.inheritIO();
        pb.start();
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}
