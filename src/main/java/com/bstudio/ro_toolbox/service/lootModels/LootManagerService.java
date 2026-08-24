package com.bstudio.ro_toolbox.service.lootModels;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.util.Properties;
import java.util.function.Consumer;
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

    @Getter
    private volatile Path selectedGameBase = null; // base installation folder (no suffix)
    @Getter
    private volatile String currentLootProfile = null;

    // logger can be updated by the GUI to forward messages
    private Consumer<String> logger;

    // listeners notified when configuration changes (e.g., selected game base changed)
    private final java.util.List<Runnable> changeListeners = new java.util.concurrent.CopyOnWriteArrayList<>();

    public LootManagerService() {
        this.logger = null;
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

    public void setLogger(Consumer<String> logger) { this.logger = logger; }

    private void log(String s) {
        LOG.info(s);
        if (logger != null) logger.accept(s + "\n");
    }

    public void addChangeListener(Runnable r) { if (r != null) changeListeners.add(r); }
    private void notifyChangeListeners() { for (Runnable r : changeListeners) { try { r.run(); } catch (Throwable ignored) {} } }

    // Allow GUI callers to publish messages to both GUI and underlying logger
    public void guiMessage(String s) { log(s); }

    public Path getResourcesDir() { return RESOURCES_DIR; }

    public Path getSelectedGameBase() { return selectedGameBase; }

    public Path getSelectedGameItemFolder() { return (selectedGameBase == null) ? null : selectedGameBase.resolve(GAME_SUFFIX); }

    public String getCurrentLootProfile() { return currentLootProfile; }

    public void setCurrentLootProfile(String profile) {
        currentLootProfile = (profile == null || profile.isBlank()) ? null : profile;
        notifyChangeListeners();
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
            notifyChangeListeners();
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
            notifyChangeListeners();
        } catch (Exception ex) {
            log("Failed to clear selected game base: " + ex.getMessage());
            throw new IllegalStateException("Unable to clear selected game base from config.", ex);
        }
    }

    public boolean isDirectoryNonEmpty(Path dir) {
        if (dir == null) return false;
        if (!Files.exists(dir) || !Files.isDirectory(dir)) return false;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
            return ds.iterator().hasNext();
        } catch (IOException e) {
            return false;
        }
    }

    public Path getCurrentResourcesRoot() {
        return (getSelectedGameItemFolder() != null) ? getSelectedGameItemFolder() : RESOURCES_DIR;
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

    // Helpers for GUI-level workflows
    public void clearResources() throws IOException { deleteDirectoryContents(RESOURCES_DIR); }
    public void clearSelectedItemFolder() throws IOException { Path itemFolder = getSelectedGameItemFolder(); if (itemFolder != null) deleteDirectoryContents(itemFolder); }

    public Path findPocSource(String folderName) {
        Path pocSource = RESOURCES_DIR.resolve(folderName);
        if (!Files.exists(pocSource) || !Files.isDirectory(pocSource)) {
            Path alt = (selectedGameBase != null) ? selectedGameBase.resolve(RESOURCES_DIR.getFileName()).resolve(folderName) : null;
            if (alt != null && Files.exists(alt) && Files.isDirectory(alt)) pocSource = alt;
        }
        return (Files.exists(pocSource) && Files.isDirectory(pocSource)) ? pocSource : null;
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

}
