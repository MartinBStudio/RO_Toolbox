package com.bstudio.ro_toolbox.controller.model;

import lombok.Data;

import java.util.List;

@Data
public class InstallProfileRequest {
    private String profileId;
    List<String> disabledManagedSubfolders;
}
