package com.bstudio.ro_toolbox.service.configEditor;

import com.bstudio.ro_toolbox.service.app.AppConfigService;
import com.bstudio.ro_toolbox.service.backup.BackupProviderResolver;
import com.bstudio.ro_toolbox.service.backup.BackupProviderResolver.BackupProvider;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.tomlj.Toml;
import org.tomlj.TomlArray;
import org.tomlj.TomlParseError;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlTable;

@Service
public class ConfigEditorService {
  private static final String IGNORE_ID = "ignore";
  private static final String ROSE_ID = "rose";
  private static final String SHOW_DROPPED_ITEM_NAME_KEY = "show_dropped_item_name";
  private static final DateTimeFormatter BACKUP_TIMESTAMP_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
  private static final Pattern SHOW_DROPPED_ITEM_NAME_BOOLEAN_PATTERN =
      Pattern.compile("^(\\s*show_dropped_item_name\\s*=\\s*)(true|false)(\\s*(?:#.*)?)$");
  private static final Pattern SHOW_DROPPED_ITEM_NAME_ASSIGNMENT_PATTERN =
      Pattern.compile("^\\s*show_dropped_item_name\\s*=\\s*.+$");
  private static final List<ConfigFileSpec> TARGET_FILES =
      List.of(
          new ConfigFileSpec(IGNORE_ID, "ignore.toml"), new ConfigFileSpec(ROSE_ID, "rose.toml"));

  private final Path configDir;
  private final BackupProviderResolver backupProviderResolver;

  public ConfigEditorService() {
    this(resolveRoseConfigDir(), new AppConfigService());
  }

  ConfigEditorService(Path configDir) {
    this(configDir, new AppConfigService());
  }

  ConfigEditorService(Path configDir, AppConfigService appConfigService) {
    this.configDir = configDir.toAbsolutePath().normalize();
    this.backupProviderResolver = new BackupProviderResolver(appConfigService);
  }

  public ConfigEditorStatus readStatus() throws IOException {
    List<ConfigFileState> files = new ArrayList<>();
    for (ConfigFileSpec spec : TARGET_FILES) {
      files.add(readFileState(spec));
    }
    return new ConfigEditorStatus(
        configDir.toString(),
        Files.exists(configDir) && Files.isDirectory(configDir),
        files,
        readOneDriveBackupStatus());
  }

  public ConfigFileState save(String fileId, String content) throws IOException {
    ConfigFileSpec spec = findTargetFile(fileId);
    if (content == null) {
      throw new IllegalArgumentException("content is required.");
    }

    TomlParseResult parsed = Toml.parse(content);
    if (parsed.hasErrors()) {
      throw new IllegalArgumentException(
          buildTomlErrorMessage(spec.fileName(), parsed.errors().getFirst()));
    }

    Files.createDirectories(configDir);
    Path filePath = configDir.resolve(spec.fileName());
    Files.writeString(filePath, content, StandardCharsets.UTF_8);
    return buildFileState(spec, content, parsed, true);
  }

  public Path getConfigDir() {
    return configDir;
  }

  public OneDriveBackupStatus readOneDriveBackupStatus() {
    Optional<BackupProvider> provider = backupProviderResolver.resolveDefaultProvider();
    if (provider.isEmpty()) {
      return new OneDriveBackupStatus(false, null, null, null, List.of());
    }
    Path backupRoot = resolveProviderBackupRoot(provider.get());
    return new OneDriveBackupStatus(
        true,
        provider.get().name(),
        provider.get().root().toString(),
        backupRoot.toString(),
        readBackups(backupRoot));
  }

  public Path getOneDriveBackupRoot() {
    BackupProvider provider =
        backupProviderResolver
            .resolveDefaultProvider()
            .orElseThrow(() -> new IllegalStateException("No backup provider was detected."));
    return resolveProviderBackupRoot(provider);
  }

