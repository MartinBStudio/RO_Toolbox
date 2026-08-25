package com.bstudio.ro_toolbox.service.lootModels;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class LootManagerService {
    private static final String DEFAULT_REPO = "https://github.com/MartinBStudio/RO_LootFilter_resources";
    private static final Path APP_DATA_ROOT = resolveAppDataRoot();
    private static final Path RESOURCES_DIR = APP_DATA_ROOT.resolve("resources").resolve("lootManager");
    private static final Path GAME_SUFFIX = Paths.get("3ddata", "item");

    private static final Path CONFIG_DIR = APP_DATA_ROOT.resolve("config");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("config.properties");

    private static Path resolveAppDataRoot() {
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) {
            return Paths.get(appData, "RO_Toolbox");
        }
        return Paths.get(System.getProperty("user.home"), ".ro_toolbox");
    }

    private static final Logger LOG = LoggerFactory.getLogger(LootManagerService.class);

    private volatile Path selectedGameBase = null; // base installation folder (no suffix)
    private volatile String currentLootProfile = null;

    public LootManagerService() {
        ensureRuntimeDirs();
        loadConfig();
    }

    private void ensureRuntimeDirs() {
        try {
            Files.createDirectories(APP_DATA_ROOT);
            Files.createDirectories(CONFIG_DIR);
            Files.createDirectories(RESOURCES_DIR);
        } catch (IOException ignored) {
        }
    }

    private void log(String s) {
        LOG.info(s);
    }

    public Path getResourcesDir() { return RESOURCES_DIR; }

    public Path getSelectedGameBase() { return selectedGameBase; }

    public Path getSelectedGameItemFolder() { return (selectedGameBase == null) ? null : selectedGameBase.resolve(GAME_SUFFIX); }

    public String getCurrentLootProfile() { return currentLootProfile; }

    public void setCurrentLootProfile(String profile) {
        currentLootProfile = (profile == null || profile.isBlank()) ? null : profile;
    }

    // --- config ---
    private void loadConfig() {
        try {
            if (!Files.exists(CONFIG_FILE)) return;
            Properties p = new Properties();
            try (InputStream in = Files.newInputStream(CONFIG_FILE)) { p.load(in); }
            String sel = p.getProperty("selectedGame");
            if (sel != null && !sel.isEmpty()) {
                Path pth = Paths.get(sel);
                if (Files.exists(pth)) selectedGameBase = pth;
            }
        } catch (Exception ignored) {
        }
    }

    /** Save the installation base folder (no 3ddata/item suffix). */
    public void saveSelectedGame(Path base) {
        try {
            if (base == null) {
                clearSelectedGame();
                return;
            }

            Files.createDirectories(CONFIG_DIR);
            Properties prop = new Properties();
            if (Files.exists(CONFIG_FILE)) {
                try (InputStream in = Files.newInputStream(CONFIG_FILE)) { prop.load(in); }
            }
            prop.setProperty("selectedGame", base.toAbsolutePath().toString());
            try (OutputStream out = Files.newOutputStream(CONFIG_FILE)) { prop.store(out, "RO LootManager config"); }
            selectedGameBase = base.toAbsolutePath().normalize();
            log("Selected game base saved: " + selectedGameBase);
        } catch (Exception ex) {
            log("Failed to save selected game base: " + ex.getMessage());
            throw new IllegalStateException("Unable to save selected game base to config.", ex);
        }
    }

    public void clearSelectedGame() {
        try {
            Files.createDirectories(CONFIG_DIR);
            Properties prop = new Properties();
            if (Files.exists(CONFIG_FILE)) {
                try (InputStream in = Files.newInputStream(CONFIG_FILE)) { prop.load(in); }
            }
            prop.remove("selectedGame");
            try (OutputStream out = Files.newOutputStream(CONFIG_FILE)) { prop.store(out, "RO LootManager config"); }
            selectedGameBase = null;
            log("Selected game base cleared.");
        } catch (Exception ex) {
            log("Failed to clear selected game base: " + ex.getMessage());
            throw new IllegalStateException("Unable to clear selected game base from config.", ex);
        }
    }

    public void downloadAndExtract(String repoUrl, Path destDir) throws IOException {
        if (repoUrl == null || repoUrl.isEmpty()) repoUrl = DEFAULT_REPO;
        if (!Files.exists(destDir)) Files.createDirectories(destDir);

        String[] branches = {"main", "master"};
        IOException lastException = null;

        for (String branch : branches) {
            String zipUrl = buildZipUrl(repoUrl, branch);
            log("Trying branch: " + branch + " -> " + zipUrl);
            try {
                Path tmp = Files.createTempFile("repo-", ".zip");
                try (InputStream in = openUrlStream(zipUrl)) {
                    if (in == null) throw new IOException("Not found: " + zipUrl);
                    Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
                }
                unzipTo(tmp, destDir);
                Files.deleteIfExists(tmp);
                return;
            } catch (IOException e) {
                lastException = e;
                log("Failed branch " + branch + ": " + e.getMessage());
            }
        }
        throw lastException != null ? lastException : new IOException("Failed to download repository zip");
    }

    private String buildZipUrl(String repoUrl, String branch) {
        if (repoUrl.endsWith("/")) repoUrl = repoUrl.substring(0, repoUrl.length() - 1);
        return repoUrl + "/archive/refs/heads/" + branch + ".zip";
    }

    private InputStream openUrlStream(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("User-Agent", "RO_LootManager/1.0");
        conn.setInstanceFollowRedirects(true);
        int code = conn.getResponseCode();
        if (code >= 200 && code < 300) return conn.getInputStream();
        conn.disconnect();
        return null;
    }

    private void unzipTo(Path zipFile, Path destDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                String[] parts = name.split("/", 2);
                String relative = parts.length == 2 ? parts[1] : (parts.length == 1 ? parts[0] : "");
                if (relative.isEmpty()) { zis.closeEntry(); continue; }
                Path outPath = destDir.resolve(relative);
                if (entry.isDirectory()) {
                    Files.createDirectories(outPath);
                } else {
                    Files.createDirectories(outPath.getParent());
                    try (OutputStream os = Files.newOutputStream(outPath)) {
                        byte[] buf = new byte[8192];
                        int len;
                        while ((len = zis.read(buf)) > 0) os.write(buf, 0, len);
                    }
                }
                log("Extracted: " + relative);
                zis.closeEntry();
            }
        }
    }

    public void deleteDirectoryContents(Path dir) throws IOException {
        if (!Files.exists(dir) || !Files.isDirectory(dir)) return;
        Files.walkFileTree(dir, new java.nio.file.SimpleFileVisitor<Path>() {
            @Override
            public java.nio.file.FileVisitResult visitFile(Path file, java.nio.file.attribute.BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                log("Deleted file: " + file.toAbsolutePath());
                return java.nio.file.FileVisitResult.CONTINUE;
            }

            @Override
            public java.nio.file.FileVisitResult postVisitDirectory(Path visitedDir, IOException exc) throws IOException {
                if (!visitedDir.equals(dir)) {
                    Files.deleteIfExists(visitedDir);
                    log("Deleted dir: " + visitedDir.toAbsolutePath());
                }
                return java.nio.file.FileVisitResult.CONTINUE;
            }
        });
    }

    public void copyDirectoryContents(Path src, Path dst) throws IOException {
        if (!Files.exists(src) || !Files.isDirectory(src)) return;
        try (java.util.stream.Stream<Path> stream = Files.walk(src)) {
            stream.forEach(sourcePath -> {
                try {
                    Path rel = src.relativize(sourcePath);
                    Path targetPath = dst.resolve(rel);
                    if (Files.isDirectory(sourcePath)) {
                        Files.createDirectories(targetPath);
                    } else {
                        Files.createDirectories(targetPath.getParent());
                        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                        log("Copied: " + targetPath.toAbsolutePath());
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    public void clearResources() throws IOException { deleteDirectoryContents(RESOURCES_DIR); }
    public void clearSelectedItemFolder() throws IOException { Path itemFolder = getSelectedGameItemFolder(); if (itemFolder != null) deleteDirectoryContents(itemFolder); }

    public record AvailableProfile(
            String id,
            String name,
            String author,
            String description,
            String url,
            String createdAt,
            long normalizedVersion,
            Path source
    ) {
    }

    public List<String> listDownloadedProfiles() {
        List<String> profiles = new ArrayList<>();
        if (RESOURCES_DIR == null || !Files.exists(RESOURCES_DIR) || !Files.isDirectory(RESOURCES_DIR)) {
            return profiles;
        }
        try (var stream = Files.list(RESOURCES_DIR)) {
            stream.filter(Files::isDirectory)
                    .filter(p -> !p.getFileName().toString().startsWith("."))
                    .forEach(p -> {
                        Path manifest = p.resolve("manifest.json");
                        if (Files.exists(manifest) && Files.isRegularFile(manifest)) {
                            profiles.add(p.getFileName().toString());
                        }
                    });
        } catch (IOException ignored) {
        }
        profiles.sort(String::compareToIgnoreCase);
        return profiles;
    }

    public List<AvailableProfile> listAvailableProfiles() {
        List<AvailableProfile> results = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        List<Path> roots = new ArrayList<>();
        roots.add(RESOURCES_DIR);
        if (selectedGameBase != null) {
            roots.add(selectedGameBase.resolveSibling(RESOURCES_DIR.getFileName()));
        }

        for (Path root : roots) {
            if (root == null || !Files.exists(root) || !Files.isDirectory(root)) continue;
            try (var stream = Files.walk(root)) {
                for (Path p : (Iterable<Path>) stream::iterator) {
                    if (!Files.isDirectory(p)) continue;
                    String name = p.getFileName().toString();
                    if (name.startsWith(".")) continue;
                    Path manifest = p.resolve("manifest.json");
                    if (!Files.exists(manifest) || !Files.isRegularFile(manifest)) continue;
                    if (seen.add(name)) {
                        results.add(new AvailableProfile(
                                name,
                                readManifestName(manifest),
                                readManifestAuthor(manifest),
                                readManifestDescription(manifest),
                                readManifestUrl(manifest),
                                readManifestCreatedAt(manifest),
                                normalizeVersion(readManifestVersion(manifest)),
                                p
                        ));
                    }
                }
            } catch (IOException ignored) {
            }
        }

        results.sort((a, b) -> {
            int versionDiff = Long.compare(b.normalizedVersion(), a.normalizedVersion());
            if (versionDiff != 0) return versionDiff;
            return a.id().compareToIgnoreCase(b.id());
        });
        return results;
    }

    public void installProfile(String profileId) throws IOException {
        Path destination = getSelectedGameItemFolder();
        if (destination == null) {
            throw new IllegalStateException("No game installation folder is selected.");
        }
        AvailableProfile selected = listAvailableProfiles().stream()
                .filter(profile -> profile.id().equals(profileId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Profile not found: " + profileId));

        clearSelectedItemFolder();
        copyDirectoryContents(selected.source(), destination);
        setCurrentLootProfile(selected.id());
    }

    public static final class ProfileInfo {
        public final String name;
        public final String author;
        public final String description;
        public final String url;
        public final String createdAt;

        public ProfileInfo(String name, String author, String description, String url, String createdAt) {
            this.name = name;
            this.author = author;
            this.description = description;
            this.url = url;
            this.createdAt = createdAt;
        }
    }

    public ProfileInfo getInstalledProfileInfo() {
        Path itemFolder = getSelectedGameItemFolder();
        if (itemFolder == null || !Files.exists(itemFolder)) {
            return null;
        }
        
        Path manifest = itemFolder.resolve("manifest.json");
        if (!Files.exists(manifest)) {
            return null;
        }
        
        return new ProfileInfo(
            readManifestName(manifest),
            readManifestAuthor(manifest),
            readManifestDescription(manifest),
            readManifestUrl(manifest),
            readManifestCreatedAt(manifest)
        );
    }

    private String readManifestName(Path manifestFile) {
        try {
            String content = Files.readString(manifestFile);
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\"name\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"").matcher(content);
            if (!matcher.find()) return null;
            String value = matcher.group(1).replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
            return value.trim();
        } catch (Exception e) {
            return null;
        }
    }

    private String readManifestDescription(Path manifestFile) {
        try {
            String content = Files.readString(manifestFile);
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\"description\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"").matcher(content);
            if (!matcher.find()) return null;
            String value = matcher.group(1).replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
            return value.trim();
        } catch (Exception e) {
            return null;
        }
    }

    private String readManifestUrl(Path manifestFile) {
        try {
            String content = Files.readString(manifestFile);
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\"url\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"").matcher(content);
            if (!matcher.find()) return null;
            String value = matcher.group(1).replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
            return value.trim();
        } catch (Exception e) {
            return null;
        }
    }

    private String readManifestAuthor(Path manifestFile) {
        try {
            String content = Files.readString(manifestFile);
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\"author\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"").matcher(content);
            if (!matcher.find()) return null;
            String value = matcher.group(1).replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
            return value.trim();
        } catch (Exception e) {
            return null;
        }
    }

    private String readManifestCreatedAt(Path manifestFile) {
        try {
            String content = Files.readString(manifestFile);
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\"createdAt\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"").matcher(content);
            if (!matcher.find()) return null;
            String value = matcher.group(1).replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
            return value.trim();
        } catch (Exception e) {
            return null;
        }
    }

    private String readManifestVersion(Path manifestFile) {
        try {
            String content = Files.readString(manifestFile);
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\"version\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"").matcher(content);
            if (!matcher.find()) return "0.0.0";
            return matcher.group(1).trim();
        } catch (Exception e) {
            return "0.0.0";
        }
    }

    private long normalizeVersion(String version) {
        if (version == null || version.isBlank()) return 0L;
        String cleaned = version.trim().replaceFirst("(?i)^v", "");
        String[] parts = cleaned.split("[.-]");
        long value = 0L;
        long multiplier = 1_000_000_000L;
        for (String part : parts) {
            if (part == null || part.isBlank()) continue;
            String digits = part.replaceAll("[^0-9]", "");
            if (digits.isEmpty()) continue;
            value += Long.parseLong(digits) * multiplier;
            multiplier /= 1000L;
        }
        return value;
    }

}
