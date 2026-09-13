package com.bstudio.ro_toolbox.service.textureReplacer;

import java.io.IOException;
import java.nio.file.Path;

public interface GameResourceService {
    void clearSelectedItemFolder() throws IOException;
    void downloadAndExtract(String repoUrl, Path destDir) throws IOException;
    Path getResourcesDir();
    Path getSelectedGameItemFolder();
    void clearResources() throws IOException;
    AvailablePackage getInstalledProfileInfo();
}
