package com.bstudio.ro_toolbox.controller.themeEditor;

import com.bstudio.ro_toolbox.service.themeEditor.ThemeEditorService;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/theme-editor")
@RequiredArgsConstructor
public class ThemeEditorController {
  private final ThemeEditorService service;

  @GetMapping("/files")
  public ThemeEditorService.FileList files() throws IOException {
    return service.list();
  }

  @GetMapping("/file")
  public ThemeEditorService.FileState file(@RequestParam String path) throws IOException {
    return service.read(path);
  }

  @PostMapping("/file")
  public ThemeEditorService.FileState save(@RequestBody ThemeEditorService.SaveRequest request)
      throws IOException {
    return service.save(request);
  }

  @PostMapping("/restore")
  public ThemeEditorService.FileState restore(@RequestBody RestoreRequest request)
      throws IOException {
    if (request == null) throw new IllegalArgumentException("Restore request is required.");
    return service.restore(request.path(), request.revision());
  }

  public record RestoreRequest(String path, String revision) {}
}
