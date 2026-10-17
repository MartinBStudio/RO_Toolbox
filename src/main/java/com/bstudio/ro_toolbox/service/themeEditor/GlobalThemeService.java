package com.bstudio.ro_toolbox.service.themeEditor;

import com.bstudio.ro_toolbox.util.AppDataPaths;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GlobalThemeService {
  private final ThemeEditorService files;
  private final Path storage;

  @Autowired
  public GlobalThemeService(ThemeEditorService files) {
    this(files, AppDataPaths.resolveRoToolboxAppDataRoot().resolve("theme-editor-global"));
  }

  GlobalThemeService(ThemeEditorService files, Path storage) {
    this.files = files;
    this.storage = storage;
  }

  private Path manifest(Path root) {
    return storage.resolve(ThemeEditorService.hash(root.toString())).resolve("defaults.properties");
  }

  private Properties load(Path root) throws IOException {
    Properties state = new Properties();
    Path manifest = manifest(root);
    if (Files.exists(manifest))
      try (var input = Files.newInputStream(manifest)) {
        state.load(input);
      }
    boolean changed = !Files.exists(manifest);
    // Capture all defaults before allowing any global edit. Reuse originals from the older editor.
    for (var entry : files.list().files()) {
      boolean runtimeBackgrounds =
          entry.path().equals("custom_ui.lua") || entry.path().endsWith("/custom_ui.lua");
      if ((!entry.editable() && !runtimeBackgrounds) || state.containsKey("file." + entry.path()))
        continue;
      Path path = safePath(root, entry.path());
      if (Files.size(path) > 2 * 1024 * 1024)
        throw new IllegalStateException("UI style file is too large: " + entry.path());
      Path backup = files.backup(path);
      Files.createDirectories(backup.getParent());
      if (!Files.exists(backup)) Files.copy(path, backup);
      // Decode up front so invalid encodings cannot cause a partial apply later.
      Files.readString(backup, StandardCharsets.UTF_8);
      state.setProperty(
          "file." + entry.path(),
          ThemeEditorService.hash(Files.readString(backup, StandardCharsets.UTF_8)));
      changed = true;
    }
    if (changed) store(root, state);
    return state;
  }

  private void store(Path root, Properties state) throws IOException {
    StringWriter output = new StringWriter();
    state.store(output, "Remembered UI defaults and global appearance");
    Path manifest = manifest(root);
    Files.createDirectories(manifest.getParent());
    files.write(manifest, output.toString());
  }

  private Path safePath(Path root, String name) throws IOException {
    Path path = root.resolve(name).normalize();
    if (!path.startsWith(root) || !Files.isRegularFile(path) || !path.toRealPath().startsWith(root))
      throw new IllegalStateException("UI file moved or is missing: " + name);
    return path.toRealPath();
  }

  private List<String> names(Properties state) {
    return state.stringPropertyNames().stream()
        .filter(k -> k.startsWith("file."))
        .map(k -> k.substring(5))
        .sorted()
        .toList();
  }

  private Map<Path, String> current(Path root, Properties state) throws IOException {
    Map<Path, String> result = new LinkedHashMap<>();
    for (String name : names(state))
      result.put(
          safePath(root, name), Files.readString(safePath(root, name), StandardCharsets.UTF_8));
    return result;
  }

  private String revision(Properties state, Map<Path, String> current) {
    StringBuilder value =
        new StringBuilder(state.getProperty("tint", ""))
            .append(':')
            .append(state.getProperty("transparency", "0"));
    current.forEach(
        (path, text) ->
            value.append('\n').append(path).append(':').append(ThemeEditorService.hash(text)));
    return ThemeEditorService.hash(value.toString());
  }

  private Status status(Path root, Properties state, Map<Path, String> current) {
    String tint = state.getProperty("tint", "");
    return new Status(
        root.toString(),
        revision(state, current),
        tint.isEmpty() ? null : tint,
        Integer.parseInt(state.getProperty("transparency", "0")),
        current.size(),
        !tint.isEmpty() || !state.getProperty("transparency", "0").equals("0"));
  }

  public synchronized Status status() throws IOException {
    Path root = files.root();
    Properties state = load(root);
    return status(root, state, current(root, state));
  }

  public synchronized Status apply(Request request) throws IOException {
    if (request == null
        || request.transparency() == null
        || request.transparency() < 0
        || request.transparency() > 100
        || (request.tint() != null && !request.tint().matches("#[0-9a-fA-F]{6}")))
      throw new IllegalArgumentException("Choose a tint color and transparency from 0 to 100.");
    Path root = files.root();
    Properties state = load(root);
    Map<Path, String> before = current(root, state);
    if (!revision(state, before).equals(request.revision()))
      throw new IllegalStateException("UI files changed. Reload the theme editor before applying.");
    Map<Path, String> after = new LinkedHashMap<>();
    for (String name : names(state)) {
      Path path = safePath(root, name);
      Path backup = files.backup(path);
      String defaults = Files.readString(backup, StandardCharsets.UTF_8);
      if (!ThemeEditorService.hash(defaults).equals(state.getProperty("file." + name)))
        throw new IllegalStateException("Remembered defaults changed for " + name);
      after.put(path, files.globalContent(path, defaults, request.tint(), request.transparency()));
    }
    List<Path> written = new ArrayList<>();
    try {
      for (var entry : after.entrySet()) {
        if (entry.getValue().equals(before.get(entry.getKey()))) continue;
        // Catch changes made while the batch was being prepared.
        if (!Files.readString(entry.getKey(), StandardCharsets.UTF_8)
            .equals(before.get(entry.getKey())))
          throw new IllegalStateException(
              "UI files changed. Reload the theme editor before applying.");
        files.write(entry.getKey(), entry.getValue());
        written.add(entry.getKey());
      }
      state.setProperty(
          "tint", request.tint() == null ? "" : request.tint().toLowerCase(Locale.ROOT));
      state.setProperty("transparency", request.transparency().toString());
      store(root, state);
    } catch (IOException | RuntimeException failure) {
      Collections.reverse(written);
      for (Path path : written)
        try {
          files.write(path, before.get(path));
        } catch (IOException rollback) {
          failure.addSuppressed(rollback);
        }
      throw failure;
    }
    return status(root, state, after);
  }

  public synchronized Status restore(RestoreRequest request) throws IOException {
    if (request == null) throw new IllegalArgumentException("Restore request is required.");
    return apply(new Request(request.revision(), null, 0));
  }

  public record Status(
      String root,
      String revision,
      String tint,
      int transparency,
      int fileCount,
      boolean applied) {}

  public record Request(String revision, String tint, Integer transparency) {}

  public record RestoreRequest(String revision) {}
}
