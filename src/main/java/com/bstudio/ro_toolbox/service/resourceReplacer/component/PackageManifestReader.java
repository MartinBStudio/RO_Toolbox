package com.bstudio.ro_toolbox.service.resourceReplacer.component;

import com.bstudio.ro_toolbox.service.resourceReplacer.model.Resource;
import com.bstudio.ro_toolbox.service.resourceReplacer.utils.ICommonResourceMethods;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class PackageManifestReader implements ICommonResourceMethods {

  public Resource readManifest(String manifestFileName, Path installedPackageFolder) {
    if (installedPackageFolder == null || !Files.exists(installedPackageFolder)) {
      return null;
    }
    Path manifest = resolveManifestPath(installedPackageFolder, manifestFileName);
    if (!Files.isRegularFile(manifest)) {
      return null;
    }
    return Resource.builder()
        .id(readManifestName(manifest))
        .name(readManifestName(manifest))
        .author(readManifestAuthor2(manifest))
        .description(readManifestDesc2(manifest))
        .url(readManifestUrl2(manifest))
        .createdAt(readManifestCreatedAt2(manifest))
        .version(readManifestVersion2(manifest))
        .managedSubfolders(readManifestManagedSubfolders(manifest))
        .disabledManagedSubfolders(
            readManifestDisabledManagedSubfolders(
                manifest.getParent(), readManifestManagedSubfolders(manifest)))
        .build();
  }

  public void deleteManifestFiles(Path directory, String... manifestNames) throws IOException {
    for (String manifestName : manifestNames) {
      if (manifestName == null || manifestName.isBlank()) continue;
      Files.deleteIfExists(directory.resolve(manifestName));
    }
  }

  public Path resolveManifestPath(Path directory, String manifestFileName) {
    return directory.resolve(manifestFileName);
  }

  public List<String> readManifestManagedSubfolders(Path manifestFile) {
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

  public String readManifestVersion2(Path manifestFile) {
    return readManifestField(manifestFile, "version");
  }

  private String readManifestName(Path manifestFile) {
    return readManifestField(manifestFile, "name");
  }

  private String readManifestAuthor2(Path manifestFile) {
    return readManifestField(manifestFile, "author");
  }

  private String readManifestDesc2(Path manifestFile) {
    return readManifestField(manifestFile, "description");
  }

  private String readManifestUrl2(Path manifestFile) {
    return readManifestField(manifestFile, "url");
  }

  private String readManifestCreatedAt2(Path manifestFile) {
    return readManifestField(manifestFile, "createdAt");
  }

  private String readManifestField(Path manifestFile, String... keys) {
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

  private List<String> readManifestDisabledManagedSubfolders(
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

  private List<String> loadPreviewImages(Path profileDir) {
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
}
