package com.bstudio.ro_toolbox.service.textureReplacer;

import java.nio.file.Path;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class AvailablePackage {
  private String id;
  private String name;
  private String author;
  private String description;
  private String url;
  private String createdAt;
  private String version;
  private long normalizedVersion;
  private Path source;
  private List<String> previewImages;
  private List<String> managedSubfolders;
  private List<String> disabledManagedSubfolders;
}
