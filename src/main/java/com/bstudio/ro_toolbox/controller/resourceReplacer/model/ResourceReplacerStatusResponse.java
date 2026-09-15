package com.bstudio.ro_toolbox.controller.resourceReplacer.model;

import com.bstudio.ro_toolbox.service.resourceReplacer.model.Resource;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ResourceReplacerStatusResponse {
  private String selectedGameBase;
  private String selectedGameItemFolder;
  private Resource installedProfile;
  private List<String> downloadedProfiles;
  private List<Resource> availableProfiles;
}
