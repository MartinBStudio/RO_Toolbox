package com.bstudio.ro_toolbox.service.resourceReplacer.component;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ResourcesUpdaterTests {
  @TempDir Path temp;

  @Test
  void exposesRemoteDescriptionIncludingEscapedText() throws Exception {
    var result =
        check(
            "{\"version\":\"1.8\",\"description\":\"10 new packages added\\nIncludes \\\"new\\\" icons\"}");
    assertTrue(result.success());
    assertTrue(result.updateAvailable());
    assertEquals("1.8", result.remoteVersion());
    assertEquals("10 new packages added\nIncludes \"new\" icons", result.remoteDescription());
  }

  @Test
  void acceptsManifestWithoutDescription() throws Exception {
    var result = check("{\"version\":\"1.8\"}");
    assertTrue(result.success());
    assertEquals("", result.remoteDescription());
  }

  private ResourcesUpdater.ResourcesUpdateCheckResult check(String manifest) throws Exception {
    Files.writeString(temp.resolve("manifest.json"), "{\"version\":\"1.7\"}");
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/main/manifest.json",
        exchange -> {
          byte[] body = manifest.getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, body.length);
          try (var output = exchange.getResponseBody()) {
            output.write(body);
          }
        });
    server.start();
    try {
      return new ResourcesUpdater(new PackageManifestReader())
          .checkResourcesUpdate("http://127.0.0.1:" + server.getAddress().getPort(), temp);
    } finally {
      server.stop(0);
    }
  }
}
