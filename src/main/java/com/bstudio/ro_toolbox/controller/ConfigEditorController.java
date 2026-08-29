package com.bstudio.ro_toolbox.controller;

import com.bstudio.ro_toolbox.service.configEditor.ConfigEditorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/config-editor")
@RequiredArgsConstructor
public class ConfigEditorController {

    private final ConfigEditorService configEditorService;

    @GetMapping("/status")
    public ConfigEditorService.ConfigEditorStatus status() throws IOException {
        return configEditorService.readStatus();
    }

    @PostMapping("/files/{fileId}")
    public ConfigEditorService.ConfigFileState saveFile(
            @PathVariable String fileId,
            @RequestBody SaveFileRequest request
    ) throws IOException {
        if (request == null || request.content() == null) {
            throw new IllegalArgumentException("content is required.");
        }
        return configEditorService.save(fileId, request.content());
    }

    @PostMapping("/folders/open")
    public MessageResponse openConfigFolder() throws IOException {
        Path configDir = configEditorService.getConfigDir();
        Files.createDirectories(configDir);
        openInDesktop(configDir);
        return new MessageResponse("Opened config folder.");
    }

    private void openInDesktop(Path path) {
        try {
            if (!Desktop.isDesktopSupported()) {
                openWithSystemCommand(path);
                return;
            }
            if (Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(path.toFile());
                return;
            }
            openWithSystemCommand(path);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to open folder: " + path.toAbsolutePath(), ex);
        }
    }

    private void openWithSystemCommand(Path path) throws IOException {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            new ProcessBuilder("explorer.exe", path.toAbsolutePath().toString()).start();
            return;
        }
        if (os.contains("mac")) {
            new ProcessBuilder("open", path.toAbsolutePath().toString()).start();
            return;
        }
        new ProcessBuilder("xdg-open", path.toAbsolutePath().toString()).start();
    }

    public record SaveFileRequest(String content) {
    }

    public record MessageResponse(String message) {
    }
}
