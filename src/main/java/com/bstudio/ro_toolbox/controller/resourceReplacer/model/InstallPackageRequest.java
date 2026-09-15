package com.bstudio.ro_toolbox.controller.resourceReplacer.model;

import java.util.List;
import lombok.Data;

@Data
public class InstallPackageRequest {
  private String profileId;
  List<String> disabledManagedSubfolders;
}