  public OneDriveBackupResult backupToOneDrive() throws IOException {
    BackupProvider provider =
        backupProviderResolver
            .resolveDefaultProvider()
            .orElseThrow(() -> new IllegalStateException("No backup provider was detected."));
    if (!Files.exists(configDir) || !Files.isDirectory(configDir)) {
      throw new IllegalStateException("ROSE config folder was not found.");
    }

    List<Path> existingFiles = new ArrayList<>();
    for (ConfigFileSpec spec : TARGET_FILES) {
      Path source = configDir.resolve(spec.fileName());
      if (Files.exists(source) && Files.isRegularFile(source)) {
        existingFiles.add(source);
      }
    }
    if (existingFiles.isEmpty()) {
      throw new IllegalStateException("No ROSE config files were found to back up.");
    }

    Path backupRoot = resolveProviderBackupRoot(provider);
    Path backupDir = backupRoot.resolve(LocalDateTime.now().format(BACKUP_TIMESTAMP_FORMATTER));
    Files.createDirectories(backupDir);
    for (Path source : existingFiles) {
      Files.copy(
          source,
          backupDir.resolve(source.getFileName()),
          StandardCopyOption.REPLACE_EXISTING,
          StandardCopyOption.COPY_ATTRIBUTES);
    }

    return new OneDriveBackupResult(
        backupDir.toString(),
        existingFiles.size(),
        existingFiles.stream().map(path -> path.getFileName().toString()).toList());
  }

  public OneDriveRestoreResult restoreFromOneDriveBackup(String backupName) throws IOException {
    String normalizedBackupName = normalizeBackupName(backupName);
    BackupProvider provider =
        backupProviderResolver
            .resolveDefaultProvider()
            .orElseThrow(() -> new IllegalStateException("No backup provider was detected."));
    Path backupRoot = resolveProviderBackupRoot(provider);
    Path backupDir = backupRoot.resolve(normalizedBackupName).toAbsolutePath().normalize();
    if (!backupDir.startsWith(backupRoot.toAbsolutePath().normalize())
        || !Files.exists(backupDir)
        || !Files.isDirectory(backupDir)) {
      throw new IllegalArgumentException("Selected OneDrive backup was not found.");
    }

    List<Path> backupFiles = new ArrayList<>();
    for (ConfigFileSpec spec : TARGET_FILES) {
      Path source = backupDir.resolve(spec.fileName());
      if (Files.exists(source) && Files.isRegularFile(source)) {
        backupFiles.add(source);
      }
    }
    if (backupFiles.isEmpty()) {
      throw new IllegalStateException("Selected OneDrive backup has no supported config files.");
    }

    Path safetyBackupDir =
        backupRoot.resolve("pre-restore-" + LocalDateTime.now().format(BACKUP_TIMESTAMP_FORMATTER));
    List<String> safetyFiles = backupExistingConfigFiles(safetyBackupDir);

    Files.createDirectories(configDir);
    for (Path source : backupFiles) {
      Files.copy(
          source,
          configDir.resolve(source.getFileName()),
          StandardCopyOption.REPLACE_EXISTING,
          StandardCopyOption.COPY_ATTRIBUTES);
    }

    return new OneDriveRestoreResult(
        backupDir.toString(),
        backupFiles.size(),
        backupFiles.stream().map(path -> path.getFileName().toString()).toList(),
        safetyFiles.isEmpty() ? null : safetyBackupDir.toString(),
        safetyFiles);
  }

  public IgnoreListState readIgnoreList() throws IOException {
    ParsedIgnoreData data = readIgnoreData();
    return new IgnoreListState(data.names(), data.fileState());
  }

  public IgnoreListState addIgnoreName(String name) throws IOException {
    String normalizedName = normalizeIgnoreName(name);
    ParsedIgnoreData data = readIgnoreData();
    boolean exists =
        data.names().stream().anyMatch(entry -> entry.equalsIgnoreCase(normalizedName));
    if (exists) {
      throw new IllegalArgumentException("Ignore entry already exists: " + normalizedName);
    }
    List<String> updatedNames = new ArrayList<>(data.names());
    updatedNames.add(normalizedName);
    ConfigFileState updatedFile = saveIgnoreList(updatedNames);
    return new IgnoreListState(updatedNames, updatedFile);
  }

  public IgnoreListState deleteIgnoreName(String name) throws IOException {
    String normalizedName = normalizeIgnoreName(name);
    ParsedIgnoreData data = readIgnoreData();
    List<String> updatedNames =
        data.names().stream()
            .filter(entry -> !entry.equalsIgnoreCase(normalizedName))
            .collect(Collectors.toCollection(ArrayList::new));
    if (updatedNames.size() == data.names().size()) {
      throw new IllegalArgumentException("Ignore entry not found: " + normalizedName);
    }
    ConfigFileState updatedFile = saveIgnoreList(updatedNames);
    return new IgnoreListState(updatedNames, updatedFile);
  }

