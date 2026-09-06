package com.bstudio.ro_toolbox.service.buffIcons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
public class BuffIconsManagerService {
    private static final String DEFAULT_REPO = "https://github.com/MartinBStudio/RO_BuffIcons_resources";
    private static final String RESOURCE_MANIFEST_FILE_NAME = "manifest.json";
    private static final String PROFILE_MANIFEST_FILE_NAME = "manifestBuffIcons.json";
    private static final Path APP_DATA_ROOT = resolveAppDataRoot();
    private static final Path RESOURCES_DIR = APP_DATA_ROOT.resolve("resources").resolve("buffIcons");
    private static final Path GAME_SUFFIX = Paths.get("");

    private static final Path CONFIG_DIR = APP_DATA_ROOT.resolve("config");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("config.properties");

    private static Path resolveAppDataRoot() {
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) {
            return Paths.get(appData, "RO_Toolbox");
        }
        return Paths.get(System.getProperty("user.home"), ".ro_toolbox");
    }

    private static final Logger LOG = LoggerFactory.getLogger(BuffIconsManagerService.class);

    private volatile Path selectedGameBase = null;

    public BuffIconsManagerService() {
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

    public Path getResourcesDir() {
        return RESOURCES_DIR;
    }

    public Path getSelectedGameBase() {
        return selectedGameBase;
    }

    public Path getSelectedGameItemFolder() {
        return selectedGameBase == null ? null : selectedGameBase.resolve(GAME_SUFFIX);
    }

    private void loadConfig() {
        try {
            if (!Files.exists(CONFIG_FILE)) {
                return;
            }
            Properties properties = new Properties();
            try (InputStream in = Files.newInputStream(CONFIG_FILE)) {
                properties.load(in);
            }
            String selected = properties.getProperty("selectedGame");
            if (selected != null && !selected.isBlank()) {
                Path candidate = Paths.get(selected.trim());
                if (Files.exists(candidate)) {
                    selectedGameBase = candidate;
                }
            }
        } catch (Exception ignored) {
        }
    }

    public void saveSelectedGame(Path base) {
        try {
            if (base == null) {
                clearSelectedGame();
                return;
            }

            Files.createDirectories(CONFIG_DIR);
            Properties properties = new Properties();
            if (Files.exists(CONFIG_FILE)) {
                try (InputStream in = Files.newInputStream(CONFIG_FILE)) {
                    properties.load(in);
                }
            }
            properties.setProperty("selectedGame", base.toAbsolutePath().toString());
            try (OutputStream out = Files.newOutputStream(CONFIG_FILE)) {
                properties.store(out, "RO BuffIcons config");
            }
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
            Properties properties = new Properties();
            if (Files.exists(CONFIG_FILE)) {
                try (InputStream in = Files.newInputStream(CONFIG_FILE)) {
                    properties.load(in);
                }
            }
            properties.remove("selectedGame");
            try (OutputStream out = Files.newOutputStream(CONFIG_FILE)) {
                properties.store(out, "RO BuffIcons config");
            }
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

    public void downloadAndExtract(String repoUrl, Path destDir) throws IOException {
        if (repoUrl == null || repoUrl.isBlank()) {
            repoUrl = DEFAULT_REPO;
        }
        if (!Files.exists(destDir)) {
            Files.createDirectories(destDir);
        }

        String[] branches = {"main", "master"};
        IOException lastException = null;

        for (String branch : branches) {
            String zipUrl = buildZipUrl(repoUrl, branch);
            log("Trying branch: " + branch + " -> " + zipUrl);
            try {
                Path tmp = Files.createTempFile("repo-", ".zip");
                try (InputStream in = openUrlStream(zipUrl)) {
                    if (in == null) {
                        throw new IOException("Not found: " + zipUrl);
                    }
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
        if (repoUrl.endsWith("/")) {
            repoUrl = repoUrl.substring(0, repoUrl.length() - 1);
        }
        return repoUrl + "/archive/refs/heads/" + branch + ".zip";
    }

    private InputStream openUrlStream(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("User-Agent", "RO_BuffIconsManager/1.0");
        conn.setInstanceFollowRedirects(true);
        int code = conn.getResponseCode();
        if (code >= 200 && code < 300) {
            return conn.getInputStream();
        }
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
                        while ((len = zis.read(buf)) > 0) {
                            os.write(buf, 0, len);
                        }
                    }
                }
                log("Extracted: " + relative);
                zis.closeEntry();
            }
        }
    }

    public void copyDirectoryContents(Path src, Path dst) throws IOException {
        if (!Files.exists(src) || !Files.isDirectory(src)) {
            return;
        }
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
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            return;
        }
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
        if (!Files.exists(RESOURCES_DIR) || !Files.isDirectory(RESOURCES_DIR)) {
            return;
        }
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
        Path gameBase = getSelectedGameBase();
        if (gameBase == null || !Files.exists(gameBase) || !Files.isDirectory(gameBase)) {
            return;
        }

        deleteInstalledFiles(gameBase);
    }

    public void installProfile(String profileId) throws IOException {
        Path destination = getSelectedGameItemFolder();
        if (destination == null) {
            throw new IllegalStateException("No game installation folder is selected.");
        }
        AvailableProfile selected = findAvailableProfile(profileId);

        deleteInstalledFiles(destination);
        copyDirectoryContents(selected.source(), destination);
    }

    private void deleteInstalledFiles(Path baseDir) throws IOException {
        if (baseDir == null) {
            return;
        }
        Files.deleteIfExists(baseDir.resolve(PROFILE_MANIFEST_FILE_NAME));
        Files.deleteIfExists(baseDir.resolve(Paths.get("3ddata", "control", "Res", "stateicon.dds")));
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
            this(id, name, author, description, url, createdAt, version, normalizedVersion, source, List.of());
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
                if (name.endsWith(".png")) {
                    mimeType = "image/png";
                } else if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
                    mimeType = "image/jpeg";
                } else if (name.endsWith(".gif")) {
                    mimeType = "image/gif";
                } else if (name.endsWith(".webp")) {
                    mimeType = "image/webp";
                } else {
                    return null;
                }
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
                        Path manifest = p.resolve(PROFILE_MANIFEST_FILE_NAME);
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
            if (root == null || !Files.exists(root) || !Files.isDirectory(root)) {
                continue;
            }
            try (var stream = Files.walk(root)) {
                for (Path p : (Iterable<Path>) stream::iterator) {
                    if (!Files.isDirectory(p)) {
                        continue;
                    }
                    String name = p.getFileName().toString();
                    if (name.startsWith(".")) {
                        continue;
                    }
                    Path manifest = p.resolve(PROFILE_MANIFEST_FILE_NAME);
                    if (!Files.exists(manifest) || !Files.isRegularFile(manifest)) {
                        continue;
                    }
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
                                loadPreviewImages(p)
                        ));
                    }
                }
            } catch (IOException ignored) {
            }
        }

        results.sort((a, b) -> {
            int versionDiff = Long.compare(b.normalizedVersion(), a.normalizedVersion());
            if (versionDiff != 0) {
                return versionDiff;
            }
            return a.id().compareToIgnoreCase(b.id());
        });
        return results;
    }

    public static final class ProfileInfo {
        public final String name;
        public final String author;
        public final String description;
        public final String url;
        public final String createdAt;
        public final String version;

        public ProfileInfo(String name, String author, String description, String url, String createdAt, String version) {
            this.name = name;
            this.author = author;
            this.description = description;
            this.url = url;
            this.createdAt = createdAt;
            this.version = version;
        }
    }

    public ProfileInfo getInstalledProfileInfo() {
        Path itemFolder = getSelectedGameItemFolder();
        if (itemFolder == null || !Files.exists(itemFolder)) {
            return null;
        }

        Path manifest = itemFolder.resolve(PROFILE_MANIFEST_FILE_NAME);
        if (!Files.exists(manifest) || !Files.isRegularFile(manifest)) {
            return null;
        }

        return new ProfileInfo(
                readManifestName(manifest),
                readManifestAuthor(manifest),
                readManifestDescription(manifest),
                readManifestUrl(manifest),
                readManifestCreatedAt(manifest),
                readManifestVersion(manifest)
        );
    }

    private String readManifestField(Path manifestFile, String... keys) {
        try {
            String content = Files.readString(manifestFile);
            for (String key : keys) {
                if (key == null || key.isBlank()) {
                    continue;
                }
                java.util.regex.Matcher matcher = java.util.regex.Pattern
                        .compile("\"" + java.util.regex.Pattern.quote(key) + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
                        .matcher(content);
                if (matcher.find()) {
                    return matcher.group(1).replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\").trim();
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private String readManifestName(Path manifestFile) {
        return readManifestField(manifestFile, "name");
    }

    private String readManifestDescription(Path manifestFile) {
        return readManifestField(manifestFile, "description");
    }

    private String readManifestUrl(Path manifestFile) {
        return readManifestField(manifestFile, "url", "repository");
    }

    private String readManifestAuthor(Path manifestFile) {
        return readManifestField(manifestFile, "author");
    }

    private String readManifestCreatedAt(Path manifestFile) {
        return readManifestField(manifestFile, "createdAt", "timestamp");
    }

    private String readManifestVersion(Path manifestFile) {
        String version = readManifestField(manifestFile, "version");
        return version == null ? "0.0.0" : version;
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

    public ResourcesUpdateCheckResult checkResourcesUpdate() {
        Path localManifest = RESOURCES_DIR.resolve(RESOURCE_MANIFEST_FILE_NAME);
        boolean localExists = Files.exists(localManifest) && Files.isRegularFile(localManifest);
        String localVersion = localExists ? readManifestVersion(localManifest) : "none";

        String[] branches = {"main", "master"};
        String repoUrl = DEFAULT_REPO;
        if (repoUrl.endsWith("/")) {
            repoUrl = repoUrl.substring(0, repoUrl.length() - 1);
        }
        String rawBase = repoUrl.replace("https://github.com/", "https://raw.githubusercontent.com/");

        for (String branch : branches) {
            String remoteUrl = rawBase + "/" + branch + "/manifest.json?cb=" + System.currentTimeMillis();
            try {
                InputStream in = openUrlStream(remoteUrl);
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
                log("Remote manifest check failed for branch " + branch + ": " + e.getMessage());
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
