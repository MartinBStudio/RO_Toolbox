package com.bstudio.ro_toolbox.service.resourceReplacer.service.loot;

import java.util.List;

public record LootModelScaleReport(String itemFolder, List<Folder> folders) {

  public record Folder(String folder, List<File> files, List<File> originalFiles) {}

  public record File(
      String fileName,
      String relativePath,
      Integer version,
      Integer flags,
      int vertexCount,
      int triangleCount,
      Bounds vertexBounds,
      Bounds headerBounds,
      String error) {}

  public record Bounds(Vector min, Vector max, Vector size, float largestAxis) {}

  public record Vector(float x, float y, float z) {}
}
