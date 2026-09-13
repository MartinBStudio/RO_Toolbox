package com.bstudio.ro_toolbox.service.common;

import com.bstudio.ro_toolbox.service.textureReplacer.AvailablePackage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public interface ICommonMethods {
    default String absoluteOrNull(Path path) {
        return path == null ? null : path.toAbsolutePath().normalize().toString();
    }

    default List<String> loadPreviewImages(Path profileDir) {
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
            System.out.println(e);
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
            return null;
        }
    }

    default List<AvailablePackage> listAvailableProfiles(Path resourcesDir, Path selectedGameBase, String manifestFileName) {
        List<AvailablePackage> results = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        List<Path> roots = new ArrayList<>();
        roots.add(resourcesDir);
        if (selectedGameBase != null) {
            roots.add(selectedGameBase.resolveSibling(resourcesDir.getFileName()));
        }

        for (Path root : roots) {
            if (root == null || !Files.exists(root) || !Files.isDirectory(root)) continue;
            try (var stream = Files.list(root)) {
                for (Path p : (Iterable<Path>) stream::iterator) {
                    if (!Files.isDirectory(p)) continue;
                    String name = p.getFileName().toString();
                    if (name.startsWith(".")) continue;
                    Path manifest = resolveManifestPath(p, manifestFileName);
                    if (manifest == null) continue;
                    if (seen.add(name)) {
                        AvailablePackage availablePackage = AvailablePackage.builder().id(readManifestName(manifest)).name(readManifestName(manifest)).author(readManifestAuthor(manifest)).description(readManifestDescription(manifest)).url(readManifestUrl(manifest)).createdAt(readManifestCreatedAt(manifest)).version(readManifestVersion(manifest)).previewImages(loadPreviewImages(p)).source(p).build();
                        results.add(availablePackage);
                    }
                }
            } catch (IOException ignored) {
            }
        }

        results.sort((a, b) -> {
            int versionDiff = Long.compare(b.getNormalizedVersion(), a.getNormalizedVersion());
            if (versionDiff != 0) return versionDiff;
            return a.getId().compareToIgnoreCase(b.getId());
        });
        return results;
    }
    default List<String> listDownloadedProfiles(Path resourcesDir,String manifestFileName) {
        List<String> profiles = new ArrayList<>();
        if (resourcesDir == null || !Files.exists(resourcesDir) || !Files.isDirectory(resourcesDir)) {
            return profiles;
        }
        try (var stream = Files.list(resourcesDir)) {
            stream.filter(Files::isDirectory)
                    .forEach(p -> {
                        String name = p.getFileName().toString();
                        if (name.startsWith(".")) {
                            return;
                        }
                        if (hasProfileAssets(p, manifestFileName)) {
                            profiles.add(p.getFileName().toString());
                        }
                    });
        } catch (IOException ignored) {
        }
        profiles.sort(String::compareToIgnoreCase);
        return profiles;
    }
    default boolean hasProfileAssets(Path directory,String manifestFileName) {
        if (directory == null || !Files.isDirectory(directory)) {
            return false;
        }
        return resolveManifestPath(directory, manifestFileName) != null;
    }
    default String readManifestDescription(Path manifestFile) {
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

    default String readManifestUrl(Path manifestFile) {
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

    default String readManifestAuthor(Path manifestFile) {
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

    default String readManifestCreatedAt(Path manifestFile) {
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

    default String readManifestVersion(Path manifestFile) {
        try {
            String content = Files.readString(manifestFile);
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\"version\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"").matcher(content);
            if (!matcher.find()) return "0.0.0";
            return matcher.group(1).trim();
        } catch (Exception e) {
            return "0.0.0";
        }
    }

    default List<String> readManifestDisabledManagedSubfolders(Path itemFolder, List<String> managedSubfolders) {
        if (itemFolder == null || !Files.exists(itemFolder) || managedSubfolders == null || managedSubfolders.isEmpty()) {
            return List.of();
        }

        List<String> disabled = new ArrayList<>();
        for (String subfolder : managedSubfolders) {
            if (subfolder == null || subfolder.isBlank()) continue;
            Path relative = Paths.get(subfolder).normalize();
            if (relative.isAbsolute() || relative.startsWith("..")) continue;

            Path target = itemFolder.resolve(relative).normalize();
            Path disabledTarget = resolveDisabledManagedSubfolderPath(target);

            if (disabledTarget != null && Files.exists(disabledTarget) && Files.isDirectory(disabledTarget)) {
                disabled.add(subfolder);
            }
        }
        return disabled;
    }
    default Path resolveDisabledManagedSubfolderPath(Path target) {
        if (target == null || target.getFileName() == null) {
            return null;
        }

        Path parent = target.getParent();
        if (parent == null) {
            return null;
        }

        return parent.resolve("disabled_" + target.getFileName());
    }
    default List<String> readManifestManagedSubfolders(Path manifestFile) {
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

    default String readManifestField(Path manifestFile, String... keys) {
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

    default String readManifestName(Path manifestFile) {
        return readManifestField(manifestFile, "name");
    }

    default Path resolveManifestPath(Path directory, String manifestFileName) {
        return directory.resolve(manifestFileName);
    }
}
