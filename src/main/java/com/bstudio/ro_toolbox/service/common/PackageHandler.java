package com.bstudio.ro_toolbox.service.common;

import com.bstudio.ro_toolbox.service.textureReplacer.ResourcePackage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PackageHandler implements ICommonMethods {

    private final PackageManifestReader packageManifestReader;

    public List<ResourcePackage> listAvailablePackages(
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
                    Path manifest = packageManifestReader.resolveManifestPath(p, manifestFileName);
                    if (manifest == null) continue;
                    if (seen.add(name)) {
                        var mani = packageManifestReader.readManifest(manifestFileName, p);
                        mani.setPreviewImages(loadPreviewImages(p));
                        mani.setSource(p);
                        results.add(mani);
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

    public void manageInstalledPackage(String profileId, List<String> disabledManagedSubfolders, Path destination, String manifestFileName)
            throws IOException {

        if (disabledManagedSubfolders == null) {
            log.info("No managed subfolders to disable or enable.");
            return;
        }
        if(disabledManagedSubfolders.isEmpty()) {
            log.info("No managed subfolders to disable or enable.");
        }
        if (destination == null) {
            throw new IllegalStateException("No game installation folder is selected.");
        }

        Path manifest = packageManifestReader.resolveManifestPath(destination, manifestFileName);
        if (!Files.exists(manifest)) {
            throw new IllegalStateException("No installed profile found.");
        }

        List<String> managedSubfolders = packageManifestReader.readManifestManagedSubfolders(manifest);
        if (managedSubfolders == null || managedSubfolders.isEmpty()) {
            throw new IllegalStateException("Profile has no managed subfolders.");
        }

        disabledManagedSubfolders =
                disabledManagedSubfolders != null ? disabledManagedSubfolders : List.of();
        Set<String> normalizedDisabled =
                disabledManagedSubfolders.stream()
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
            boolean shouldBeDisabled = normalizedDisabled.contains(subfolder.trim().toLowerCase());
            boolean isCurrentlyDisabled = Files.exists(disabledPath) && Files.isDirectory(disabledPath);

            if (shouldBeDisabled && !isCurrentlyDisabled) {
                // Disable: rename folder to disabled_*
                if (Files.exists(target) && Files.isDirectory(target)) {
                    moveDirectoryReplacingExisting(target, disabledPath);
                    log.info("Disabled managed subfolder: " + target.toAbsolutePath());
                }
            } else if (!shouldBeDisabled && isCurrentlyDisabled) {
                // Enable: rename folder back from disabled_*
                if (Files.exists(disabledPath) && Files.isDirectory(disabledPath)) {
                    moveDirectoryReplacingExisting(disabledPath, target);
                    log.info("Enabled managed subfolder: " + disabledPath.toAbsolutePath());
                }
            }
        }
    }

    private void moveDirectoryReplacingExisting(Path source, Path target) throws IOException {
        if (source == null || target == null) {
            return;
        }
        if (Files.exists(target)) {
            if (!Files.isDirectory(target)) {
                Files.deleteIfExists(target);
            } else {
                deleteDirectoryContents(target);
                Files.deleteIfExists(target);
            }
        }
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    public void uninstallPackageFolders(Path packageDir,String manifestFileName) throws IOException {
        if (packageDir == null || !Files.exists(packageDir) || !Files.isDirectory(packageDir)) return;

        Path manifest = packageManifestReader.resolveManifestPath(packageDir, manifestFileName);
        List<String> managedSubfolders = packageManifestReader.readManifestManagedSubfolders(manifest);
        if (managedSubfolders != null && !managedSubfolders.isEmpty()) {
            deleteManagedSubfolders(packageDir, managedSubfolders);
        }
        packageManifestReader.deleteManifestFiles(packageDir, manifestFileName);
    }

    public void uninstallPackageFiles(Path installedDir,Path resourcesDir, String manifestFileName) throws IOException {
        if (installedDir == null || !Files.exists(installedDir) || !Files.isDirectory(installedDir)) return;

        // Always clear the installed manifest so the app no longer shows the profile as installed
        packageManifestReader.deleteManifestFiles(installedDir, manifestFileName);

        // Restore original files from .default if available
        Path defaultProfile = resourcesDir.resolve(".default");
        if (!Files.exists(defaultProfile) || !Files.isDirectory(defaultProfile)) return;

        List<Path> managedFiles = readDefaultFileList(defaultProfile.resolve("FILE_LIST.txt"));
        for (Path relativeFile : managedFiles) {
            Path defaultFile = defaultProfile.resolve(relativeFile);
            Path target = resolveManagedFile(installedDir, relativeFile);
            if (Files.isDirectory(target)) {
                continue;
            }
            Files.deleteIfExists(target);
            if (Files.isRegularFile(defaultFile)) {
                Files.createDirectories(target.getParent());
                Files.copy(defaultFile, target, StandardCopyOption.REPLACE_EXISTING);
                log.info("Restored default file: " + target.toAbsolutePath());
            } else {
                log.info("Deleted managed file (no default available): " + target.toAbsolutePath());
            }
        }
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
