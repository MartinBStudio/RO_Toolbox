package com.bstudio.ro_toolbox.service.configEditor;

import org.springframework.stereotype.Service;
import org.tomlj.Toml;
import org.tomlj.TomlArray;
import org.tomlj.TomlParseError;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlTable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ConfigEditorService {
    private static final String IGNORE_ID = "ignore";
    private static final String ROSE_ID = "rose";
    private static final List<ConfigFileSpec> TARGET_FILES = List.of(
            new ConfigFileSpec(IGNORE_ID, "ignore.toml"),
            new ConfigFileSpec(ROSE_ID, "rose.toml")
    );

    private final Path configDir;

    public ConfigEditorService() {
        this(resolveRoseConfigDir());
    }

    ConfigEditorService(Path configDir) {
        this.configDir = configDir.toAbsolutePath().normalize();
    }

    public ConfigEditorStatus readStatus() throws IOException {
        List<ConfigFileState> files = new ArrayList<>();
        for (ConfigFileSpec spec : TARGET_FILES) {
            files.add(readFileState(spec));
        }
        return new ConfigEditorStatus(
                configDir.toString(),
                Files.exists(configDir) && Files.isDirectory(configDir),
                files
        );
    }

    public ConfigFileState save(String fileId, String content) throws IOException {
        ConfigFileSpec spec = findTargetFile(fileId);
        if (content == null) {
            throw new IllegalArgumentException("content is required.");
        }

        TomlParseResult parsed = Toml.parse(content);
        if (parsed.hasErrors()) {
            throw new IllegalArgumentException(buildTomlErrorMessage(spec.fileName(), parsed.errors().getFirst()));
        }

        Files.createDirectories(configDir);
        Path filePath = configDir.resolve(spec.fileName());
        Files.writeString(filePath, content, StandardCharsets.UTF_8);
        return buildFileState(spec, content, parsed, true);
    }

    public Path getConfigDir() {
        return configDir;
    }

    public IgnoreListState readIgnoreList() throws IOException {
        ParsedIgnoreData data = readIgnoreData();
        return new IgnoreListState(data.names(), data.fileState());
    }

    public IgnoreListState addIgnoreName(String name) throws IOException {
        String normalizedName = normalizeIgnoreName(name);
        ParsedIgnoreData data = readIgnoreData();
        boolean exists = data.names().stream().anyMatch(entry -> entry.equalsIgnoreCase(normalizedName));
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
        List<String> updatedNames = data.names().stream()
                .filter(entry -> !entry.equalsIgnoreCase(normalizedName))
                .collect(Collectors.toCollection(ArrayList::new));
        if (updatedNames.size() == data.names().size()) {
            throw new IllegalArgumentException("Ignore entry not found: " + normalizedName);
        }
        ConfigFileState updatedFile = saveIgnoreList(updatedNames);
        return new IgnoreListState(updatedNames, updatedFile);
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
            return new ConfigFileState(spec.id(), spec.fileName(), filePath.toString(), false, null, null, null);
        }

        String content = Files.readString(filePath, StandardCharsets.UTF_8);
        TomlParseResult parsed = Toml.parse(content);
        if (parsed.hasErrors()) {
            String parseError = buildTomlErrorMessage(spec.fileName(), parsed.errors().getFirst());
            return new ConfigFileState(spec.id(), spec.fileName(), filePath.toString(), true, content, null, parseError);
        }
        return buildFileState(spec, content, parsed, true);
    }

    private ConfigFileState buildFileState(ConfigFileSpec spec, String content, TomlParseResult parsed, boolean exists) {
        Map<String, Object> parsedObject = convertTable(parsed);
        return new ConfigFileState(spec.id(), spec.fileName(), configDir.resolve(spec.fileName()).toString(), exists, content, parsedObject, null);
    }

    private static String buildTomlErrorMessage(String fileName, TomlParseError error) {
        if (error.position() != null) {
            return "Invalid TOML in " + fileName + " at line " + error.position().line() + ", column " + error.position().column() + ": " + error.getMessage();
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
        if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean) {
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
            throw new IllegalArgumentException("Invalid ignore.toml format: expected [[ignore]] table array.");
        }

        List<String> names = new ArrayList<>();
        for (int index = 0; index < ignoreArray.size(); index++) {
            Object item = ignoreArray.get(index);
            if (!(item instanceof TomlTable table)) {
                throw new IllegalArgumentException("Invalid ignore.toml format: each [[ignore]] entry must be a table.");
            }
            String name = table.getString("name");
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Invalid ignore.toml format: each [[ignore]] entry requires non-empty name.");
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
            builder.append("name = '").append(escapeTomlLiteral(names.get(index))).append("'").append(System.lineSeparator());
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

    private static Path resolveRoseConfigDir() {
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) {
            return Paths.get(appData, "Rednim Games", "ROSE Online", "config");
        }
        return Paths.get(System.getProperty("user.home"), "AppData", "Roaming", "Rednim Games", "ROSE Online", "config");
    }

    private record ConfigFileSpec(String id, String fileName) {
    }

    public record ConfigEditorStatus(String configDir, boolean configDirExists, List<ConfigFileState> files) {
    }

    public record ConfigFileState(
            String id,
            String fileName,
            String filePath,
            boolean exists,
            String content,
            Map<String, Object> parsed,
            String parseError
    ) {
    }

    public record IgnoreListState(List<String> names, ConfigFileState file) {
    }

    private record ParsedIgnoreData(List<String> names, ConfigFileState fileState) {
    }
}