  public RoseConfigState readRoseConfigState() throws IOException {
    ConfigFileSpec roseSpec = findTargetFile(ROSE_ID);
    ConfigFileState fileState = readFileState(roseSpec);
    if (fileState.parseError() != null) {
      throw new IllegalArgumentException(fileState.parseError());
    }
    boolean showDroppedItemName = false;
    if (fileState.parsed() != null) {
      showDroppedItemName =
          findBooleanByKey(fileState.parsed(), SHOW_DROPPED_ITEM_NAME_KEY).orElse(false);
    }
    return new RoseConfigState(showDroppedItemName, fileState.exists());
  }

  public RoseConfigState setShowDroppedItemName(boolean enabled) throws IOException {
    ConfigFileSpec roseSpec = findTargetFile(ROSE_ID);
    ConfigFileState fileState = readFileState(roseSpec);
    if (fileState.parseError() != null) {
      throw new IllegalArgumentException(fileState.parseError());
    }
    String updatedContent =
        upsertRoseBooleanValue(fileState.content(), SHOW_DROPPED_ITEM_NAME_KEY, enabled);
    save(roseSpec.id(), updatedContent);
    return readRoseConfigState();
  }

  private ConfigFileSpec findTargetFile(String fileId) {
    String normalized = fileId == null ? "" : fileId.trim().toLowerCase();
    return TARGET_FILES.stream()
        .filter(file -> file.id().equals(normalized))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unsupported file id: " + fileId));
  }

  private ConfigFileState readFileState(ConfigFileSpec spec) throws IOException {
    Path filePath = configDir.resolve(spec.fileName());
    if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
      return new ConfigFileState(
          spec.id(), spec.fileName(), filePath.toString(), false, null, null, null);
    }

