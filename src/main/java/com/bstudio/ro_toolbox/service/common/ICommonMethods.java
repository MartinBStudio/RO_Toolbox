package com.bstudio.ro_toolbox.service.common;

import com.bstudio.ro_toolbox.service.textureReplacer.ResourcePackage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
      return stream
          .filter(Files::isRegularFile)
          .filter(this::isSupportedPreviewImage)
          .sorted(
              Comparator.comparing(
                  path -> path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER))
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
    return name.endsWith(".png")
        || name.endsWith(".jpg")
        || name.endsWith(".jpeg")
        || name.endsWith(".gif")
        || name.endsWith(".webp");
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

  default List<ResourcePackage> listAvailablePackages(
      Path resourcesDir, Path selectedGameBase, String manifestFileName) {
    List<ResourcePackage> results = new ArrayList<>();
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
            ResourcePackage resourcePackage =
                ResourcePackage.builder()
                    .id(readManifestName(manifest))
                    .name(readManifestName(manifest))
                    .author(readManifestAuthor(manifest))
                    .description(readManifestDescription(manifest))
                    .url(readManifestUrl(manifest))
                    .createdAt(readManifestCreatedAt(manifest))
                    .version(readManifestVersion(manifest))
                    .previewImages(loadPreviewImages(p))
                    .source(p)
                    .build();
            results.add(resourcePackage);
          }
        }
      } catch (IOException ignored) {
      }
    }

    results.sort(
        (a, b) -> {
          int versionDiff = Long.compare(b.getNormalizedVersion(), a.getNormalizedVersion());
          if (versionDiff != 0) return versionDiff;
          return a.getId().compareToIgnoreCase(b.getId());
        });
    return results;
  }

  default String readManifestDescription(Path manifestFile) {
    try {
      String content = Files.readString(manifestFile);
      java.util.regex.Matcher matcher =
          java.util.regex.Pattern.compile("\"description\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
              .matcher(content);
      if (!matcher.find()) return null;
      String value =
          matcher.group(1).replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
      return value.trim();
    } catch (Exception e) {
      return null;
    }
  }

  default String readManifestUrl(Path manifestFile) {
    try {
      String content = Files.readString(manifestFile);
      java.util.regex.Matcher matcher =
          java.util.regex.Pattern.compile("\"url\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
              .matcher(content);
      if (!matcher.find()) return null;
      String value =
          matcher.group(1).replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
      return value.trim();
    } catch (Exception e) {
      return null;
    }
  }

  default String readManifestAuthor(Path manifestFile) {
    try {
      String content = Files.readString(manifestFile);
      java.util.regex.Matcher matcher =
          java.util.regex.Pattern.compile("\"author\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
              .matcher(content);
      if (!matcher.find()) return null;
      String value =
          matcher.group(1).replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
      return value.trim();
    } catch (Exception e) {
      return null;
    }
  }

  default String readManifestCreatedAt(Path manifestFile) {
    try {
      String content = Files.readString(manifestFile);
      java.util.regex.Matcher matcher =
          java.util.regex.Pattern.compile("\"createdAt\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
              .matcher(content);
      if (!matcher.find()) return null;
      String value =
          matcher.group(1).replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
      return value.trim();
    } catch (Exception e) {
      return null;
    }
  }

  default String readManifestVersion(Path manifestFile) {
    try {
      String content = Files.readString(manifestFile);
      java.util.regex.Matcher matcher =
          java.util.regex.Pattern.compile("\"version\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
              .matcher(content);
      if (!matcher.find()) return "0.0.0";
      return matcher.group(1).trim();
    } catch (Exception e) {
      return "0.0.0";
    }
  }

  default List<String> readManifestDisabledManagedSubfolders(
      Path itemFolder, List<String> managedSubfolders) {
    if (itemFolder == null
        || !Files.exists(itemFolder)
        || managedSubfolders == null
        || managedSubfolders.isEmpty()) {
      return List.of();
    }

    List<String> disabled = new ArrayList<>();
    for (String subfolder : managedSubfolders) {
      if (subfolder == null || subfolder.isBlank()) continue;
      Path relative = Paths.get(subfolder).normalize();
      if (relative.isAbsolute() || relative.startsWith("..")) continue;

      Path target = itemFolder.resolve(relative).normalize();
      Path disabledTarget = resolveDisabledManagedSubfolderPath(target);

      if (disabledTarget != null
          && Files.exists(disabledTarget)
          && Files.isDirectory(disabledTarget)) {
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
      java.util.regex.Matcher arrayMatcher =
          java.util.regex.Pattern.compile(
                  "\"managedSubfolders\"\\s*:\\s*\\[(.*?)]", java.util.regex.Pattern.DOTALL)
              .matcher(content);
      if (!arrayMatcher.find()) return null;

      String arrayContent = arrayMatcher.group(1);
      java.util.regex.Matcher itemMatcher =
          java.util.regex.Pattern.compile("\"((?:\\\\.|[^\"\\\\])*)\"").matcher(arrayContent);
      while (itemMatcher.find()) {
        String raw =
            itemMatcher
                .group(1)
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
        java.util.regex.Matcher matcher =
            java.util.regex.Pattern.compile(
                    "\""
                        + java.util.regex.Pattern.quote(key)
                        + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
                .matcher(content);
        if (matcher.find()) {
          return matcher
              .group(1)
              .replace("\\n", "\n")
              .replace("\\\"", "\"")
              .replace("\\\\", "\\")
              .trim();
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

  default void copyDirectoryContents(Path src, Path dst) throws IOException {
    if (!Files.exists(src) || !Files.isDirectory(src)) return;
    try (java.util.stream.Stream<Path> stream = Files.walk(src)) {
      stream
          .filter(sourcePath -> !isHiddenPathInTree(src, sourcePath))
          .forEach(
              sourcePath -> {
                try {
                  Path rel = src.relativize(sourcePath);
                  Path targetPath = dst.resolve(rel);
                  if (Files.isDirectory(sourcePath)) {
                    Files.createDirectories(targetPath);
                  } else {
                    Files.createDirectories(targetPath.getParent());
                    Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
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

  default void deleteDirectoryContents(Path dir) throws IOException {
    if (!Files.exists(dir) || !Files.isDirectory(dir)) {
      return;
    }
    Files.walkFileTree(
        dir,
        new java.nio.file.SimpleFileVisitor<Path>() {
          @Override
          public java.nio.file.FileVisitResult visitFile(
              Path file, java.nio.file.attribute.BasicFileAttributes attrs) throws IOException {
            Files.deleteIfExists(file);
            return java.nio.file.FileVisitResult.CONTINUE;
          }

          @Override
          public java.nio.file.FileVisitResult postVisitDirectory(Path visitedDir, IOException exc)
              throws IOException {
            if (!visitedDir.equals(dir)) {
              Files.deleteIfExists(visitedDir);
            }
            return java.nio.file.FileVisitResult.CONTINUE;
          }
        });
  }

  default void deleteManifestFiles(Path directory, String... manifestNames) throws IOException {
    for (String manifestName : manifestNames) {
      if (manifestName == null || manifestName.isBlank()) continue;
      Files.deleteIfExists(directory.resolve(manifestName));
    }
  }

  default void clearResources(Path resourcesDir) throws IOException {
    if (!Files.exists(resourcesDir) || !Files.isDirectory(resourcesDir)) return;
    try (var stream = Files.list(resourcesDir)) {
      for (Path entry : (Iterable<Path>) stream::iterator) {
        String name = entry.getFileName().toString();
        if (".default".equals(name)) {
          continue;
        }
        if (Files.isDirectory(entry)) {
          deleteDirectoryContents(entry);
          Files.deleteIfExists(entry);
        } else {
          Files.deleteIfExists(entry);
        }
      }
    }
  }

  default ResourcePackage findSelectedProfile(
      String profileId, List<ResourcePackage> availableProfiles) {
    String normalizedProfileId = profileId == null ? "" : profileId.trim();
    if (normalizedProfileId.isEmpty()) {
      throw new IllegalArgumentException("profileId is required.");
    }
    return availableProfiles.stream()
        .filter(profile -> profile.getId().equals(normalizedProfileId))
        .findFirst()
        .orElseThrow(
            () -> new IllegalArgumentException("Profile not found: " + normalizedProfileId));
  }

  default List<Path> readDefaultFileList(Path fileListPath) throws IOException {
    if (fileListPath == null || !Files.exists(fileListPath) || !Files.isRegularFile(fileListPath)) {
      return Collections.emptyList();
    }
    List<Path> files = new ArrayList<>();
    for (String line : Files.readAllLines(fileListPath)) {
      if (line == null) continue;
      String trimmed = line.trim();
      if (trimmed.isEmpty()) continue;
      Path relative = Paths.get(trimmed.replace("\\", "/")).normalize();
      if (relative.isAbsolute() || relative.startsWith("..")) {
        continue;
      }
      files.add(relative);
    }
    return files;
  }

  default Path resolveManagedFile(Path root, Path relative) {
    Path target = root.resolve(relative).normalize();
    if (!target.startsWith(root)) {
      throw new IllegalStateException(
          "Resolved path escapes root for FILE_LIST entry: " + relative);
    }
    return target;
  }
  default void deleteManagedSubfolders(Path baseDir, List<String> managedSubfolders)
          throws IOException {
    if (managedSubfolders == null || managedSubfolders.isEmpty()) return;
    for (String subfolder : managedSubfolders) {
      if (subfolder == null || subfolder.isBlank()) continue;
      Path relative = Paths.get(subfolder).normalize();
      if (relative.isAbsolute() || relative.startsWith("..")) {
        continue;
      }
      Path target = baseDir.resolve(relative).normalize();
      if (!target.startsWith(baseDir)) {
        continue;
      }
      if (Files.exists(target) && Files.isDirectory(target)) {
        deleteDirectoryContents(target);
        Files.deleteIfExists(target);
      }

      Path disabledTarget = resolveDisabledManagedSubfolderPath(target);
      if (disabledTarget != null
              && Files.exists(disabledTarget)
              && Files.isDirectory(disabledTarget)) {
        deleteDirectoryContents(disabledTarget);
        Files.deleteIfExists(disabledTarget);
      }
    }
  }
}
