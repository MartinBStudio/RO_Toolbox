package com.bstudio.ro_toolbox.service.common;

import com.bstudio.ro_toolbox.config.GeneralConstants;
import com.bstudio.ro_toolbox.util.RepositoryZipDownloader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@Slf4j
public class ResourcesUpdater implements ICommonMethods{
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

    public ResourcesUpdateCheckResult checkResourcesUpdate(String repoUrl,Path resourcesDir) {
        Path localManifest = resourcesDir.resolve(GeneralConstants.RESOURCE_MANIFEST_FILE_NAME);
        boolean localExists = Files.exists(localManifest) && Files.isRegularFile(localManifest);
        String localVersion = localExists ? readManifestVersion(localManifest) : "none";

        String[] branches = {"main", "master"};
        if (repoUrl.endsWith("/")) {
            repoUrl = repoUrl.substring(0, repoUrl.length() - 1);
        }
        String rawBase = repoUrl.replace("https://github.com/", "https://raw.githubusercontent.com/");

        for (String branch : branches) {
            String remoteUrl = rawBase + "/" + branch + "/" + GeneralConstants.RESOURCE_MANIFEST_FILE_NAME + "?cb=" + System.currentTimeMillis();
            try {
                InputStream in = RepositoryZipDownloader.openUrlStream(remoteUrl, "RO_ResourcesUpdater/1.0");
                if (in == null) {
                    continue;
                }
                String content;
                try (java.io.InputStreamReader reader = new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)) {
                    content = new java.io.BufferedReader(reader).lines().collect(java.util.stream.Collectors.joining("\n"));
                }
                java.util.regex.Matcher matcher = java.util.regex.Pattern
                        .compile("\"version\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
                        .matcher(content);
                String remoteVersion = matcher.find() ? matcher.group(1).trim() : "0.0.0";
                boolean updateAvailable = !localExists || normalizeVersion(remoteVersion) > normalizeVersion(localVersion);
                String message = updateAvailable
                        ? "New resources available: v" + remoteVersion + (localExists ? " (local: v" + localVersion + ")" : " (not downloaded)")
                        : "Resources are up to date (v" + localVersion + ").";
                return new ResourcesUpdateCheckResult(localVersion, remoteVersion, localExists, updateAvailable, true, message);
            } catch (Exception e) {
                log.info("Remote manifest check failed for branch " + branch + ": " + e.getMessage());
            }
        }

        return new ResourcesUpdateCheckResult(localVersion, "unknown", localExists, false, false, "Unable to check remote manifest.");
    }

    public record ResourcesUpdateCheckResult(
            String localVersion,
            String remoteVersion,
            boolean localExists,
            boolean updateAvailable,
            boolean success,
            String message
    ) {
    }

}
