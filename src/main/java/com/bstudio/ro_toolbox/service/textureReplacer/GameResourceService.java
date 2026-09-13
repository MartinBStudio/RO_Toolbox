package com.bstudio.ro_toolbox.service.textureReplacer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface GameResourceService {
  Path getResourcesDir();

  Path getGameDataDir();

  void clearInstalledPackage() throws IOException;

  void downloadAndExtract() throws IOException;

  void installPackage(String profileId, List<String> disabledManagedSubfolders) throws IOException;

  void clearDownloadedPackages() throws IOException;

  ResourcePackage getInstalledPackageInfo();
}
