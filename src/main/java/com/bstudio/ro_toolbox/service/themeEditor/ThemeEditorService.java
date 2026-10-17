package com.bstudio.ro_toolbox.service.themeEditor;

import com.bstudio.ro_toolbox.service.app.AppConfigService;
import com.bstudio.ro_toolbox.util.AppDataPaths;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ThemeEditorService {
  private static final Pattern REGION =
      Pattern.compile("(?is)<style\\b[^>]*>(.*?)</style>|\\bstyle\\s*=\\s*([\"'])(.*?)\\2");
  private static final Pattern COMMENTS = Pattern.compile("(?s)/\\*.*?\\*/|<!--.*?-->");
  private static final Pattern BACKGROUND =
      Pattern.compile("(?i)(?<![\\w-])background(?:-color)?\\s*:\\s*([^;{}]+)");
  private static final Pattern HEAD_END = Pattern.compile("(?i)</head\\s*>");
  private final AppConfigService config;
  private final Path fixedRoot;
  private final Path backups;

  @Autowired
  public ThemeEditorService(AppConfigService config) {
    this(
        config, null, AppDataPaths.resolveRoToolboxAppDataRoot().resolve("theme-editor-originals"));
  }

  ThemeEditorService(AppConfigService config, Path root, Path backups) {
    this.config = config;
    this.fixedRoot = root;
    this.backups = backups.toAbsolutePath().normalize();
  }

  Path root() throws IOException {
    Path base = config.getSelectedGameBase();
    Path root =
        fixedRoot != null
            ? fixedRoot
            : base != null ? base.resolve("data/ui") : Path.of("D:\\Games\\ROSE Online\\data\\ui");
    if (!Files.isDirectory(root))
      throw new IllegalStateException("UI folder does not exist: " + root);
    return root.toRealPath();
  }

  public FileList list() throws IOException {
    Path root = root();
    try (var paths = Files.walk(root)) {
      var files =
          paths
              .filter(p -> Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS))
              .map(
                  p -> new FileEntry(root.relativize(p).toString().replace('\\', '/'), editable(p)))
              .sorted(Comparator.comparing(FileEntry::path))
              .toList();
      return new FileList(root.toString(), files);
    }
  }

  private boolean editable(Path path) {
    String name = path.toString().toLowerCase(Locale.ROOT);
    return name.endsWith(".html")
        || name.endsWith(".rml")
        || name.endsWith(".css")
        || name.endsWith(".rcss");
  }

  private Path resolve(String name) throws IOException {
    if (name == null || name.isBlank())
      throw new IllegalArgumentException("File path is required.");
    Path root = root();
    Path path = root.resolve(name).normalize();
    if (!path.startsWith(root) || !Files.isRegularFile(path) || !path.toRealPath().startsWith(root))
      throw new IllegalArgumentException("Choose an existing file inside the UI folder.");
    return path.toRealPath();
  }

  private String content(Path path) throws IOException {
    if (!editable(path))
      throw new IllegalArgumentException(
          "This file is an asset; only HTML, RML and CSS backgrounds can be edited.");
    if (Files.size(path) > 2 * 1024 * 1024)
      throw new IllegalArgumentException("File is too large to edit (maximum 2 MB).");
    return Files.readString(path, StandardCharsets.UTF_8);
  }

  public synchronized FileState read(String name) throws IOException {
    Path path = resolve(name);
    return state(name, path, content(path));
  }

  private FileState state(String name, Path path, String text) {
    return new FileState(
        name,
        hash(text),
        targets(path, text).stream().map(Target::view).toList(),
        Files.exists(backup(path)));
  }

  private List<Target> targets(Path path, String text) {
    List<Target> result = new ArrayList<>();
    String masked = mask(text);
    String lower = path.toString().toLowerCase(Locale.ROOT);
    if (lower.endsWith(".css") || lower.endsWith(".rcss")) scan(result, masked, 0, text);
    else {
      Matcher regions = REGION.matcher(masked);
      while (regions.find()) {
        int group = regions.group(1) != null ? 1 : 3;
        scan(result, regions.group(group), regions.start(group), text);
      }
      Matcher head = HEAD_END.matcher(masked);
      if (head.find() && !text.contains("/* RO Toolbox window background */")) {
        result.add(
            new Target(
                new Background(
                    "window",
                    "Window background override (replaces texture)",
                    0,
                    "#212121",
                    255,
                    "New override"),
                head.start(),
                head.start(),
                true));
      }
    }
    return result;
  }

  private String mask(String text) {
    Matcher matcher = COMMENTS.matcher(text);
    StringBuilder masked = new StringBuilder(text);
    while (matcher.find())
      for (int i = matcher.start(); i < matcher.end(); i++)
        if (masked.charAt(i) != '\n' && masked.charAt(i) != '\r') masked.setCharAt(i, ' ');
    return masked.toString();
  }

  private void scan(List<Target> targets, String region, int offset, String source) {
    Matcher matches = BACKGROUND.matcher(region);
    while (matches.find()) {
      String value = matches.group(1).trim();
      int[] rgba = parseColor(value.replaceFirst("(?i)\\s*!important\\s*$", ""));
      if (rgba == null) continue;
      int start = offset + matches.start(1), end = offset + matches.end(1);
      int brace = region.lastIndexOf('{', matches.start());
      int prior =
          Math.max(
              region.lastIndexOf('}', Math.max(0, brace - 1)),
              region.lastIndexOf('>', Math.max(0, brace - 1)));
      String label =
          brace >= 0
              ? region.substring(prior + 1, brace).trim().replaceAll("\\s+", " ")
              : "Inline background";
      if (label.length() > 180) label = label.substring(label.length() - 180);
      int line = 1 + (int) source.substring(0, start).chars().filter(c -> c == '\n').count();
      targets.add(
          new Target(
              new Background(
                  "bg-" + start,
                  label,
                  line,
                  String.format("#%02x%02x%02x", rgba[0], rgba[1], rgba[2]),
                  rgba[3],
                  value),
              start,
              end,
              false));
    }
  }

  private int[] parseColor(String color) {
    String value = color.trim().toLowerCase(Locale.ROOT);
    value =
        switch (value) {
          case "transparent" -> "#00000000";
          case "black" -> "#000000";
          case "white" -> "#ffffff";
          case "red" -> "#ff0000";
          case "blue" -> "#0000ff";
          case "green" -> "#008000";
          default -> value;
        };
    try {
      if (value.matches("#[0-9a-f]{3,4}|#[0-9a-f]{6}|#[0-9a-f]{8}")) {
        String hex = value.substring(1);
        if (hex.length() <= 4) {
          StringBuilder expanded = new StringBuilder();
          for (char c : hex.toCharArray()) expanded.append(c).append(c);
          hex = expanded.toString();
        }
        return new int[] {
          Integer.parseInt(hex.substring(0, 2), 16),
          Integer.parseInt(hex.substring(2, 4), 16),
          Integer.parseInt(hex.substring(4, 6), 16),
          hex.length() == 8 ? Integer.parseInt(hex.substring(6, 8), 16) : 255
        };
      }
      Matcher rgb =
          Pattern.compile(
                  "rgba?\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)(?:\\s*,\\s*(\\d+))?\\s*\\)")
              .matcher(value);
      if (rgb.matches()) {
        int[] result = {
          Integer.parseInt(rgb.group(1)),
          Integer.parseInt(rgb.group(2)),
          Integer.parseInt(rgb.group(3)),
          rgb.group(4) == null ? 255 : Integer.parseInt(rgb.group(4))
        };
        return Arrays.stream(result).allMatch(v -> v >= 0 && v <= 255) ? result : null;
      }
    } catch (NumberFormatException ignored) {
    }
    return null;
  }

  public synchronized FileState save(SaveRequest request) throws IOException {
    if (request == null || request.changes() == null || request.changes().isEmpty())
      throw new IllegalArgumentException("Choose at least one background to save.");
    Path path = resolve(request.path());
    String original = content(path);
    checkRevision(original, request.revision());
    Map<String, Target> targets = new HashMap<>();
    for (Target target : targets(path, original)) targets.put(target.view().id(), target);
    Map<Target, String> replacements = new HashMap<>();
    for (Change change : request.changes()) {
      if (change == null
          || change.color() == null
          || !change.color().matches("#[0-9a-fA-F]{6}")
          || change.alpha() == null
          || change.alpha() < 0
          || change.alpha() > 255)
        throw new IllegalArgumentException("Use a hex color and alpha from 0 to 255.");
      Target target = targets.get(change.id());
      if (target == null)
        throw new IllegalArgumentException("Background no longer exists. Reload the file.");
      int[] color = parseColor(change.color());
      String value =
          String.format(
              Locale.ROOT, "rgba(%d, %d, %d, %d)", color[0], color[1], color[2], change.alpha());
      if (target.insert()) {
        String nl = original.contains("\r\n") ? "\r\n" : "\n";
        value =
            "<style>/* RO Toolbox window background */"
                + nl
                + "body, body.window-fixed-size, body.window-resizeable, body.window-fixed, body.window-box, body.window-small { decorator: none; background-color: "
                + value
                + "; }"
                + nl
                + "</style>"
                + nl;
      } else if (target.view().original().toLowerCase(Locale.ROOT).contains("!important"))
        value += " !important";
      replacements.put(target, value);
    }
    StringBuilder updated = new StringBuilder(original);
    replacements.entrySet().stream()
        .sorted((a, b) -> Integer.compare(b.getKey().start(), a.getKey().start()))
        .forEach(e -> updated.replace(e.getKey().start(), e.getKey().end(), e.getValue()));
    Path backup = backup(path);
    Files.createDirectories(backup.getParent());
    if (!Files.exists(backup)) Files.copy(path, backup);
    write(path, updated.toString());
    return state(request.path(), path, updated.toString());
  }

  public synchronized FileState restore(String name, String revision) throws IOException {
    Path path = resolve(name);
    checkRevision(content(path), revision);
    Path backup = backup(path);
    if (!Files.isRegularFile(backup))
      throw new IllegalArgumentException("No original backup exists for this file.");
    String original = Files.readString(backup, StandardCharsets.UTF_8);
    write(path, original);
    return state(name, path, original);
  }

  private void checkRevision(String text, String revision) {
    if (!hash(text).equals(revision))
      throw new IllegalStateException(
          "This file changed outside the editor. Reload it before saving.");
  }

  Path backup(Path path) {
    return backups.resolve(hash(path.toString())).resolve(path.getFileName());
  }

  void write(Path path, String text) throws IOException {
    Path temporary = Files.createTempFile(path.getParent(), ".ro-toolbox-", ".tmp");
    try {
      Files.writeString(temporary, text, StandardCharsets.UTF_8);
      try {
        Files.move(
            temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException ex) {
        Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
      }
    } finally {
      Files.deleteIfExists(temporary);
    }
  }

  static String hash(String text) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private record Target(Background view, int start, int end, boolean insert) {}

  String globalContent(Path path, String original, String tint, int transparency) {
    if (tint == null && transparency == 0) return original;
    if (path.getFileName().toString().equals("custom_ui.lua")) {
      String runtime = original;
      if (transparency > 0) {
        Pattern alpha =
            Pattern.compile(
                "local alpha_byte\\s*=\\s*math\\.floor\\(alpha\\s*\\*\\s*255\\s*/\\s*100\\s*\\+\\s*0\\.5\\)");
        Matcher match = alpha.matcher(runtime);
        if (!match.find())
          throw new IllegalStateException("Runtime UI transparency format is unsupported: " + path);
        runtime =
            match.replaceFirst(
                "local alpha_byte = math.floor(alpha * 255 / 100 * "
                    + (100 - transparency)
                    + " / 100 + 0.5)");
      }
      if (tint != null) {
        Pattern color = Pattern.compile("(?m)^(\\s*)local background_color\\s*=[^\\r\\n]*");
        Matcher match = color.matcher(runtime);
        if (!match.find())
          throw new IllegalStateException("Runtime UI color format is unsupported: " + path);
        int[] rgb = parseColor(tint);
        String replacement =
            match.group(1)
                + "local background_color = 'rgba("
                + rgb[0]
                + ", "
                + rgb[1]
                + ", "
                + rgb[2]
                + ", ' .. alpha_byte .. ')'";
        runtime = match.replaceFirst(Matcher.quoteReplacement(replacement));
      }
      return runtime;
    }
    StringBuilder updated = new StringBuilder(original);
    targets(path, original).stream()
        .filter(t -> !t.insert())
        .sorted(Comparator.comparingInt(Target::start).reversed())
        .forEach(
            target -> {
              Background bg = target.view();
              boolean backdrop = isScreenBackdrop(path, original, target.start(), bg.selector());
              int[] rgb = parseColor(tint == null || backdrop ? bg.color() : tint);
              int alpha = (int) Math.round(bg.alpha() * (100 - transparency) / 100.0);
              String value =
                  String.format(Locale.ROOT, "rgba(%d, %d, %d, %d)", rgb[0], rgb[1], rgb[2], alpha);
              if (bg.original().toLowerCase(Locale.ROOT).contains("!important"))
                value += " !important";
              updated.replace(target.start(), target.end(), value);
            });
    // Tiled decorations cover background colors. Replace panel tiles, while keeping image icons.
    String text = updated.toString();
    Pattern tiles =
        Pattern.compile("(?is)(?<![\\w-])decorator\\s*:\\s*tiled-box\\s*\\([^;]*?\\)\\s*;");
    Matcher matches = tiles.matcher(mask(text));
    List<int[]> ranges = new ArrayList<>();
    while (matches.find()) ranges.add(new int[] {matches.start(), matches.end()});
    int[] rgb = parseColor(tint == null ? "#212121" : tint);
    String panel =
        String.format(
            Locale.ROOT,
            "decorator: none; background-color: rgba(%d, %d, %d, %d);",
            rgb[0],
            rgb[1],
            rgb[2],
            (int) Math.round(255 * (100 - transparency) / 100.0));
    // Only process stylesheet regions, never script text or HTML attributes unrelated to style.
    String lower = path.toString().toLowerCase(Locale.ROOT);
    List<int[]> styleRanges = new ArrayList<>();
    if (lower.endsWith(".css") || lower.endsWith(".rcss"))
      styleRanges.add(new int[] {0, text.length()});
    else {
      Matcher styles = REGION.matcher(mask(text));
      while (styles.find()) {
        int g = styles.group(1) != null ? 1 : 3;
        styleRanges.add(new int[] {styles.start(g), styles.end(g)});
      }
    }
    StringBuilder result = new StringBuilder(text);
    for (int i = ranges.size() - 1; i >= 0; i--) {
      int[] range = ranges.get(i);
      if (styleRanges.stream().anyMatch(s -> range[0] >= s[0] && range[1] <= s[1])) {
        if (isScreenBackdrop(path, text, range[0], "")) continue;
        int blockStart = text.lastIndexOf('{', range[0]);
        int blockEnd = text.indexOf('}', range[1]);
        String block = blockStart >= 0 && blockEnd >= 0 ? text.substring(blockStart, blockEnd) : "";
        Matcher background = BACKGROUND.matcher(mask(block));
        boolean hasColor = false;
        while (background.find()) {
          if (parseColor(background.group(1).trim().replaceFirst("(?i)\\s*!important\\s*$", ""))
              != null) hasColor = true;
        }
        result.replace(range[0], range[1], hasColor ? "decorator: none;" : panel);
      }
    }
    return result.toString();
  }

  private boolean isScreenBackdrop(Path path, String source, int offset, String selector) {
    String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
    if (name.matches(".*(?:overlay|backdrop|scrim).*")) return true;
    if (selector.toLowerCase(Locale.ROOT).matches(".*(?:overlay|backdrop|scrim).*")) return true;
    String masked = mask(source);
    int start = masked.lastIndexOf('{', offset);
    int end = masked.indexOf('}', offset);
    if (start < 0 || end < 0) return false;
    int prior = masked.lastIndexOf('}', start);
    String blockSelector = masked.substring(prior + 1, start).toLowerCase(Locale.ROOT);
    if (blockSelector.matches("(?s).*(?:overlay|backdrop|scrim).*")) return true;
    String block = masked.substring(start + 1, end);
    // Viewport-covering shades are not panel surfaces, even when they have generic selectors.
    boolean fullWidth =
        Pattern.compile("(?i)(?<![\\w-])width\\s*:\\s*100(?:%|vw)\\s*(?:;|$)")
            .matcher(block)
            .find();
    boolean fullHeight =
        Pattern.compile("(?i)(?<![\\w-])height\\s*:\\s*100(?:%|vh)\\s*(?:;|$)")
            .matcher(block)
            .find();
    return fullWidth && fullHeight;
  }

  public record FileEntry(String path, boolean editable) {}

  public record FileList(String root, List<FileEntry> files) {}

  public record Background(
      String id, String selector, int line, String color, int alpha, String original) {}

  public record FileState(
      String path, String revision, List<Background> backgrounds, boolean hasBackup) {}

  public record Change(String id, String color, Integer alpha) {}

  public record SaveRequest(String path, String revision, List<Change> changes) {}
}
