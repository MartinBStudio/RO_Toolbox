package com.bstudio.ro_toolbox.service.resourceReplacer.service.loot;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

final class ZmsScaleScanner {
  private static final float MAX_REASONABLE_SIZE = 10_000.0f;
  private static final int POSITION = 0x0002;
  private static final int NORMAL = 0x0004;
  private static final int COLOR = 0x0008;
  private static final int BONE_INDEX = 0x0010;
  private static final int BONE_WEIGHT = 0x0020;
  private static final int TANGENT = 0x0040;
  private static final int UV1 = 0x0080;
  private static final int UV2 = 0x0100;
  private static final int UV3 = 0x0200;
  private static final int UV4 = 0x0400;

  private ZmsScaleScanner() {}

  static List<LootModelScaleReport.File> scanFolder(Path folder, Path itemRoot) throws IOException {
    try (var files = Files.walk(folder)) {
      return files
          .filter(Files::isRegularFile)
          .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".zms"))
          .sorted(Comparator.comparing(path -> itemRoot.relativize(path).toString().toLowerCase()))
          .map(path -> scanFile(path, itemRoot))
          .toList();
    }
  }

  static int scaleFolder(Path folder, float factor) throws IOException {
    try (var files = Files.walk(folder)) {
      List<Path> zmsFiles =
          files
              .filter(Files::isRegularFile)
              .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".zms"))
              .sorted(Comparator.comparing(path -> path.toString().toLowerCase()))
              .toList();
      int updated = 0;
      for (Path zmsFile : zmsFiles) {
        scaleFile(zmsFile, factor);
        updated++;
      }
      return updated;
    }
  }

  private static LootModelScaleReport.File scanFile(Path path, Path itemRoot) {
    String relativePath = itemRoot.relativize(path).toString().replace('\\', '/');
    try {
      byte[] bytes = Files.readAllBytes(path);
      ParsedZms parsed = parse(bytes);
      return new LootModelScaleReport.File(
          path.getFileName().toString(),
          relativePath,
          parsed.version(),
          parsed.flags(),
          parsed.vertexCount(),
          parsed.triangleCount(),
          parsed.vertexBounds(),
          parsed.headerBounds(),
          null);
    } catch (Exception ex) {
      return new LootModelScaleReport.File(
          relativePath, relativePath, null, null, 0, 0, null, null, ex.getMessage());
    }
  }

  private static ParsedZms parse(byte[] bytes) {
    if (bytes.length < 39) {
      throw new IllegalArgumentException("File is too small to be a ZMS mesh.");
    }

    ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
    String magic = new String(bytes, 0, 7, StandardCharsets.US_ASCII);
    if (!magic.matches("ZMS000[5-8]")) {
      throw new IllegalArgumentException("Unsupported ZMS magic: " + magic);
    }

    int version = Character.digit(magic.charAt(6), 10);
    int offset = 7;
    if (bytes.length > 7 && bytes[7] == 0) {
      offset = 8;
    }

    int flags = buffer.getInt(offset);
    float headerMinX = buffer.getFloat(offset + 4);
    float headerMinY = buffer.getFloat(offset + 8);
    float headerMinZ = buffer.getFloat(offset + 12);
    float headerMaxX = buffer.getFloat(offset + 16);
    float headerMaxY = buffer.getFloat(offset + 20);
    float headerMaxZ = buffer.getFloat(offset + 24);
    offset += 28;

    boolean legacy = version == 5 || version == 6;
    int boneCount;
    if (legacy) {
      boneCount = buffer.getInt(offset);
      offset += 4 + (boneCount * 8);
    } else {
      boneCount = Short.toUnsignedInt(buffer.getShort(offset));
      offset += 2 + (boneCount * 2);
    }

    int vertexCount;
    if (legacy) {
      vertexCount = buffer.getInt(offset);
      offset += 4;
    } else {
      vertexCount = Short.toUnsignedInt(buffer.getShort(offset));
      offset += 2;
    }

    int positionStart = legacy ? offset + (4 * vertexCount) : offset;
    if ((flags & POSITION) == 0) {
      throw new IllegalArgumentException("Mesh does not include position data.");
    }
    float minX = Float.POSITIVE_INFINITY;
    float minY = Float.POSITIVE_INFINITY;
    float minZ = Float.POSITIVE_INFINITY;
    float maxX = Float.NEGATIVE_INFINITY;
    float maxY = Float.NEGATIVE_INFINITY;
    float maxZ = Float.NEGATIVE_INFINITY;

    for (int i = 0; i < vertexCount; i++) {
      int positionOffset = positionStart + (i * 12);

      float x = buffer.getFloat(positionOffset);
      float y = buffer.getFloat(positionOffset + 4);
      float z = buffer.getFloat(positionOffset + 8);
      minX = Math.min(minX, x);
      minY = Math.min(minY, y);
      minZ = Math.min(minZ, z);
      maxX = Math.max(maxX, x);
      maxY = Math.max(maxY, y);
      maxZ = Math.max(maxZ, z);
    }

    offset = skipVertexAttributeArrays(offset, vertexCount, flags, legacy);
    int triangleCount;
    if (legacy) {
      triangleCount = buffer.getInt(offset);
    } else {
      triangleCount = Short.toUnsignedInt(buffer.getShort(offset));
    }

    LootModelScaleReport.Bounds vertexBounds = validatedBounds(minX, minY, minZ, maxX, maxY, maxZ);
    LootModelScaleReport.Bounds headerBounds =
        bounds(headerMinX, headerMinY, headerMinZ, headerMaxX, headerMaxY, headerMaxZ);
    return new ParsedZms(version, flags, vertexCount, triangleCount, vertexBounds, headerBounds);
  }

  private static void scaleFile(Path path, float factor) throws IOException {
    byte[] bytes = Files.readAllBytes(path);
    parse(bytes);
    ParsedZmsLayout layout = parseLayout(bytes);
    ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);

    float minX = Float.POSITIVE_INFINITY;
    float minY = Float.POSITIVE_INFINITY;
    float minZ = Float.POSITIVE_INFINITY;
    float maxX = Float.NEGATIVE_INFINITY;
    float maxY = Float.NEGATIVE_INFINITY;
    float maxZ = Float.NEGATIVE_INFINITY;

    for (int i = 0; i < layout.vertexCount(); i++) {
      int positionOffset = layout.positionStart() + (i * 12);

      float x = buffer.getFloat(positionOffset) * factor;
      float y = buffer.getFloat(positionOffset + 4) * factor;
      float z = buffer.getFloat(positionOffset + 8) * factor;
      if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
        throw new IllegalArgumentException("Scaled mesh contains a non-finite position.");
      }

      buffer.putFloat(positionOffset, x);
      buffer.putFloat(positionOffset + 4, y);
      buffer.putFloat(positionOffset + 8, z);
      minX = Math.min(minX, x);
      minY = Math.min(minY, y);
      minZ = Math.min(minZ, z);
      maxX = Math.max(maxX, x);
      maxY = Math.max(maxY, y);
      maxZ = Math.max(maxZ, z);
    }

    validatedBounds(minX, minY, minZ, maxX, maxY, maxZ);
    buffer.putFloat(layout.baseOffset() + 4, minX);
    buffer.putFloat(layout.baseOffset() + 8, minY);
    buffer.putFloat(layout.baseOffset() + 12, minZ);
    buffer.putFloat(layout.baseOffset() + 16, maxX);
    buffer.putFloat(layout.baseOffset() + 20, maxY);
    buffer.putFloat(layout.baseOffset() + 24, maxZ);

    Path backup = path.resolveSibling(path.getFileName() + ".bak");
    if (!Files.exists(backup)) {
      Files.copy(path, backup);
    }
    Files.write(path, bytes);
  }

  private static ParsedZmsLayout parseLayout(byte[] bytes) {
    if (bytes.length < 39) {
      throw new IllegalArgumentException("File is too small to be a ZMS mesh.");
    }

    ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
    String magic = new String(bytes, 0, 7, StandardCharsets.US_ASCII);
    if (!magic.matches("ZMS000[5-8]")) {
      throw new IllegalArgumentException("Unsupported ZMS magic: " + magic);
    }

    int version = Character.digit(magic.charAt(6), 10);
    int baseOffset = bytes.length > 7 && bytes[7] == 0 ? 8 : 7;
    int flags = buffer.getInt(baseOffset);
    if ((flags & POSITION) == 0) {
      throw new IllegalArgumentException("Mesh does not include position data.");
    }

    int offset = baseOffset + 28;
    boolean legacy = version == 5 || version == 6;
    int boneCount;
    if (legacy) {
      boneCount = buffer.getInt(offset);
      offset += 4 + (boneCount * 8);
    } else {
      boneCount = Short.toUnsignedInt(buffer.getShort(offset));
      offset += 2 + (boneCount * 2);
    }

    int vertexCount;
    if (legacy) {
      vertexCount = buffer.getInt(offset);
      offset += 4;
    } else {
      vertexCount = Short.toUnsignedInt(buffer.getShort(offset));
      offset += 2;
    }

    return new ParsedZmsLayout(
        baseOffset, vertexCount, legacy ? offset + (4 * vertexCount) : offset);
  }

  private static LootModelScaleReport.Bounds validatedBounds(
      float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
    LootModelScaleReport.Bounds bounds = bounds(minX, minY, minZ, maxX, maxY, maxZ);
    if (!isReasonableSize(bounds.size().x())
        || !isReasonableSize(bounds.size().y())
        || !isReasonableSize(bounds.size().z())) {
      throw new IllegalArgumentException("Mesh size is outside the expected ZMS range.");
    }
    return bounds;
  }

  private static LootModelScaleReport.Bounds bounds(
      float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
    float sizeX = maxX - minX;
    float sizeY = maxY - minY;
    float sizeZ = maxZ - minZ;
    return new LootModelScaleReport.Bounds(
        new LootModelScaleReport.Vector(minX, minY, minZ),
        new LootModelScaleReport.Vector(maxX, maxY, maxZ),
        new LootModelScaleReport.Vector(sizeX, sizeY, sizeZ),
        Math.max(Math.max(sizeX, sizeY), sizeZ));
  }

  private static boolean isReasonableSize(float value) {
    return Float.isFinite(value) && value >= 0 && value < MAX_REASONABLE_SIZE;
  }

  private static int skipVertexAttributeArrays(
      int offset, int vertexCount, int flags, boolean legacy) {
    if (legacy) {
      offset += 4 * vertexCount;
    }
    if ((flags & POSITION) != 0) {
      offset += 12 * vertexCount;
    }
    if ((flags & NORMAL) != 0) {
      offset += 12 * vertexCount;
    }
    if ((flags & COLOR) != 0) {
      offset += 16 * vertexCount;
    }
    if ((flags & BONE_INDEX) != 0 && (flags & BONE_WEIGHT) != 0) {
      offset += (legacy ? 32 : 24) * vertexCount;
    }
    if ((flags & TANGENT) != 0) {
      offset += 12 * vertexCount;
    }
    if ((flags & UV1) != 0) {
      offset += 8 * vertexCount;
    }
    if ((flags & UV2) != 0) {
      offset += 8 * vertexCount;
    }
    if ((flags & UV3) != 0) {
      offset += 8 * vertexCount;
    }
    if ((flags & UV4) != 0) {
      offset += 8 * vertexCount;
    }
    return offset;
  }

  private record ParsedZms(
      int version,
      int flags,
      int vertexCount,
      int triangleCount,
      LootModelScaleReport.Bounds vertexBounds,
      LootModelScaleReport.Bounds headerBounds) {}

  private record ParsedZmsLayout(int baseOffset, int vertexCount, int positionStart) {}
}
