package com.bstudio.ro_toolbox.controller.themeEditor;

import com.bstudio.ro_toolbox.service.themeEditor.GlobalThemeService;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/theme-editor/global")
@RequiredArgsConstructor
public class GlobalThemeController {
  private final GlobalThemeService service;

  @GetMapping
  public GlobalThemeService.Status status() throws IOException {
    return service.status();
  }

  @PostMapping
  public GlobalThemeService.Status apply(@RequestBody GlobalThemeService.Request request)
      throws IOException {
    return service.apply(request);
  }

  @PostMapping("/restore")
  public GlobalThemeService.Status restore(@RequestBody GlobalThemeService.RestoreRequest request)
      throws IOException {
    return service.restore(request);
  }
}
