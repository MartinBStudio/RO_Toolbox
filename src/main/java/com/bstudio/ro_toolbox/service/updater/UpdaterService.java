package com.bstudio.ro_toolbox.service.updater;

import com.bstudio.ro_toolbox.RoToolboxApplication;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UpdaterService {
    private static final String GITHUB_OWNER = "MartinBStudio";
    private static final String GITHUB_REPO = "RO_Toolbox";
    private static final String RELEASES_URL = "https://github.com/" + GITHUB_OWNER + "/" + GITHUB_REPO + "/releases";

    private final RoToolboxApplication app;

    public UpdateCheckResult checkForUpdate() {
        String currentVersion = app.getVersion();
        String fallbackUrl = RELEASES_URL;
        try {
            String apiUrl = "https://api.github.com/repos/" + GITHUB_OWNER + "/" + GITHUB_REPO + "/releases/latest";
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "RO-Toolbox-Updater")
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return new UpdateCheckResult(currentVersion, "unknown", parseVersionNumber(currentVersion), 0, false, fallbackUrl,
                        "GitHub release check failed with status " + response.statusCode() + ".", false, null);
            }

            String body = response.body();
            Matcher tagMatcher = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"").matcher(body);
            Matcher urlMatcher = Pattern.compile("\"html_url\"\\s*:\\s*\"([^\"]+)\"").matcher(body);
            Matcher assetsMatcher = Pattern.compile("\"browser_download_url\"\\s*:\\s*\"([^\"]+)\"").matcher(body);

            String releaseTag = tagMatcher.find() ? tagMatcher.group(1) : "unknown";
            String releaseUrl = urlMatcher.find() ? urlMatcher.group(1) : fallbackUrl;
            String assetUrl = null;
            while (assetsMatcher.find()) {
                String candidate = assetsMatcher.group(1);
                if (candidate != null && candidate.toLowerCase().endsWith(".jar")) {
                    assetUrl = candidate;
                    break;
                }
            }

            int currentNumeric = parseVersionNumber(currentVersion);
            int releaseNumeric = parseVersionNumber(releaseTag);
            boolean updateAvailable = releaseNumeric > currentNumeric;

            String message;
            if (releaseNumeric == currentNumeric) {
                message = "Current version matches the checked GitHub release " + releaseTag + ".";
            } else if (updateAvailable) {
                message = "A newer release is available: " + releaseTag + " compared to your current version " + currentVersion + ".";
            } else {
                message = "Your current version appears newer than the checked GitHub release tag " + releaseTag + ".";
            }

            return new UpdateCheckResult(currentVersion, releaseTag, currentNumeric, releaseNumeric, updateAvailable, releaseUrl, message, true, assetUrl);
        } catch (Exception ex) {
            return new UpdateCheckResult(currentVersion, "unknown", parseVersionNumber(currentVersion), 0, false, fallbackUrl,
                    "Unable to check GitHub release: " + ex.getMessage(), false, null);
        }
    }

    public UpdateInstallResult installUpdate(UpdateCheckResult result) {
        if (result == null || !result.success() || !result.updateAvailable()) {
            return new UpdateInstallResult(false, "No update is available to install.");
        }
        if (result.assetUrl() == null || result.assetUrl().isBlank()) {
            return new UpdateInstallResult(false, "No downloadable JAR asset was found for the latest release.");
        }

        Path currentJar = resolveCurrentJarPath();
        if (currentJar == null || !currentJar.toString().toLowerCase().endsWith(".jar")) {
            return new UpdateInstallResult(false, "Self-update requires running the packaged JAR application.");
        }

        try {
            Path updateDir = currentJar.getParent() != null ? currentJar.getParent().resolve(".ro_toolbox_update") : Path.of(System.getProperty("user.home"), ".ro_toolbox_update");
            Files.createDirectories(updateDir);
            Path downloadedJar = updateDir.resolve("RO_Toolbox-update-" + System.currentTimeMillis() + ".jar");
            downloadFile(result.assetUrl(), downloadedJar);

            String javaBin = Path.of(System.getProperty("java.home"), "bin", System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java").toString();
            ProcessBuilder pb = new ProcessBuilder(
                    javaBin,
                    "-cp",
                    System.getProperty("java.class.path"),
                    "com.bstudio.ro_toolbox.service.updater.UpdaterBootstrap",
                    currentJar.toString(),
                    downloadedJar.toString()
            );
            pb.redirectErrorStream(true);
            pb.start();
            return new UpdateInstallResult(true, "Update started. The application will restart automatically.");
        } catch (Exception ex) {
            return new UpdateInstallResult(false, "Failed to download and install the update: " + ex.getMessage());
        }
    }

    private Path resolveCurrentJarPath() {
        try {
            String classPath = System.getProperty("java.class.path");
            if (classPath == null || classPath.isBlank()) {
                return null;
            }
            Path candidate = Path.of(classPath.split(java.io.File.pathSeparator)[0]);
            if (candidate.toString().toLowerCase().endsWith(".jar") || candidate.toString().toLowerCase().endsWith(".war")) {
                return candidate.toAbsolutePath().normalize();
            }
            return null;
        } catch (Exception ex) {
            return null;
        }
    }

    private void downloadFile(String url, Path target) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "RO-Toolbox-Updater")
                .GET()
                .build();

        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) {
            throw new IOException("Download failed with status " + response.statusCode() + ".");
        }
        try (var input = new java.io.ByteArrayInputStream(response.body())) {
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static int parseVersionNumber(String version) {
        if (version == null || version.isBlank()) {
            return 0;
        }
        String digits = version.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    public record UpdateCheckResult(
            String currentVersion,
            String releaseVersion,
            int currentNumericVersion,
            int releaseNumericVersion,
            boolean updateAvailable,
            String releaseUrl,
            String message,
            boolean success,
            String assetUrl
    ) {
    }

    public record UpdateInstallResult(
            boolean success,
            String message
    ) {
    }
}
