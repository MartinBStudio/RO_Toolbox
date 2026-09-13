package com.bstudio.ro_toolbox.service.textureReplacer.buffIcons;

import com.bstudio.ro_toolbox.config.GeneralConstants;
import com.bstudio.ro_toolbox.service.common.ICommonMethods;
import com.bstudio.ro_toolbox.service.common.ResourcesUpdater;
import com.bstudio.ro_toolbox.service.textureReplacer.AvailablePackage;
import com.bstudio.ro_toolbox.service.textureReplacer.GameResourceService;
import com.bstudio.ro_toolbox.service.app.AppConfigService;
import com.bstudio.ro_toolbox.util.AppDataPaths;
import com.bstudio.ro_toolbox.util.RepositoryZipDownloader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@DependsOn("appConfigService")
public class IconsManagerService implements GameResourceService, ICommonMethods {
    private static final String ICONS_REMOTE_REPOSITORY = "https://github.com/MartinBStudio/RO_BuffIcons_resources";
    private static final String PACKAGE_MANIFEST_FILE = "manifestBuffIcons.json";

    private static final Path RESOURCES_DIR = AppDataPaths.resolveRoToolboxAppDataRoot().resolve("resources").resolve("buffIcons");
    private static final Path GAME_SUFFIX = Paths.get("");

    private final AppConfigService appConfigService;
    private final ResourcesUpdater resourcesUpdater;

    public Path getResourcesDir() {
        return RESOURCES_DIR;
    }

    public Path getGameDataDir() {
        Path selectedGameBase = appConfigService.getSelectedGameBase();
        return (selectedGameBase == null) ? null : selectedGameBase.resolve(GAME_SUFFIX);
    }

    public void downloadAndExtract() throws IOException {
        RepositoryZipDownloader.downloadAndExtract(
                ICONS_REMOTE_REPOSITORY,
                RESOURCES_DIR,
                "RO_BuffIconsManager/1.0",
                log::info
        );
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
                                log.info("Copied: " + targetPath.toAbsolutePath());
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
                log.info("Deleted file: " + file.toAbsolutePath());
                return java.nio.file.FileVisitResult.CONTINUE;
            }

            @Override
            public java.nio.file.FileVisitResult postVisitDirectory(Path visitedDir, IOException exc) throws IOException {
                if (!visitedDir.equals(dir)) {
                    Files.deleteIfExists(visitedDir);
                    log.info("Deleted dir: " + visitedDir.toAbsolutePath());
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
                    log.info("Deleted profile dir: " + entry.toAbsolutePath());
                } else {
                    Files.deleteIfExists(entry);
                    log.info("Deleted resource file: " + entry.toAbsolutePath());
                }
            }
        }
    }

    public void clearSelectedItemFolder() throws IOException {
        Path gameBase = appConfigService.getSelectedGameBase();
        if (gameBase == null || !Files.exists(gameBase) || !Files.isDirectory(gameBase)) {
            return;
        }

        deleteInstalledFiles(gameBase);
    }

    public void installProfile(String profileId) throws IOException {
        Path destination = getGameDataDir();
        if (destination == null) {
            throw new IllegalStateException("No game installation folder is selected.");
        }
        AvailablePackage selected = findAvailableProfile(profileId);

        deleteInstalledFiles(destination);
        copyDirectoryContents(selected.getSource(), destination);
    }

    private void deleteInstalledFiles(Path baseDir) throws IOException {
        if (baseDir == null) {
            return;
        }
        Files.deleteIfExists(baseDir.resolve(PACKAGE_MANIFEST_FILE));
        Files.deleteIfExists(baseDir.resolve(Paths.get("3ddata", "control", "Res", "stateicon.dds")));
    }

    private AvailablePackage findAvailableProfile(String profileId) {
        String normalizedProfileId = profileId == null ? "" : profileId.trim();
        if (normalizedProfileId.isEmpty()) {
            throw new IllegalArgumentException("profileId is required.");
        }
        return listAvailableProfiles().stream()
                .filter(profile -> profile.getId().equals(normalizedProfileId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Profile not found: " + normalizedProfileId));
    }


    public List<String> listDownloadedProfiles() {
        return listDownloadedProfiles(RESOURCES_DIR, PACKAGE_MANIFEST_FILE);
    }

    public List<AvailablePackage> listAvailableProfiles() {
        return listAvailableProfiles(RESOURCES_DIR, appConfigService.getSelectedGameBase(), PACKAGE_MANIFEST_FILE);
    }

    public AvailablePackage getInstalledProfileInfo() {
        Path itemFolder = getGameDataDir();
        if (itemFolder == null || !Files.exists(itemFolder)) {
            return null;
        }

        Path manifest = itemFolder.resolve(PACKAGE_MANIFEST_FILE);
        if (manifest == null || !Files.isRegularFile(manifest)) {
            return null;
        }
        AvailablePackage availablePackage = AvailablePackage.builder().id(readManifestName(manifest)).name(readManifestName(manifest)).author(readManifestAuthor(manifest)).description(readManifestDescription(manifest)).url(readManifestUrl(manifest)).createdAt(readManifestCreatedAt(manifest)).version(readManifestVersion(manifest)).previewImages(loadPreviewImages(manifest)).source(manifest).managedSubfolders(readManifestManagedSubfolders(manifest)).build();

        return availablePackage;
    }



    public ResourcesUpdater.ResourcesUpdateCheckResult checkResourcesUpdate() {
        return resourcesUpdater.checkResourcesUpdate(ICONS_REMOTE_REPOSITORY, RESOURCES_DIR);
    }

}
