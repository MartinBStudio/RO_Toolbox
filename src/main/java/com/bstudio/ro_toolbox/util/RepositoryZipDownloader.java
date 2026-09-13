package com.bstudio.ro_toolbox.util;

import java.io.IOException;
import java.io.FilterInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class RepositoryZipDownloader {
    private RepositoryZipDownloader() {
    }

    public static void downloadAndExtract(
            String repoUrl,
            String defaultRepoUrl,
            Path destinationDir,
            String userAgent,
            Consumer<String> logger
    ) throws IOException {
        Objects.requireNonNull(defaultRepoUrl, "defaultRepoUrl is required.");
        Objects.requireNonNull(destinationDir, "destinationDir is required.");
        Objects.requireNonNull(userAgent, "userAgent is required.");

        String effectiveRepo = sanitizeRepositoryUrl((repoUrl == null || repoUrl.isBlank()) ? defaultRepoUrl : repoUrl);
        Files.createDirectories(destinationDir);

        String[] branches = {"main", "master"};
        IOException lastException = null;

        for (String branch : branches) {
            String zipUrl = effectiveRepo + "/archive/refs/heads/" + branch + ".zip";
            log(logger, "Trying branch: " + branch + " -> " + zipUrl);

            Path tempZip = Files.createTempFile("repo-", ".zip");
            try {
                if (!downloadArchive(zipUrl, userAgent, tempZip)) {
                    throw new IOException("Not found: " + zipUrl);
                }
                unzipWithoutRootFolder(tempZip, destinationDir, logger);
                return;
            } catch (IOException ex) {
                lastException = ex;
                log(logger, "Failed branch " + branch + ": " + ex.getMessage());
            } finally {
                try {
                    Files.deleteIfExists(tempZip);
                } catch (IOException ignored) {
                }
            }
        }

        throw lastException != null ? lastException : new IOException("Failed to download repository zip");
    }

    public static InputStream openUrlStream(String url, String userAgent) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestProperty("User-Agent", userAgent);
        connection.setInstanceFollowRedirects(true);
        int statusCode = connection.getResponseCode();
        if (statusCode < 200 || statusCode >= 300) {
            connection.disconnect();
            return null;
        }
        return new DisconnectingInputStream(connection.getInputStream(), connection);
    }

    private static String sanitizeRepositoryUrl(String repoUrl) {
        String sanitized = repoUrl.trim();
        if (sanitized.endsWith("/")) {
            sanitized = sanitized.substring(0, sanitized.length() - 1);
        }
        if (sanitized.endsWith(".git")) {
            sanitized = sanitized.substring(0, sanitized.length() - 4);
        }
        return sanitized;
    }

    private static boolean downloadArchive(String url, String userAgent, Path outputFile) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestProperty("User-Agent", userAgent);
        connection.setInstanceFollowRedirects(true);
        try {
            int statusCode = connection.getResponseCode();
            if (statusCode < 200 || statusCode >= 300) {
                return false;
            }

            try (InputStream inputStream = connection.getInputStream()) {
                Files.copy(inputStream, outputFile, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } finally {
            connection.disconnect();
        }
    }

    private static void unzipWithoutRootFolder(Path zipFile, Path destinationDir, Consumer<String> logger) throws IOException {
        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                String[] parts = entry.getName().split("/", 2);
                String relativePath = parts.length == 2 ? parts[1] : (parts.length == 1 ? parts[0] : "");
                if (relativePath.isEmpty()) {
                    zipInputStream.closeEntry();
                    continue;
                }

                Path outputPath = destinationDir.resolve(relativePath);
                if (entry.isDirectory()) {
                    Files.createDirectories(outputPath);
                } else {
                    Files.createDirectories(outputPath.getParent());
                    Files.copy(zipInputStream, outputPath, StandardCopyOption.REPLACE_EXISTING);
                }
                log(logger, "Extracted: " + relativePath);
                zipInputStream.closeEntry();
            }
        }
    }

    private static void log(Consumer<String> logger, String message) {
        if (logger != null) {
            logger.accept(message);
        }
    }

    private static final class DisconnectingInputStream extends FilterInputStream {
        private final HttpURLConnection connection;

        private DisconnectingInputStream(InputStream in, HttpURLConnection connection) {
            super(in);
            this.connection = connection;
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();
            } finally {
                connection.disconnect();
            }
        }
    }
}
