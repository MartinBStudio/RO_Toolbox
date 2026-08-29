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
}
