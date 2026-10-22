package com.bstudio.ro_toolbox.service.resourceReplacer.component;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

class PackageMetadataTests {
  @TempDir Path temp;
  private final PackageManifestReader reader = new PackageManifestReader();

  @Test
  void exposesOptionalMetadataInAvailableAndInstalledPackages() throws Exception {
    Path directory = Files.createDirectories(temp.resolve("Adventurer"));
    Files.writeString(
        directory.resolve("package.json"),
        """
        {"name":"Adventurer's Plains","version":"1.3.5","author":"ivqonsanada",
         "made":" ai ","mode":" dark ","tags":["kind:theme","made:ai","color:green","color:yellow","cursors"],
         "managedSubfolders":["FILE_LIST.TXT"]}
        """);
    var installed = reader.readManifest("package.json", directory);
    assertEquals(
        List.of("kind:theme", "made:ai", "color:green", "color:yellow", "cursors"),
        installed.getTags());
    var available = new PackageHandler(reader).listAvailablePackages(temp, null, "package.json");
    assertEquals(1, available.size());
    assertEquals(installed.getTags(), available.getFirst().getTags());
    JsonMapper mapper = JsonMapper.builder().build();
    Map<?, ?> json = mapper.readValue(mapper.writeValueAsString(installed), Map.class);
    assertFalse(json.containsKey("made"));
    assertFalse(json.containsKey("mode"));
    assertEquals(installed.getTags(), json.get("tags"));
  }

  @Test
  void acceptsOlderManifestsAndAbsentOrNullMetadata() throws Exception {
    for (String metadata : List.of("", ",\"made\":null,\"mode\":null,\"tags\":null")) {
      Files.writeString(
          temp.resolve("package.json"), "{\"name\":\"Legacy\",\"version\":\"1\"" + metadata + "}");
      var resource = reader.readManifest("package.json", temp);
      assertEquals("Legacy", resource.getName());
      assertEquals(List.of(), resource.getTags());
    }
  }

  @Test
  void ignoresWrongTypesAndHandlesEscapedTagStrings() throws Exception {
    Files.writeString(
        temp.resolve("package.json"),
        """
        {"name":"Test","made":123,"mode":"  ",
         "tags":[" color:green ",null,123,{"unexpected":"tag"},"", "color:green", "style:art\\u0020deco", "label:a\\\"b"]}
        """);
    var resource = reader.readManifest("package.json", temp);
    assertEquals(List.of("color:green", "style:art deco", "label:a\"b"), resource.getTags());
    Files.writeString(temp.resolve("package.json"), "{\"name\":\"Test\",\"tags\":\"color:green\"}");
    assertEquals(List.of(), reader.readManifest("package.json", temp).getTags());
  }

  @Test
  void supportsUtf8BomInOptionalMetadata() throws Exception {
    Files.writeString(
        temp.resolve("package.json"),
        "\uFEFF{\"name\":\"Test\",\"made\":\"ai\",\"tags\":[\"color:green\"]}");
    var resource = reader.readManifest("package.json", temp);
    assertEquals(List.of("color:green"), resource.getTags());
  }

  @Test
  void malformedOptionalMetadataDoesNotBreakLegacyManifestReading() throws Exception {
    Files.writeString(temp.resolve("package.json"), "{\"name\":\"Legacy\",\"tags\":[");
    var resource = reader.readManifest("package.json", temp);
    assertEquals("Legacy", resource.getName());
    assertEquals(List.of(), resource.getTags());
  }
}
