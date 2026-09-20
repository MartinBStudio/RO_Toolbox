package com.bstudio.ro_toolbox.service.resourceReplacer.component;

import com.bstudio.ro_toolbox.config.GeneralConstants;
import com.bstudio.ro_toolbox.service.resourceReplacer.utils.ICommonResourceMethods;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ResourcesUpdater implements ICommonResourceMethods {
  private final PackageManifestReader packageManifestReader;
  private final String USER_AGENT = "RO_ResourcesUpdater/1.0";

  public ResourcesUpdateCheckResult checkResourcesUpdate(String repoUrl, Path resourcesDir) {
    Path localManifest = resourcesDir.resolve(GeneralConstants.RESOURCE_MANIFEST_FILE_NAME);
    boolean localExists = Files.exists(localManifest) && Files.isRegularFile(localManifest);
    String localVersion =
        localExists ? packageManifestReader.readManifestVersion2(localManifest) : "none";

    String[] branches = {"main"};
    if (repoUrl.endsWith("/")) {
      repoUrl = repoUrl.substring(0, repoUrl.length() - 1);
    }
    String rawBase = repoUrl.replace("https://github.com/", "https://raw.githubusercontent.com/");

    for (String branch : branches) {
      String remoteUrl =
          rawBase
              + "/"
              + branch
              + "/"
              + GeneralConstants.RESOURCE_MANIFEST_FILE_NAME
              + "?cb="
              + System.currentTimeMillis();
      try {
        InputStream in = openUrlStream(remoteUrl);
        if (in == null) {
          continue;
        }
        String content;
        try (java.io.InputStreamReader reader =
            new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)) {
          content =
              new java.io.BufferedReader(reader)
                  .lines()
                  .collect(java.util.stream.Collectors.joining("\n"));
        }
        java.util.regex.Matcher matcher =
            java.util.regex.Pattern.compile("\"version\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
                .matcher(content);
        String remoteVersion = matcher.find() ? matcher.group(1).trim() : "0.0.0";
        boolean updateAvailable =
            !localExists || normalizeVersion(remoteVersion) > normalizeVersion(localVersion);
        String message =
            updateAvailable
                ? "New resources available: v"
                    + remoteVersion
                    + (localExists ? " (local: v" + localVersion + ")" : " (not downloaded)")
                : "Resources are up to date (v" + localVersion + ").";
        return new ResourcesUpdateCheckResult(
            localVersion, remoteVersion, localExists, updateAvailable, true, message);
      } catch (Exception e) {
        log.info("Remote manifest check failed for branch " + branch + ": " + e.getMessage());
      }
    }

    return new ResourcesUpdateCheckResult(
        localVersion, "unknown", localExists, false, false, "Unable to check remote manifest.");
  }

  public void runUpdate(String defaultRepoUrl, Path destinationDir) throws IOException {
    Objects.requireNonNull(defaultRepoUrl, "defaultRepoUrl is required.");
    Objects.requireNonNull(destinationDir, "destinationDir is required.");

    String effectiveRepo = sanitizeRepositoryUrl(defaultRepoUrl);
    Files.createDirectories(destinationDir);

    String[] branches = {"main", "master"};
    IOException lastException = null;

    for (String branch : branches) {
      String zipUrl = effectiveRepo + "/archive/refs/heads/" + branch + ".zip";
      log.info("Trying branch: " + branch + " -> " + zipUrl);

      Path tempZip = Files.createTempFile("repo-", ".zip");
      try {
        if (!downloadArchive(zipUrl, tempZip)) {
          throw new IOException("Not found: " + zipUrl);
        }
        replaceWithArchiveContents(tempZip, destinationDir);
        return;
      } catch (IOException ex) {
        lastException = ex;
        log.info("Failed branch " + branch + ": " + ex.getMessage());
      } finally {
        try {
          Files.deleteIfExists(tempZip);
        } catch (IOException ignored) {
        }
      }
    }

    throw lastException;
  }

  private InputStream openUrlStream(String url) throws IOException {
    HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
    connection.setRequestProperty("User-Agent", USER_AGENT);
    connection.setInstanceFollowRedirects(true);
    int statusCode = connection.getResponseCode();
    if (statusCode < 200 || statusCode >= 300) {
      connection.disconnect();
      return null;
    }
    return new DisconnectingInputStream(connection.getInputStream(), connection);
  }

  private String sanitizeRepositoryUrl(String repoUrl) {
    String sanitized = repoUrl.trim();
    if (sanitized.endsWith("/")) {
      sanitized = sanitized.substring(0, sanitized.length() - 1);
    }
    if (sanitized.endsWith(".git")) {
      sanitized = sanitized.substring(0, sanitized.length() - 4);
    }
    return sanitized;
  }

  private boolean downloadArchive(String url, Path outputFile) throws IOException {
    HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
    connection.setRequestProperty("User-Agent", USER_AGENT);
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

  private void replaceWithArchiveContents(Path zipFile, Path destinationDir) throws IOException {
    Path tempExtractDir = Files.createTempDirectory("resources-update-");
    try {
      unzipWithoutRootFolder(zipFile, tempExtractDir);
      deleteDirectoryContents(destinationDir);
      moveDirectoryContents(tempExtractDir, destinationDir);
    } finally {
      if (Files.exists(tempExtractDir)) {
        deleteDirectoryContents(tempExtractDir);
        Files.deleteIfExists(tempExtractDir);
      }
    }
  }

  private void unzipWithoutRootFolder(Path zipFile, Path destinationDir) throws IOException {
    try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(zipFile))) {
      ZipEntry entry;
      while ((entry = zipInputStream.getNextEntry()) != null) {
        String[] parts = entry.getName().split("/", 2);
        String relativePath = parts.length == 2 ? parts[1] : (parts.length == 1 ? parts[0] : "");
        if (relativePath.isEmpty()) {
          zipInputStream.closeEntry();
          continue;
        }

        Path outputPath = destinationDir.resolve(relativePath).normalize();
        if (!outputPath.startsWith(destinationDir)) {
          throw new IOException("Archive entry escapes destination: " + entry.getName());
        }
        if (entry.isDirectory()) {
          Files.createDirectories(outputPath);
        } else {
          Files.createDirectories(outputPath.getParent());
          Files.copy(zipInputStream, outputPath, StandardCopyOption.REPLACE_EXISTING);
        }
        log.info("Extracted: " + relativePath);
        zipInputStream.closeEntry();
      }
    }
  }

  private void moveDirectoryContents(Path sourceDir, Path destinationDir) throws IOException {
    Files.createDirectories(destinationDir);
    try (var stream = Files.list(sourceDir)) {
      for (Path source : (Iterable<Path>) stream::iterator) {
        Path target = destinationDir.resolve(source.getFileName());
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
      }
    }
  }

  private long normalizeVersion(String version) {
    if (version == null || version.isBlank()) {
      return 0L;
    }
    String cleaned = version.trim().replaceFirst("(?i)^v", "");
    String[] parts = cleaned.split("[.-]");
    long value = 0L;
    long multiplier = 1_000_000_000L;
    for (String part : parts) {
      if (part == null || part.isBlank()) {
        continue;
      }
      String digits = part.replaceAll("[^0-9]", "");
      if (digits.isEmpty()) {
        continue;
      }
      value += Long.parseLong(digits) * multiplier;
      multiplier /= 1000L;
    }
    return value;
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

  public record ResourcesUpdateCheckResult(
      String localVersion,
      String remoteVersion,
      boolean localExists,
      boolean updateAvailable,
      boolean success,
      String message) {}
}
