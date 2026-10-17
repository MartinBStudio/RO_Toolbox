package com.bstudio.ro_toolbox.service.themeEditor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.bstudio.ro_toolbox.config.ApiExceptionHandler;
import com.bstudio.ro_toolbox.controller.themeEditor.GlobalThemeController;
import com.bstudio.ro_toolbox.service.app.AppConfigService;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class GlobalThemeServiceTests {
  @TempDir Path temp;
  Path root;
  ThemeEditorService files;
  GlobalThemeService service;

  @BeforeEach
  void setup() throws Exception {
    root = Files.createDirectories(temp.resolve("ui"));
    files = new ThemeEditorService(mock(AppConfigService.class), root, temp.resolve("originals"));
    service = new GlobalThemeService(files, temp.resolve("global"));
  }

  @Test
  void remembersDefaultsBeforeEditingAndRestoresAfterRestart() throws Exception {
    String original =
        "\ufeff.panel {background: rgba(10, 20, 30, 170); color: white;}\r\n.clear {background: transparent;}\r\n";
    Path path = root.resolve("panel.css");
    Files.writeString(path, original);
    Files.write(root.resolve("icon.png"), new byte[] {0, 1, 2});
    var status = service.status();
    assertEquals(1, status.fileCount());
    assertTrue(Files.exists(files.backup(path.toRealPath())));
    assertEquals(original, Files.readString(path));
    status = service.apply(new GlobalThemeService.Request(status.revision(), "#112233", 50));
    assertTrue(Files.readString(path).contains("rgba(17, 34, 51, 85)"));
    assertTrue(Files.readString(path).contains("rgba(17, 34, 51, 0)"));
    service = new GlobalThemeService(files, temp.resolve("global"));
    assertEquals("#112233", service.status().tint());
    assertEquals(50, service.status().transparency());
    status = service.apply(new GlobalThemeService.Request(service.status().revision(), null, 20));
    assertTrue(
        Files.readString(path).contains("rgba(10, 20, 30, 136)"),
        "Adjustments must always use original alpha");
    service.restore(new GlobalThemeService.RestoreRequest(status.revision()));
    assertEquals(original, Files.readString(path));
    assertArrayEquals(new byte[] {0, 1, 2}, Files.readAllBytes(root.resolve("icon.png")));
    assertNull(service.status().tint());
    assertFalse(service.status().applied());
  }

  @Test
  void appliesToSharedTilesAndEmbeddedAndInlineBackgroundsWithoutFadingTextOrImages()
      throws Exception {
    Files.writeString(
        root.resolve("window.css"),
        ".window {decorator: tiled-box(a,b,c); color: white;} .panel {background: rgba(1,2,3,80); decorator: tiled-box(a,b,c);} .icon {decorator: image(close);} /* decorator: tiled-box(a,b,c); */");
    Files.writeString(
        root.resolve("window.html"),
        "<rml><head><style>.row {background: #1234;}</style></head><body><div style=\"background: rgb(1,2,3);\">Text</div></body></rml>");
    var status =
        service.apply(new GlobalThemeService.Request(service.status().revision(), "#445566", 50));
    String css = Files.readString(root.resolve("window.css"));
    assertTrue(css.contains("decorator: none; background-color: rgba(68, 85, 102, 128)"));
    assertTrue(css.contains("background: rgba(68, 85, 102, 40); decorator: none;"));
    assertTrue(css.contains("color: white"));
    assertTrue(css.contains("decorator: image(close)"));
    assertTrue(css.contains("/* decorator: tiled-box(a,b,c); */"));
    String html = Files.readString(root.resolve("window.html"));
    assertTrue(html.contains("rgba(68, 85, 102, 34)"));
    assertTrue(html.contains("rgba(68, 85, 102, 128)"));
    service.apply(new GlobalThemeService.Request(status.revision(), "#445566", 100));
    assertTrue(Files.readString(root.resolve("window.css")).contains("rgba(68, 85, 102, 0)"));
  }

  @Test
  void reusesOriginalsFromEarlierPerFileEditor() throws Exception {
    Path path = root.resolve("test.css");
    String original = "body {background: rgb(1,2,3);}";
    Files.writeString(path, original);
    var file = files.read("test.css");
    files.save(
        new ThemeEditorService.SaveRequest(
            file.path(),
            file.revision(),
            List.of(
                new ThemeEditorService.Change(
                    file.backgrounds().getFirst().id(), "#ffffff", 255))));
    var status = service.status();
    service.restore(new GlobalThemeService.RestoreRequest(status.revision()));
    assertEquals(original, Files.readString(path));
  }

  @Test
  void validatesAndRejectsExternalChangesBeforeWritingAnyFile() throws Exception {
    Files.writeString(root.resolve("a.css"), "body {background: black;}");
    Files.writeString(root.resolve("b.css"), "body {background: black;}");
    var status = service.status();
    assertThrows(
        IllegalArgumentException.class,
        () ->
            service.apply(
                new GlobalThemeService.Request(status.revision(), "red; color: white", 10)));
    assertThrows(
        IllegalArgumentException.class,
        () -> service.apply(new GlobalThemeService.Request(status.revision(), null, 101)));
    Files.writeString(root.resolve("b.css"), "body {background: white;}");
    assertThrows(
        IllegalStateException.class,
        () -> service.apply(new GlobalThemeService.Request(status.revision(), "#112233", 50)));
    assertEquals("body {background: black;}", Files.readString(root.resolve("a.css")));
    assertEquals("body {background: white;}", Files.readString(root.resolve("b.css")));
  }

  @Test
  void failedBatchRollsBackFilesAndKeepsSavedSettings() throws Exception {
    String original = "body {background: black;}";
    Files.writeString(root.resolve("a.css"), original);
    Files.writeString(root.resolve("b.css"), original);
    ThemeEditorService failing =
        new ThemeEditorService(mock(AppConfigService.class), root, temp.resolve("originals")) {
          boolean failed;

          @Override
          void write(Path path, String text) throws IOException {
            if (path.getFileName().toString().equals("b.css") && !failed) {
              failed = true;
              throw new IOException("Test write failure");
            }
            super.write(path, text);
          }
        };
    service = new GlobalThemeService(failing, temp.resolve("global"));
    String revision = service.status().revision();
    assertThrows(
        IOException.class,
        () -> service.apply(new GlobalThemeService.Request(revision, "#123456", 50)));
    assertEquals(original, Files.readString(root.resolve("a.css")));
    assertEquals(original, Files.readString(root.resolve("b.css")));
    assertFalse(service.status().applied());
  }

  @Test
  void globalApiAppliesAndRestoresAllFiles() throws Exception {
    Files.writeString(root.resolve("test.css"), "body {background: black;}");
    var mvc =
        MockMvcBuilders.standaloneSetup(new GlobalThemeController(service))
            .setControllerAdvice(new ApiExceptionHandler())
            .build();
    mvc.perform(get("/api/theme-editor/global")).andExpect(status().isOk());
    String json =
        "{\"revision\":\""
            + service.status().revision()
            + "\",\"tint\":\"#112233\",\"transparency\":50}";
    mvc.perform(
            post("/api/theme-editor/global").contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().isOk());
    assertTrue(Files.readString(root.resolve("test.css")).contains("rgba(17, 34, 51, 128)"));
    String restore = "{\"revision\":\"" + service.status().revision() + "\"}";
    mvc.perform(
            post("/api/theme-editor/global/restore")
                .contentType(MediaType.APPLICATION_JSON)
                .content(restore))
        .andExpect(status().isOk());
    assertEquals("body {background: black;}", Files.readString(root.resolve("test.css")));
  }

  @Test
  void tintPreservesScreenBackdropsWhileTransparencyStillApplies() throws Exception {
    Files.createDirectories(root.resolve("common"));
    Path overlay = root.resolve("common/modal_overlay.html");
    String original =
        "<rml><head><style>body {width: 100%; height: 100%; background: rgba(10, 10, 10, 170);}</style></head><body id=\"common-modal-overlay\"></body></rml>";
    Files.writeString(overlay, original);
    Files.writeString(
        root.resolve("surfaces.css"),
        ".backdrop {background: rgba(0, 0, 0, 180);} .screen {width: 100vw; height: 100vh; background: rgba(20, 20, 20, 200);} .panel {background: rgba(33, 33, 33, 80);}");
    var status =
        service.apply(new GlobalThemeService.Request(service.status().revision(), "#ff0000", 0));
    assertTrue(Files.readString(overlay).contains("rgba(10, 10, 10, 170)"));
    String css = Files.readString(root.resolve("surfaces.css"));
    assertTrue(css.contains("rgba(0, 0, 0, 180)"));
    assertTrue(css.contains("rgba(20, 20, 20, 200)"));
    assertTrue(css.contains("rgba(255, 0, 0, 80)"));
    status = service.apply(new GlobalThemeService.Request(status.revision(), "#ff0000", 50));
    assertTrue(Files.readString(overlay).contains("rgba(10, 10, 10, 85)"));
    assertTrue(Files.readString(root.resolve("surfaces.css")).contains("rgba(255, 0, 0, 40)"));
    // Simulate a backdrop written by the previous version, then reapply identical settings.
    Files.writeString(
        overlay, Files.readString(overlay).replace("rgba(10, 10, 10, 85)", "rgba(255, 0, 0, 85)"));
    status =
        service.apply(new GlobalThemeService.Request(service.status().revision(), "#ff0000", 50));
    assertTrue(Files.readString(overlay).contains("rgba(10, 10, 10, 85)"));
    service.restore(new GlobalThemeService.RestoreRequest(status.revision()));
    assertEquals(original, Files.readString(overlay));
  }

  @Test
  void runtimePanelColorsFollowGlobalSettingsAndRestoreExactly() throws Exception {
    Path path = root.resolve("custom_ui.lua");
    String original =
        "local function update_backgrounds()\r\n    local alpha_byte = math.floor(alpha * 255 / 100 + 0.5)\r\n    local background_color = 'rgba(' .. rgb[1] .. ', ' .. rgb[2] .. ', ' .. rgb[3] .. ', ' .. alpha_byte .. ')'\r\nend\r\n";
    Files.writeString(path, original);
    var status =
        service.apply(new GlobalThemeService.Request(service.status().revision(), "#123456", 60));
    assertTrue(Files.readString(path).contains("math.floor(alpha * 255 / 100 * 40 / 100 + 0.5)"));
    assertTrue(Files.readString(path).contains("'rgba(18, 52, 86, ' .. alpha_byte .. ')'"));
    service.restore(new GlobalThemeService.RestoreRequest(status.revision()));
    assertEquals(original, Files.readString(path));
  }

  @Test
  void installedThemeCopiesApplyTwiceAndRestoreExactly() throws Exception {
    String fixture = System.getenv("RO_TOOLBOX_THEME_FIXTURE");
    Assumptions.assumeTrue(fixture != null && Files.isDirectory(Path.of(fixture)));
    Map<Path, byte[]> originals = new LinkedHashMap<>();
    Path source = Path.of(fixture);
    try (var paths = Files.walk(source)) {
      for (Path path : paths.filter(Files::isRegularFile).toList()) {
        String name = source.relativize(path).toString();
        if (!name.matches("(?i).*\\.(html|rml|css|rcss)$")
            && !path.getFileName().toString().equals("custom_ui.lua")) continue;
        Path copy = root.resolve(name);
        Files.createDirectories(copy.getParent());
        byte[] bytes = Files.readAllBytes(path);
        Files.write(copy, bytes);
        originals.put(copy, bytes);
      }
    }
    var status =
        service.apply(new GlobalThemeService.Request(service.status().revision(), "#123456", 40));
    status = service.apply(new GlobalThemeService.Request(status.revision(), "#abcdef", 70));
    assertEquals(originals.size(), status.fileCount());
    service = new GlobalThemeService(files, temp.resolve("global"));
    service.restore(new GlobalThemeService.RestoreRequest(service.status().revision()));
    for (var entry : originals.entrySet())
      assertArrayEquals(
          entry.getValue(), Files.readAllBytes(entry.getKey()), entry.getKey().toString());
    System.out.println(
        "Global apply, reapply and restore passed for "
            + originals.size()
            + " installed UI file copies.");
  }
}
