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
            Path updateDir = currentJar.getParent() != null
                    ? currentJar.getParent().resolve(".ro_toolbox_update")
                    : Path.of(System.getProperty("user.home"), ".ro_toolbox_update");
            Files.createDirectories(updateDir);
            Path downloadedJar = updateDir.resolve("RO_Toolbox-update.jar");
            downloadFile(result.assetUrl(), downloadedJar);

            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

            // Prefer the jpackage native launcher (RO_Toolbox.exe) when the app is running
            // from a jpackage app-image (jar lives in <root>/app/RO_Toolbox.jar,
            // launcher lives in <root>/RO_Toolbox.exe).
            String restartCommand = resolveRestartCommand(currentJar, isWindows);

            if (isWindows) {
                Path scriptPath = updateDir.resolve("update.bat");
                String script = "@echo off\r\n"
                        + "timeout /t 3 /nobreak >NUL\r\n"
                        + "move /Y \"" + downloadedJar.toAbsolutePath() + "\" \"" + currentJar.toAbsolutePath() + "\"\r\n"
                        + restartCommand + "\r\n"
                        + "del \"%~f0\"\r\n";
                Files.writeString(scriptPath, script);
                new ProcessBuilder("cmd.exe", "/c", "start", "/min", "", scriptPath.toString()).start();
            } else {
                Path scriptPath = updateDir.resolve("update.sh");
                String script = "#!/bin/sh\n"
                        + "sleep 3\n"
                        + "mv -f \"" + downloadedJar.toAbsolutePath() + "\" \"" + currentJar.toAbsolutePath() + "\"\n"
                        + restartCommand + " &\n"
                        + "rm -- \"$0\"\n";
                Files.writeString(scriptPath, script);
                scriptPath.toFile().setExecutable(true);
                new ProcessBuilder("sh", scriptPath.toString()).start();
            }

            return new UpdateInstallResult(true, "Update downloaded. The app will restart automatically.");
        } catch (Exception ex) {
            return new UpdateInstallResult(false, "Failed to download and install the update: " + ex.getMessage());
        }
    }

    /** Returns the shell command used to relaunch the app after the update. */
    private String resolveRestartCommand(Path currentJar, boolean isWindows) {
        // jpackage layout: <root>/app/RO_Toolbox.jar  →  <root>/RO_Toolbox.exe
        Path appDir = currentJar.getParent();
        if (appDir != null) {
            Path appRoot = appDir.getParent();
            if (appRoot != null) {
                String exeName = isWindows ? "RO_Toolbox.exe" : "RO_Toolbox";
                Path nativeLauncher = appRoot.resolve(exeName);
                if (Files.exists(nativeLauncher)) {
                    return "start \"\" \"" + nativeLauncher.toAbsolutePath() + "\"";
                }
            }
        }
        // Fallback: plain java -jar (used when running the JAR directly)
        String javaBin = Path.of(System.getProperty("java.home"), "bin", isWindows ? "javaw.exe" : "java").toString();
        return "start \"\" \"" + javaBin + "\" -jar \"" + currentJar.toAbsolutePath() + "\"";
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

    private void downloadFile(String url, Path target) throws IOException {
        String currentUrl = url;
        int maxRedirects = 10;
        while (maxRedirects-- > 0) {
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(currentUrl).openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 RO-Toolbox-Updater");
            conn.setRequestProperty("Accept", "application/octet-stream");
            conn.setInstanceFollowRedirects(false);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(120000);
            conn.connect();

            int status = conn.getResponseCode();
            if (status >= 200 && status < 300) {
                try (java.io.InputStream in = conn.getInputStream()) {
                    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                }
                conn.disconnect();
                return;
            } else if (status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {
                String location = conn.getHeaderField("Location");
                conn.disconnect();
                if (location == null || location.isBlank()) {
                    throw new IOException("Redirect with no Location header from: " + currentUrl);
                }
                currentUrl = location;
            } else {
                conn.disconnect();
                throw new IOException("Download failed with status " + status + " from: " + currentUrl);
            }
        }
        throw new IOException("Too many redirects while downloading update.");
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
