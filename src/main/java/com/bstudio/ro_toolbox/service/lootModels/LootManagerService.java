package com.bstudio.ro_toolbox.service.lootModels;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class LootManagerService {
    private static final String DEFAULT_REPO = "https://github.com/MartinBStudio/RO_LootFilter_resources";
    private static final String MANIFEST_FILE_NAME = "manifestLoot.json";
    private static final String LEGACY_MANIFEST_FILE_NAME = "manifest.json";
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

    public void clearAppConfig() throws IOException {
        if (Files.exists(CONFIG_FILE)) {
            Files.deleteIfExists(CONFIG_FILE);
            log("Deleted app config: " + CONFIG_FILE.toAbsolutePath());
        }
    }

    public String getSelectedService() throws IOException {
        if (!Files.exists(CONFIG_FILE)) {
            return null;
        }
        Properties prop = new Properties();
        try (InputStream in = Files.newInputStream(CONFIG_FILE)) {
            prop.load(in);
        }
        String selectedService = prop.getProperty("selectedService");
        return (selectedService == null || selectedService.isBlank()) ? null : selectedService.trim();
    }

    public void saveSelectedService(String serviceId) throws IOException {
        if (serviceId == null || serviceId.isBlank()) {
            throw new IllegalArgumentException("Service id is required.");
        }
        Files.createDirectories(CONFIG_DIR);
        Properties prop = new Properties();
        if (Files.exists(CONFIG_FILE)) {
            try (InputStream in = Files.newInputStream(CONFIG_FILE)) {
                prop.load(in);
            }
        }
        prop.setProperty("selectedService", serviceId.trim());
        try (OutputStream out = Files.newOutputStream(CONFIG_FILE)) {
            prop.store(out, "RO LootManager config");
        }
    }

    public boolean getQuickLaunchOnlyMode() throws IOException {
        if (!Files.exists(CONFIG_FILE)) {
            return false;
        }
        Properties prop = new Properties();
        try (InputStream in = Files.newInputStream(CONFIG_FILE)) {
            prop.load(in);
        }
        return Boolean.parseBoolean(prop.getProperty("quickLaunchOnlyMode", "false").trim());
    }

    public void saveQuickLaunchOnlyMode(boolean enabled) throws IOException {
        Files.createDirectories(CONFIG_DIR);
        Properties prop = new Properties();
        if (Files.exists(CONFIG_FILE)) {
            try (InputStream in = Files.newInputStream(CONFIG_FILE)) {
                prop.load(in);
            }
        }
        prop.setProperty("quickLaunchOnlyMode", String.valueOf(enabled));
        try (OutputStream out = Files.newOutputStream(CONFIG_FILE)) {
            prop.store(out, "RO LootManager config");
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
                if (relative.isEmpty()) {
                    zis.closeEntry();
                    continue;
                }
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

    public void copyDirectoryContents(Path src, Path dst) throws IOException {
        if (!Files.exists(src) || !Files.isDirectory(src)) return;
        try (java.util.stream.Stream<Path> stream = Files.walk(src)) {
            stream.filter(sourcePath -> !isHiddenPathInTree(src, sourcePath))
                    .forEach(sourcePath -> {
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

    private boolean isHiddenPathInTree(Path root, Path path) {
        if (path == null || root == null) {
            return false;
        }
        Path relative = root.relativize(path).normalize();
        if (relative.toString().isEmpty()) {
            return false;
        }
        for (Path segment : relative) {
            if (segment.toString().startsWith(".")) {
                return true;
            }
        }
        return false;
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

    public void clearResources() throws IOException {
        if (!Files.exists(RESOURCES_DIR) || !Files.isDirectory(RESOURCES_DIR)) return;
        try (var stream = Files.list(RESOURCES_DIR)) {
            for (Path entry : (Iterable<Path>) stream::iterator) {
                String name = entry.getFileName().toString();
                if (".default".equals(name)) {
                    continue;
                }
                if (Files.isDirectory(entry)) {
                    deleteDirectoryContents(entry);
                    Files.deleteIfExists(entry);
                    log("Deleted profile dir: " + entry.toAbsolutePath());
                } else {
                    Files.deleteIfExists(entry);
                    log("Deleted resource file: " + entry.toAbsolutePath());
                }
            }
        }

    }

    public void clearSelectedItemFolder() throws IOException {
        Path itemFolder = getSelectedGameItemFolder();
        if (itemFolder == null || !Files.exists(itemFolder) || !Files.isDirectory(itemFolder)) return;

        Path manifest = resolveManifestPath(itemFolder);
        List<String> managedSubfolders = readManifestManagedSubfolders(manifest);
        if (managedSubfolders != null && !managedSubfolders.isEmpty()) {
            deleteManagedSubfolders(itemFolder, managedSubfolders);
        }
        deleteManifestFiles(itemFolder, MANIFEST_FILE_NAME);
    }

    private void removeInstalledProfileFiles(Path destination) throws IOException {
        Path manifest = destination.resolve(MANIFEST_FILE_NAME);
        if (Files.exists(manifest) && Files.isRegularFile(manifest)) {
            List<String> managedSubfolders = readManifestManagedSubfolders(manifest);
            if (managedSubfolders != null && !managedSubfolders.isEmpty()) {
                deleteManagedSubfolders(destination, managedSubfolders);
            }
        }
        deleteManifestFiles(destination, MANIFEST_FILE_NAME);
    }

    private void normalizeInstalledManifest(Path destination) throws IOException {
        Path currentManifest = destination.resolve(MANIFEST_FILE_NAME);
        Path legacyManifest = destination.resolve(LEGACY_MANIFEST_FILE_NAME);
        if (Files.exists(legacyManifest) && Files.isRegularFile(legacyManifest)) {
            if (!Files.exists(currentManifest) || !Files.isRegularFile(currentManifest)) {
                Files.move(legacyManifest, currentManifest, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.deleteIfExists(legacyManifest);
            }
        }
    }

    private Path resolveManifestPath(Path directory) {
        return directory.resolve(MANIFEST_FILE_NAME);
    }

    private void deleteManifestFiles(Path directory, String... manifestNames) throws IOException {
        for (String manifestName : manifestNames) {
            if (manifestName == null || manifestName.isBlank()) continue;
            Files.deleteIfExists(directory.resolve(manifestName));
        }
    }

    public record AvailableProfile(
            String id,
            String name,
            String author,
            String description,
            String url,
            String createdAt,
            String version,
            long normalizedVersion,
            Path source,
            List<String> managedSubfolders,
            List<String> previewImages
    ) {
        public AvailableProfile(
                String id,
                String name,
                String author,
                String description,
                String url,
                String createdAt,
                String version,
                long normalizedVersion,
                Path source
        ) {
            this(id, name, author, description, url, createdAt, version, normalizedVersion, source, List.of(), List.of());
        }
    }

    public List<String> loadPreviewImages(Path profileDir) {
        Path previewDir = profileDir == null ? null : profileDir.resolve(".preview");
        if (previewDir == null || !Files.isDirectory(previewDir)) {
            return List.of();
        }
        try (var stream = Files.walk(previewDir)) {
            return stream.filter(Files::isRegularFile)
                    .filter(this::isSupportedPreviewImage)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER))
                    .map(this::toDataUrl)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (IOException e) {
            log("Failed to load preview images from " + previewDir.toAbsolutePath() + ": " + e.getMessage());
            return List.of();
        }
    }

    private boolean isSupportedPreviewImage(Path file) {
        if (file == null || !Files.isRegularFile(file)) {
            return false;
        }
        String name = file.getFileName().toString().toLowerCase();
        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".gif") || name.endsWith(".webp");
    }

    private String toDataUrl(Path file) {
        try (InputStream in = Files.newInputStream(file)) {
            byte[] bytes = in.readAllBytes();
            String mimeType = Files.probeContentType(file);
            if (mimeType == null) {
                String name = file.getFileName().toString().toLowerCase();
                if (name.endsWith(".png")) mimeType = "image/png";
                else if (name.endsWith(".jpg") || name.endsWith(".jpeg")) mimeType = "image/jpeg";
                else if (name.endsWith(".gif")) mimeType = "image/gif";
                else if (name.endsWith(".webp")) mimeType = "image/webp";
                else return null;
            }
            return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (IOException e) {
            log("Failed to encode preview image " + file.toAbsolutePath() + ": " + e.getMessage());
            return null;
        }
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
                        Path manifest = resolveManifestPath(p);
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
                    Path manifest = resolveManifestPath(p);
                    if (!Files.exists(manifest) || !Files.isRegularFile(manifest)) continue;
                    if (seen.add(name)) {
                        results.add(new AvailableProfile(
                                name,
                                readManifestName(manifest),
                                readManifestAuthor(manifest),
                                readManifestDescription(manifest),
                                readManifestUrl(manifest),
                                readManifestCreatedAt(manifest),
                                readManifestVersion(manifest),
                                normalizeVersion(readManifestVersion(manifest)),
                                p,
                                readManifestManagedSubfolders(manifest),
                                loadPreviewImages(p)
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
        installProfile(profileId, List.of());
    }

    public void installProfile(String profileId, List<String> disabledManagedSubfolders) throws IOException {
        Path destination = getSelectedGameItemFolder();
        if (destination == null) {
            throw new IllegalStateException("No game installation folder is selected.");
        }
        AvailableProfile selected = findAvailableProfile(profileId);

        removeInstalledProfileFiles(destination);
        copyDirectoryContents(selected.source(), destination);
        normalizeInstalledManifest(destination);
        setCurrentLootProfile(selected.id());

        if (disabledManagedSubfolders != null && !disabledManagedSubfolders.isEmpty()) {
            manageInstalledProfile(profileId, disabledManagedSubfolders);
        }
    }

    public void manageInstalledProfile(String profileId, List<String> disabledManagedSubfolders) throws IOException {
        Path destination = getSelectedGameItemFolder();
        if (destination == null) {
            throw new IllegalStateException("No game installation folder is selected.");
        }

        Path manifest = resolveManifestPath(destination);
        if (!Files.exists(manifest)) {
            throw new IllegalStateException("No installed profile found.");
        }

        List<String> managedSubfolders = readManifestManagedSubfolders(manifest);
        if (managedSubfolders == null || managedSubfolders.isEmpty()) {
            throw new IllegalStateException("Profile has no managed subfolders.");
        }

        disabledManagedSubfolders = disabledManagedSubfolders != null ? disabledManagedSubfolders : List.of();
        Set<String> normalizedDisabled = disabledManagedSubfolders.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> value.toLowerCase())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        for (String subfolder : managedSubfolders) {
            if (subfolder == null || subfolder.isBlank()) continue;
            Path relative = Paths.get(subfolder).normalize();
            if (relative.isAbsolute() || relative.startsWith("..")) continue;
            Path target = destination.resolve(relative).normalize();
            if (!target.startsWith(destination)) continue;

            Path parent = target.getParent();
            String targetName = target.getFileName().toString();
            if (parent == null) continue;

            Path disabledPath = parent.resolve("disabled_" + targetName);
            boolean shouldBeDisabled = normalizedDisabled.contains(subfolder.toLowerCase());
            boolean isCurrentlyDisabled = Files.exists(disabledPath) && Files.isDirectory(disabledPath);

            if (shouldBeDisabled && !isCurrentlyDisabled) {
                // Disable: rename folder to disabled_*
                if (Files.exists(target) && Files.isDirectory(target)) {
                    Files.move(target, disabledPath, StandardCopyOption.REPLACE_EXISTING);
                    log("Disabled managed subfolder: " + target.toAbsolutePath());
                }
            } else if (!shouldBeDisabled && isCurrentlyDisabled) {
                // Enable: rename folder back from disabled_*
                if (Files.exists(disabledPath) && Files.isDirectory(disabledPath)) {
                    Files.move(disabledPath, target, StandardCopyOption.REPLACE_EXISTING);
                    log("Enabled managed subfolder: " + disabledPath.toAbsolutePath());
                }
            }
        }
    }

    private AvailableProfile findAvailableProfile(String profileId) {
        String normalizedProfileId = profileId == null ? "" : profileId.trim();
        if (normalizedProfileId.isEmpty()) {
            throw new IllegalArgumentException("profileId is required.");
        }
        return listAvailableProfiles().stream()
                .filter(profile -> profile.id().equals(normalizedProfileId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Profile not found: " + normalizedProfileId));
    }

    public static final class ProfileInfo {
        public final String name;
        public final String author;
        public final String description;
        public final String url;
        public final String createdAt;
        public final String version;
        public final List<String> managedSubfolders;
        public final List<String> disabledManagedSubfolders;

        public ProfileInfo(String name, String author, String description, String url, String createdAt, String version, List<String> managedSubfolders, List<String> disabledManagedSubfolders) {
            this.name = name;
            this.author = author;
            this.description = description;
            this.url = url;
            this.createdAt = createdAt;
            this.version = version;
            this.managedSubfolders = managedSubfolders;
            this.disabledManagedSubfolders = disabledManagedSubfolders;
        }

        public ProfileInfo(String name, String author, String description, String url, String createdAt, String version) {
            this(name, author, description, url, createdAt, version, List.of(), List.of());
        }
    }

    public ProfileInfo getInstalledProfileInfo() {
        Path itemFolder = getSelectedGameItemFolder();
        if (itemFolder == null || !Files.exists(itemFolder)) {
            return null;
        }

        Path manifest = resolveManifestPath(itemFolder);
        if (!Files.exists(manifest)) {
            return null;
        }

        List<String> managedSubfolders = readManifestManagedSubfolders(manifest);
        return new ProfileInfo(
            readManifestName(manifest),
            readManifestAuthor(manifest),
            readManifestDescription(manifest),
            readManifestUrl(manifest),
            readManifestCreatedAt(manifest),
            readManifestVersion(manifest),
            managedSubfolders,
            readManifestDisabledManagedSubfolders(itemFolder, managedSubfolders)
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

    private List<String> readManifestManagedSubfolders(Path manifestFile) {
        List<String> subfolders = new ArrayList<>();
        if (manifestFile == null || !Files.exists(manifestFile) || !Files.isRegularFile(manifestFile)) {
            return null;
        }
        try {
            String content = Files.readString(manifestFile);
            java.util.regex.Matcher arrayMatcher = java.util.regex.Pattern.compile(
                    "\"managedSubfolders\"\\s*:\\s*\\[(.*?)]",
                    java.util.regex.Pattern.DOTALL
            ).matcher(content);
            if (!arrayMatcher.find()) return null;

            String arrayContent = arrayMatcher.group(1);
            java.util.regex.Matcher itemMatcher = java.util.regex.Pattern.compile(
                    "\"((?:\\\\.|[^\"\\\\])*)\""
            ).matcher(arrayContent);
            while (itemMatcher.find()) {
                String raw = itemMatcher.group(1)
                        .replace("\\n", "\n")
                        .replace("\\\"", "\"")
                        .replace("\\\\", "\\")
                        .trim();
                if (!raw.isEmpty()) subfolders.add(raw);
            }
            return subfolders;
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> readManifestDisabledManagedSubfolders(Path itemFolder, List<String> managedSubfolders) {
        if (itemFolder == null || !Files.exists(itemFolder) || managedSubfolders == null || managedSubfolders.isEmpty()) {
            return List.of();
        }

        List<String> disabled = new ArrayList<>();
        for (String subfolder : managedSubfolders) {
            if (subfolder == null || subfolder.isBlank()) continue;
            Path relative = Paths.get(subfolder).normalize();
            if (relative.isAbsolute() || relative.startsWith("..")) continue;

            Path target = itemFolder.resolve(relative).normalize();
            Path disabledTarget = target.getParent() == null
                    ? itemFolder.resolve("disabled_" + target.getFileName())
                    : target.getParent().resolve("disabled_" + target.getFileName());

            if (!Files.exists(target) && Files.exists(disabledTarget) && Files.isDirectory(disabledTarget)) {
                disabled.add(subfolder);
            }
        }
        return disabled;
    }

    private void deleteManagedSubfolders(Path baseDir, List<String> managedSubfolders) throws IOException {
        if (managedSubfolders == null || managedSubfolders.isEmpty()) return;
        for (String subfolder : managedSubfolders) {
            if (subfolder == null || subfolder.isBlank()) continue;
            Path relative = Paths.get(subfolder).normalize();
            if (relative.isAbsolute() || relative.startsWith("..")) {
                log("Skipping invalid managedSubfolder path: " + subfolder);
                continue;
            }
            Path target = baseDir.resolve(relative).normalize();
            if (!target.startsWith(baseDir)) {
                log("Skipping out-of-scope managedSubfolder path: " + subfolder);
                continue;
            }
            if (Files.exists(target) && Files.isDirectory(target)) {
                deleteDirectoryContents(target);
                Files.deleteIfExists(target);
                log("Deleted managed subfolder: " + target.toAbsolutePath());
            }
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

    public ResourcesUpdateCheckResult checkResourcesUpdate() {
        Path localManifest = RESOURCES_DIR.resolve("manifest.json");
        boolean localExists = Files.exists(localManifest) && Files.isRegularFile(localManifest);
        String localVersion = localExists ? readManifestVersion(localManifest) : "none";

        String[] branches = {"main", "master"};
        String repoUrl = DEFAULT_REPO;
        if (repoUrl.endsWith("/")) repoUrl = repoUrl.substring(0, repoUrl.length() - 1);
        String rawBase = repoUrl
                .replace("https://github.com/", "https://raw.githubusercontent.com/");

        for (String branch : branches) {
            String remoteUrl = rawBase + "/" + branch + "/manifest.json";
            try {
                InputStream in = openUrlStream(remoteUrl);
                if (in == null) continue;
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
                log("Remote manifest check failed for branch " + branch + ": " + e.getMessage());
            }
        }
        return new ResourcesUpdateCheckResult(localVersion, "unknown", localExists, false, false,
                "Unable to check remote manifest.");
    }

    public record ResourcesUpdateCheckResult(
            String localVersion,
            String remoteVersion,
            boolean localExists,
            boolean updateAvailable,
            boolean success,
            String message
    ) {}

}