    String content = Files.readString(filePath, StandardCharsets.UTF_8);
    TomlParseResult parsed = Toml.parse(content);
    if (parsed.hasErrors()) {
      String parseError = buildTomlErrorMessage(spec.fileName(), parsed.errors().getFirst());
      return new ConfigFileState(
          spec.id(), spec.fileName(), filePath.toString(), true, content, null, parseError);
    }
    return buildFileState(spec, content, parsed, true);
  }

  private ConfigFileState buildFileState(
      ConfigFileSpec spec, String content, TomlParseResult parsed, boolean exists) {
    Map<String, Object> parsedObject = convertTable(parsed);
    return new ConfigFileState(
        spec.id(),
        spec.fileName(),
        configDir.resolve(spec.fileName()).toString(),
        exists,
        content,
        parsedObject,
        null);
  }

  private static String buildTomlErrorMessage(String fileName, TomlParseError error) {
    if (error.position() != null) {
      return "Invalid TOML in "
          + fileName
          + " at line "
          + error.position().line()
          + ", column "
          + error.position().column()
          + ": "
          + error.getMessage();
    }
    return "Invalid TOML in " + fileName + ": " + error.getMessage();
  }

  private Map<String, Object> convertTable(TomlTable table) {
    Map<String, Object> output = new LinkedHashMap<>();
    for (String key : table.keySet()) {
      output.put(key, convertValue(table.get(key)));
    }
    return output;
  }

  private Object convertValue(Object value) {
    if (value == null
        || value instanceof String
        || value instanceof Number
        || value instanceof Boolean) {
      return value;
    }
    if (value instanceof TomlTable nestedTable) {
      return convertTable(nestedTable);
    }
    if (value instanceof TomlArray array) {
      List<Object> values = new ArrayList<>();
      for (int i = 0; i < array.size(); i++) {
        values.add(convertValue(array.get(i)));
      }
      return values;
    }
    return value.toString();
  }

  private ParsedIgnoreData readIgnoreData() throws IOException {
    ConfigFileSpec ignoreSpec = findTargetFile(IGNORE_ID);
    ConfigFileState fileState = readFileState(ignoreSpec);
    if (fileState.parseError() != null) {
      throw new IllegalArgumentException(fileState.parseError());
    }
    if (fileState.content() == null || fileState.content().isBlank()) {
      return new ParsedIgnoreData(new ArrayList<>(), fileState);
    }

    TomlParseResult parsed = Toml.parse(fileState.content());
    List<String> names = parseIgnoreNames(parsed);
    return new ParsedIgnoreData(names, fileState);
  }

  private List<String> parseIgnoreNames(TomlParseResult parsed) {
    Object ignoreValue = parsed.get("ignore");
    if (ignoreValue == null) {
      return new ArrayList<>();
    }
    if (!(ignoreValue instanceof TomlArray ignoreArray)) {
      throw new IllegalArgumentException(
          "Invalid ignore.toml format: expected [[ignore]] table array.");
    }

    List<String> names = new ArrayList<>();
    for (int index = 0; index < ignoreArray.size(); index++) {
      Object item = ignoreArray.get(index);
      if (!(item instanceof TomlTable table)) {
        throw new IllegalArgumentException(
            "Invalid ignore.toml format: each [[ignore]] entry must be a table.");
      }
      String name = table.getString("name");
      if (name == null || name.isBlank()) {
        throw new IllegalArgumentException(
            "Invalid ignore.toml format: each [[ignore]] entry requires non-empty name.");
      }
      names.add(name.trim());
    }
    return names;
  }

  private ConfigFileState saveIgnoreList(List<String> names) throws IOException {
    String content = buildIgnoreToml(names);
    return save(IGNORE_ID, content);
  }

  private String buildIgnoreToml(List<String> names) {
    if (names.isEmpty()) {
      return "";
    }
    StringBuilder builder = new StringBuilder();
    for (int index = 0; index < names.size(); index++) {
      if (index > 0) {
        builder.append(System.lineSeparator());
      }
      builder.append("[[ignore]]").append(System.lineSeparator());
      builder
          .append("name = '")
          .append(escapeTomlLiteral(names.get(index)))
          .append("'")
          .append(System.lineSeparator());
    }
    return builder.toString();
  }

  private static String normalizeIgnoreName(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("name is required.");
    }
    return value.trim();
  }

  private static String escapeTomlLiteral(String value) {
    return value.replace("'", "''");
  }

  private String upsertRoseBooleanValue(String content, String key, boolean enabled) {
    String normalizedContent = content == null ? "" : content;
    if (normalizedContent.isBlank()) {
      return key + " = " + enabled + System.lineSeparator();
    }

    String[] lines = normalizedContent.split("\\R", -1);
    String newline = normalizedContent.contains("\r\n") ? "\r\n" : "\n";
    boolean updated = false;
    for (int index = 0; index < lines.length; index++) {
      String line = lines[index];
      Matcher matcher = SHOW_DROPPED_ITEM_NAME_BOOLEAN_PATTERN.matcher(line);
      if (matcher.matches()) {
        lines[index] = matcher.group(1) + enabled + matcher.group(3);
        updated = true;
        break;
      }
      if (SHOW_DROPPED_ITEM_NAME_ASSIGNMENT_PATTERN.matcher(line).matches()) {
        throw new IllegalArgumentException(
            "Invalid rose.toml format: show_dropped_item_name must be true or false.");
      }
    }

    if (!updated) {
      StringBuilder appended = new StringBuilder(normalizedContent);
      if (!normalizedContent.endsWith("\n") && !normalizedContent.endsWith("\r\n")) {
        appended.append(newline);
      }
      appended.append(key).append(" = ").append(enabled).append(newline);
      return appended.toString();
    }

    return String.join(newline, lines);
  }

  private Optional<Boolean> findBooleanByKey(Object node, String key) {
    if (node instanceof Map<?, ?> map) {
      for (Map.Entry<?, ?> entry : map.entrySet()) {
        if (entry.getKey() instanceof String entryKey && entryKey.equals(key)) {
          Object value = entry.getValue();
          if (value instanceof Boolean booleanValue) {
            return Optional.of(booleanValue);
          }
          throw new IllegalArgumentException(
              "Invalid rose.toml format: show_dropped_item_name must be true or false.");
        }
        Optional<Boolean> nested = findBooleanByKey(entry.getValue(), key);
        if (nested.isPresent()) {
          return nested;
        }
      }
    }
    if (node instanceof List<?> values) {
      for (Object item : values) {
        Optional<Boolean> nested = findBooleanByKey(item, key);
        if (nested.isPresent()) {
          return nested;
        }
      }
    }
    return Optional.empty();
  }

  private static Path resolveRoseConfigDir() {
    String appData = System.getenv("APPDATA");
    if (appData != null && !appData.isBlank()) {
      return Paths.get(appData, "Rednim Games", "ROSE Online", "config");
    }
    return Paths.get(
        System.getProperty("user.home"),
        "AppData",
        "Roaming",
        "Rednim Games",
        "ROSE Online",
        "config");
  }

  private Path resolveProviderBackupRoot(BackupProvider provider) {
    return backupProviderResolver.resolveBackupRoot(provider, "ROSE Online Config");
  }

  private List<OneDriveBackupEntry> readBackups(Path backupRoot) {
    if (!Files.exists(backupRoot) || !Files.isDirectory(backupRoot)) {
      return List.of();
    }
    try (var entries = Files.list(backupRoot)) {
      return entries
          .filter(Files::isDirectory)
          .filter(path -> !path.getFileName().toString().startsWith("pre-restore-"))
          .map(this::readBackupEntry)
          .filter(Optional::isPresent)
          .map(Optional::get)
          .sorted((first, second) -> second.name().compareTo(first.name()))
          .toList();
    } catch (IOException ex) {
      return List.of();
    }
  }

  private Optional<OneDriveBackupEntry> readBackupEntry(Path backupDir) {
    List<String> files =
        TARGET_FILES.stream()
            .map(ConfigFileSpec::fileName)
            .filter(fileName -> Files.isRegularFile(backupDir.resolve(fileName)))
            .toList();
    if (files.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(
        new OneDriveBackupEntry(backupDir.getFileName().toString(), backupDir.toString(), files));
  }

  private List<String> backupExistingConfigFiles(Path safetyBackupDir) throws IOException {
    if (!Files.exists(configDir) || !Files.isDirectory(configDir)) {
      return List.of();
    }

    List<String> copiedFiles = new ArrayList<>();
    for (ConfigFileSpec spec : TARGET_FILES) {
      Path source = configDir.resolve(spec.fileName());
      if (!Files.exists(source) || !Files.isRegularFile(source)) {
        continue;
      }
      Files.createDirectories(safetyBackupDir);
      Files.copy(
          source,
          safetyBackupDir.resolve(source.getFileName()),
          StandardCopyOption.REPLACE_EXISTING,
          StandardCopyOption.COPY_ATTRIBUTES);
      copiedFiles.add(source.getFileName().toString());
    }
    return copiedFiles;
  }

  private String normalizeBackupName(String backupName) {
    if (backupName == null || backupName.isBlank()) {
      throw new IllegalArgumentException("backupName is required.");
    }
    String normalized = backupName.trim();
    if (normalized.contains("/") || normalized.contains("\\") || normalized.equals("..")) {
      throw new IllegalArgumentException("backupName is invalid.");
    }
    return normalized;
  }

  private record ConfigFileSpec(String id, String fileName) {}

  public record ConfigEditorStatus(
      String configDir,
      boolean configDirExists,
      List<ConfigFileState> files,
      OneDriveBackupStatus oneDriveBackup) {}

  public record ConfigFileState(
      String id,
      String fileName,
      String filePath,
      boolean exists,
      String content,
      Map<String, Object> parsed,
      String parseError) {}

  public record IgnoreListState(List<String> names, ConfigFileState file) {}

  public record RoseConfigState(boolean showDroppedItemName, boolean roseFileExists) {}

  public record OneDriveBackupStatus(
      boolean available,
      String providerName,
      String oneDrivePath,
      String backupRootPath,
      List<OneDriveBackupEntry> backups) {}

  public record OneDriveBackupResult(String backupPath, int copiedFiles, List<String> files) {}

  public record OneDriveBackupEntry(String name, String path, List<String> files) {}

  public record OneDriveRestoreResult(
      String restoredFrom,
      int restoredFiles,
      List<String> files,
      String safetyBackupPath,
      List<String> safetyBackupFiles) {}

  private record ParsedIgnoreData(List<String> names, ConfigFileState fileState) {}
}
