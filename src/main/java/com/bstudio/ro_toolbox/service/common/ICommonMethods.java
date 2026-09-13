package com.bstudio.ro_toolbox.service.common;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public interface ICommonMethods {
    default String absoluteOrNull(Path path) {
        return path == null ? null : path.toAbsolutePath().normalize().toString();
    }

    default List<String> loadPreviewImages(Path profileDir) {
        System.out.println(profileDir);
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
}
