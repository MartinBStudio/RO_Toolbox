package com.bstudio.ro_toolbox.controller.model;

import java.util.List;
import lombok.Data;

@Data
public class InstallProfileRequest {
  private String profileId;
  List<String> disabledManagedSubfolders;
}
