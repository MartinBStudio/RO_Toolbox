package com.bstudio.ro_toolbox.service.resourceReplacer.service.loot;

import java.util.List;

public record LootModelPreview(
    String folder,
    String fileName,
    String relativePath,
    Integer version,
    Integer flags,
    int vertexCount,
    int triangleCount,
    List<Float> positions,
    List<Float> normals,
    List<Float> uvs,
    List<Float> colors,
    List<Integer> indices,
    LootModelScaleReport.Bounds bounds,
    String error) {}
