package com.bstudio.ro_toolbox.controller.model;

import com.bstudio.ro_toolbox.service.textureReplacer.AvailablePackage;
import lombok.Builder;
import lombok.Data;

import java.util.List;
@Builder
@Data
public class PackageServiceStatusResponse {
    private String selectedGameBase;
    private String selectedGameItemFolder;
    private AvailablePackage installedProfile;
    private List<String> downloadedProfiles;
    private List<AvailablePackage> availableProfiles;
}
