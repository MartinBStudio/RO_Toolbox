package com.bstudio.ro_toolbox.service.resourceReplacer.model;

import com.bstudio.ro_toolbox.service.resourceReplacer.component.ResourcesUpdater;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

public interface IResourceReplacer {
  Path getResourcesDir();

  Path getGameDataDir();

  Resource getStatus();

  List<Resource> listPackages() throws IOException;

  void clearDownloaded() throws IOException;

  void uninstallPackage() throws IOException;

  void managePackage(String profileId, List<String> disabledManagedSubfolders) throws IOException;

  void installPackage(String profileId, List<String> disabledManagedSubfolders) throws IOException;

  void runUpdate(Consumer<ResourcesUpdater.DownloadProgress> progressListener) throws IOException;

  ResourcesUpdater.ResourcesUpdateCheckResult checkForUpdate();
}
