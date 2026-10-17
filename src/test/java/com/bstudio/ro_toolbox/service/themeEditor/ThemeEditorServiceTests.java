package com.bstudio.ro_toolbox.service.themeEditor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.bstudio.ro_toolbox.config.ApiExceptionHandler;
import com.bstudio.ro_toolbox.controller.themeEditor.ThemeEditorController;
import com.bstudio.ro_toolbox.service.app.AppConfigService;
import com.bstudio.ro_toolbox.service.themeEditor.ThemeEditorService.*;
import java.nio.file.*;
import java.util.List;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ThemeEditorServiceTests {
  @TempDir Path temporary;
  private Path root;
  private ThemeEditorService service;

  @BeforeEach
  void setup() throws Exception {
    root = Files.createDirectories(temporary.resolve("ui"));
    service =
        new ThemeEditorService(mock(AppConfigService.class), root, temporary.resolve("originals"));
  }

  @Test
  void apiListsReadsSavesRejectsConflictsAndRestores() throws Exception {
    String original = "body { background: rgba(1, 2, 3, 255); }";
    Path path = root.resolve("test.css");
    Files.writeString(path, original);
    var mvc =
        MockMvcBuilders.standaloneSetup(new ThemeEditorController(service))
            .setControllerAdvice(new ApiExceptionHandler())
            .build();
    mvc.perform(get("/api/theme-editor/files")).andExpect(status().isOk());
    mvc.perform(get("/api/theme-editor/file").param("path", "test.css")).andExpect(status().isOk());
    FileState file = service.read("test.css");
    String request =
        "{\"path\":\"test.css\",\"revision\":\""
            + file.revision()
            + "\",\"changes\":[{\"id\":\""
            + file.backgrounds().getFirst().id()
            + "\",\"color\":\"#112233\",\"alpha\":128}]}";
    mvc.perform(
            post("/api/theme-editor/file").contentType(MediaType.APPLICATION_JSON).content(request))
        .andExpect(status().isOk());
    assertTrue(Files.readString(path).contains("rgba(17, 34, 51, 128)"));
    mvc.perform(
            post("/api/theme-editor/file").contentType(MediaType.APPLICATION_JSON).content(request))
        .andExpect(status().isConflict());
    mvc.perform(get("/api/theme-editor/file").param("path", "../outside.css"))
        .andExpect(status().isBadRequest());
    String restore =
        "{\"path\":\"test.css\",\"revision\":\"" + service.read("test.css").revision() + "\"}";
    mvc.perform(
            post("/api/theme-editor/restore")
                .contentType(MediaType.APPLICATION_JSON)
                .content(restore))
        .andExpect(status().isOk());
    assertEquals(original, Files.readString(path));
  }

  @Test
  void roundTripsCopiesOfInstalledGameFilesWhenFixtureIsProvided() throws Exception {
    String fixture = System.getenv("RO_TOOLBOX_THEME_FIXTURE");
    Assumptions.assumeTrue(fixture != null && Files.isDirectory(Path.of(fixture)));
    Path source = Path.of(fixture);
    int count = 0;
    try (var paths = Files.walk(source)) {
      for (Path input : paths.filter(Files::isRegularFile).toList()) {
        String name = source.relativize(input).toString();
        if (!name.matches("(?i).*\\.(html|rml|css|rcss)$") || Files.size(input) > 2 * 1024 * 1024)
          continue;
        Path copy = root.resolve(name);
        Files.createDirectories(copy.getParent());
        byte[] original = Files.readAllBytes(input);
        Files.write(copy, original);
        FileState file = service.read(name);
        if (file.backgrounds().isEmpty()) continue;
        var changes =
            file.backgrounds().stream().map(bg -> new Change(bg.id(), "#123456", 128)).toList();
        FileState saved = service.save(new SaveRequest(file.path(), file.revision(), changes));
        assertTrue(
            saved.backgrounds().stream()
                .allMatch(bg -> bg.color().equals("#123456") && bg.alpha() == 128),
            name);
        service.restore(saved.path(), saved.revision());
        assertArrayEquals(original, Files.readAllBytes(copy), name);
        count++;
      }
    }
    assertTrue(count > 10, "Expected real theme fixtures");
    System.out.println(
        "Round-tripped "
            + count
            + " installed UI file copies; installed originals were read only.");
  }

  @Test
  void listsNestedFilesAndAssetsWithoutWriting() throws Exception {
    Files.createDirectories(root.resolve("themes/rose"));
    Files.writeString(root.resolve("themes/rose/window.html"), "<rml></rml>");
    Files.write(root.resolve("font.ttf"), new byte[] {0, 1, 2});
    assertEquals(
        List.of(new FileEntry("font.ttf", false), new FileEntry("themes/rose/window.html", true)),
        service.list().files());
    assertFalse(Files.exists(temporary.resolve("originals")));
    assertThrows(IllegalArgumentException.class, () -> service.read("font.ttf"));
  }

  @Test
  void updatesOnlySelectedDeclarationsPreservingBomCommentsAndLineEndings() throws Exception {
    String original =
        "\ufeff/* background: red; */\r\n.panel { background: rgba(10, 20, 30, 170); color: white; }\r\n.other { background-color: #abcd !important; }\r\n";
    Path path = root.resolve("window.css");
    Files.writeString(path, original);
    FileState file = service.read("window.css");
    assertEquals(2, file.backgrounds().size());
    assertEquals(170, file.backgrounds().getFirst().alpha());
    assertEquals(221, file.backgrounds().get(1).alpha());
    FileState saved =
        service.save(
            new SaveRequest(
                file.path(),
                file.revision(),
                List.of(
                    new Change(file.backgrounds().getFirst().id(), "#112233", 0),
                    new Change(file.backgrounds().get(1).id(), "#445566", 128))));
    assertEquals(
        original
            .replace("rgba(10, 20, 30, 170)", "rgba(17, 34, 51, 0)")
            .replace("#abcd !important", "rgba(68, 85, 102, 128) !important"),
        Files.readString(path));
    assertTrue(saved.hasBackup());
    saved =
        service.save(
            new SaveRequest(
                saved.path(),
                saved.revision(),
                List.of(new Change(saved.backgrounds().getFirst().id(), "#ffffff", 255))));
    service.restore(saved.path(), saved.revision());
    assertEquals(original, Files.readString(path));
  }

  @Test
  void addsAndReeditsWindowOverrideWithoutDuplicatingIt() throws Exception {
    String original =
        "<rml>\r\n<head><title>Inventory</title></head>\r\n<body template=\"window-fixed-size\">Items</body></rml>";
    Path path = root.resolve("inventory.html");
    Files.writeString(path, original);
    FileState file = service.read("inventory.html");
    FileState saved =
        service.save(
            new SaveRequest(
                file.path(), file.revision(), List.of(new Change("window", "#123456", 80))));
    assertEquals(1, saved.backgrounds().size());
    assertTrue(
        Files.readString(path).contains("decorator: none; background-color: rgba(18, 52, 86, 80)"));
    assertTrue(Files.readString(path).contains("body.window-fixed-size"));
    saved =
        service.save(
            new SaveRequest(
                saved.path(),
                saved.revision(),
                List.of(new Change(saved.backgrounds().getFirst().id(), "#000000", 255))));
    assertEquals(1, saved.backgrounds().size());
    service.restore(saved.path(), saved.revision());
    assertEquals(original, Files.readString(path));
  }

  @Test
  void findsInlineAndEmbeddedStylesButIgnoresCommentsAndUnsupportedTextures() throws Exception {
    Files.writeString(
        root.resolve("test.html"),
        "<rml><head><style>/*background:red;*/ .panel {background: rgb(1,2,3);} .texture {background: url(image.png);}</style></head><body><div style=\"background-color: transparent; color: white\"></div></body></rml>");
    FileState file = service.read("test.html");
    assertEquals(3, file.backgrounds().size());
    assertEquals(0, file.backgrounds().get(1).alpha());
  }

  @Test
  void rejectsStaleRevisionsForSaveAndRestore() throws Exception {
    Path path = root.resolve("test.css");
    Files.writeString(path, "body {background: black;}");
    FileState file = service.read("test.css");
    Files.writeString(path, "body {background: white;}");
    assertThrows(
        IllegalStateException.class,
        () ->
            service.save(
                new SaveRequest(
                    file.path(),
                    file.revision(),
                    List.of(new Change(file.backgrounds().getFirst().id(), "#ffffff", 255)))));
    assertThrows(IllegalStateException.class, () -> service.restore(file.path(), file.revision()));
    assertEquals("body {background: white;}", Files.readString(path));
    assertFalse(Files.exists(temporary.resolve("originals")));
  }

  @Test
  void rejectsTraversalAndInvalidColorsBeforeWriting() throws Exception {
    Files.writeString(temporary.resolve("outside.css"), "body {background: black;}");
    assertThrows(IllegalArgumentException.class, () -> service.read("../outside.css"));
    Files.writeString(root.resolve("test.css"), "body {background: black;}");
    FileState file = service.read("test.css");
    assertThrows(
        IllegalArgumentException.class,
        () ->
            service.save(
                new SaveRequest(
                    file.path(),
                    file.revision(),
                    List.of(
                        new Change(file.backgrounds().getFirst().id(), "red; color: blue", 255)))));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            service.save(
                new SaveRequest(
                    file.path(),
                    file.revision(),
                    List.of(new Change(file.backgrounds().getFirst().id(), "#ffffff", 256)))));
    assertFalse(Files.exists(temporary.resolve("originals")));
  }
}
