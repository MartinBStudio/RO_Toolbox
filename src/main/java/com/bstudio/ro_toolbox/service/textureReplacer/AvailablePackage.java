package com.bstudio.ro_toolbox.service.textureReplacer;

import lombok.Builder;
import lombok.Data;

import java.nio.file.Path;
import java.util.List;

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
}
