package com.bstudio.ro_toolbox.service.resourceReplacer.service.loot;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class ZmsPreviewParser {
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

  private ZmsPreviewParser() {}

  static LootModelPreview parseLargestFile(Path folder, Path itemRoot, String folderName)
      throws IOException {
    try (var files = Files.walk(folder)) {
      List<Path> zmsFiles =
          files
              .filter(Files::isRegularFile)
              .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".zms"))
              .sorted(Comparator.comparing(path -> path.toString().toLowerCase()))
              .toList();
      LootModelPreview best = null;
      for (Path zmsFile : zmsFiles) {
        LootModelPreview preview = parseFile(zmsFile, itemRoot, folderName);
        if (preview.error() != null) {
          if (best == null) {
            best = preview;
          }
          continue;
        }
        if (best == null
            || best.error() != null
            || preview.bounds().largestAxis() > best.bounds().largestAxis()) {
          best = preview;
        }
      }
      if (best == null) {
        return error(folderName, null, null, "No ZMS model files were found.");
      }
      return best;
    }
  }

  private static LootModelPreview parseFile(Path path, Path itemRoot, String folderName) {
    String relativePath = itemRoot.relativize(path).toString().replace('\\', '/');
    try {
      byte[] bytes = Files.readAllBytes(path);
      ParsedMesh mesh = parse(bytes);
      return new LootModelPreview(
          folderName,
          path.getFileName().toString(),
          relativePath,
          mesh.version(),
          mesh.flags(),
          mesh.vertexCount(),
          mesh.triangleCount(),
          mesh.positions(),
          mesh.normals(),
          mesh.uvs(),
          mesh.colors(),
          mesh.indices(),
          mesh.bounds(),
          null);
    } catch (Exception ex) {
      return error(folderName, path.getFileName().toString(), relativePath, ex.getMessage());
    }
  }

  private static LootModelPreview error(
      String folderName, String fileName, String relativePath, String message) {
    return new LootModelPreview(
        folderName,
        fileName,
        relativePath,
        null,
        null,
        0,
        0,
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        null,
        message);
  }

  private static ParsedMesh parse(byte[] bytes) {
    if (bytes.length < 39) {
      throw new IllegalArgumentException("File is too small to be a ZMS mesh.");
    }

    ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
    String magic = new String(bytes, 0, 7, StandardCharsets.US_ASCII);
    if (!magic.matches("ZMS000[5-8]")) {
      throw new IllegalArgumentException("Unsupported ZMS magic: " + magic);
    }

    int version = Character.digit(magic.charAt(6), 10);
    int offset = bytes.length > 7 && bytes[7] == 0 ? 8 : 7;
    int flags = buffer.getInt(offset);
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
      offset += 4 * vertexCount;
    } else {
      vertexCount = Short.toUnsignedInt(buffer.getShort(offset));
      offset += 2;
    }

    if ((flags & POSITION) == 0) {
      throw new IllegalArgumentException("Mesh does not include position data.");
    }

    List<Float> positions = List.of();
    List<Float> normals = List.of();
    List<Float> colors = List.of();
    List<Float> uvs = List.of();
    BoundsBuilder bounds = new BoundsBuilder();

    if ((flags & POSITION) != 0) {
      positions = readFloatArray(buffer, offset, vertexCount * 3);
      for (int i = 0; i < positions.size(); i += 3) {
        bounds.include(positions.get(i), positions.get(i + 1), positions.get(i + 2));
      }
      offset += 12 * vertexCount;
    }
    if ((flags & NORMAL) != 0) {
      normals = readFloatArray(buffer, offset, vertexCount * 3);
      offset += 12 * vertexCount;
    }
    if ((flags & COLOR) != 0) {
      colors = readFloatArray(buffer, offset, vertexCount * 4);
      offset += 16 * vertexCount;
    }
    if ((flags & BONE_INDEX) != 0 && (flags & BONE_WEIGHT) != 0) {
      offset += (legacy ? 32 : 24) * vertexCount;
    }
    if ((flags & TANGENT) != 0) {
      offset += 12 * vertexCount;
    }
    if ((flags & UV1) != 0) {
      uvs = readFloatArray(buffer, offset, vertexCount * 2);
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

    int triangleCount;
    if (legacy) {
      triangleCount = buffer.getInt(offset);
      offset += 4;
    } else {
      triangleCount = Short.toUnsignedInt(buffer.getShort(offset));
      offset += 2;
    }

    int indexCount = triangleCount * 3;
    List<Integer> indices = new ArrayList<>(indexCount);
    int bytesNeededAsShorts = indexCount * 2;
    int bytesNeededAsInts = indexCount * 4;
    if (bytes.length - offset >= bytesNeededAsShorts) {
      for (int i = 0; i < indexCount; i++) {
        indices.add(Short.toUnsignedInt(buffer.getShort(offset + (i * 2))));
      }
    } else if (bytes.length - offset >= bytesNeededAsInts) {
      for (int i = 0; i < indexCount; i++) {
        indices.add(buffer.getInt(offset + (i * 4)));
      }
    } else {
      throw new IllegalArgumentException("Mesh index data is incomplete.");
    }

    return new ParsedMesh(
        version,
        flags,
        vertexCount,
        triangleCount,
        positions,
        normals,
        uvs,
        colors,
        indices,
        bounds.toBounds());
  }

  private static List<Float> readFloatArray(ByteBuffer buffer, int offset, int count) {
    List<Float> values = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      values.add(buffer.getFloat(offset + (i * 4)));
    }
    return values;
  }

  private static final class BoundsBuilder {
    private float minX = Float.POSITIVE_INFINITY;
    private float minY = Float.POSITIVE_INFINITY;
    private float minZ = Float.POSITIVE_INFINITY;
    private float maxX = Float.NEGATIVE_INFINITY;
    private float maxY = Float.NEGATIVE_INFINITY;
    private float maxZ = Float.NEGATIVE_INFINITY;

    void include(float x, float y, float z) {
      minX = Math.min(minX, x);
      minY = Math.min(minY, y);
      minZ = Math.min(minZ, z);
      maxX = Math.max(maxX, x);
      maxY = Math.max(maxY, y);
      maxZ = Math.max(maxZ, z);
    }

    LootModelScaleReport.Bounds toBounds() {
      float sizeX = maxX - minX;
      float sizeY = maxY - minY;
      float sizeZ = maxZ - minZ;
      return new LootModelScaleReport.Bounds(
          new LootModelScaleReport.Vector(minX, minY, minZ),
          new LootModelScaleReport.Vector(maxX, maxY, maxZ),
          new LootModelScaleReport.Vector(sizeX, sizeY, sizeZ),
          Math.max(Math.max(sizeX, sizeY), sizeZ));
    }
  }

  private record ParsedMesh(
      int version,
      int flags,
      int vertexCount,
      int triangleCount,
      List<Float> positions,
      List<Float> normals,
      List<Float> uvs,
      List<Float> colors,
      List<Integer> indices,
      LootModelScaleReport.Bounds bounds) {}
}
