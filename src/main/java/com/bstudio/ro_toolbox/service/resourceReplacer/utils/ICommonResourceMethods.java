package com.bstudio.ro_toolbox.service.resourceReplacer.utils;

import com.bstudio.ro_toolbox.service.resourceReplacer.model.Resource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

public interface ICommonResourceMethods {
  default String absoluteOrNull(Path path) {
    return path == null ? null : path.toAbsolutePath().normalize().toString();
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

  default Resource findSelectedPackage(String profileId, List<Resource> availableProfiles) {
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
