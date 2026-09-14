package com.bstudio.ro_toolbox.service.textureReplacer;

import com.bstudio.ro_toolbox.service.common.ResourcesUpdater;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface GameResourceService {
    Path getResourcesDir();
    Path getGameDataDir();
    ResourcePackage getStatus();
    List<ResourcePackage> listPackages() throws IOException;
    void clearDownloaded() throws IOException;
    void uninstallPackage() throws IOException;
    void managePackage(String profileId, List<String> disabledManagedSubfolders) throws IOException;
    void installPackage(String profileId, List<String> disabledManagedSubfolders) throws IOException;
    void runUpdate() throws IOException;
    ResourcesUpdater.ResourcesUpdateCheckResult checkForUpdate();
}
