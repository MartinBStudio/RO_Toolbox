package com.bstudio.ro_toolbox.controller.model;

import com.bstudio.ro_toolbox.service.textureReplacer.ResourcePackage;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class PackageServiceStatusResponse {
  private String selectedGameBase;
  private String selectedGameItemFolder;
  private ResourcePackage installedProfile;
  private List<String> downloadedProfiles;
  private List<ResourcePackage> availableProfiles;
}
