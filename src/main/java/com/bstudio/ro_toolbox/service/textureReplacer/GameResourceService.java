package com.bstudio.ro_toolbox.service.textureReplacer;

import java.io.IOException;
import java.nio.file.Path;

public interface GameResourceService {
    void clearSelectedItemFolder() throws IOException;
    void downloadAndExtract() throws IOException;
    Path getResourcesDir();
    Path getGameDataDir();
    void clearResources() throws IOException;
    AvailablePackage getInstalledProfileInfo();
}
