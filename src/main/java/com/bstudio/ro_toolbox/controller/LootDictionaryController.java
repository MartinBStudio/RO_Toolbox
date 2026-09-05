package com.bstudio.ro_toolbox.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/loot")
public class LootDictionaryController {
  @Autowired
  private ResourceLoader resourceLoader;

  @GetMapping(value = "/dictionary", produces = MediaType.APPLICATION_JSON_VALUE)
  public String getLootDictionary() throws IOException {
    return new String(
        resourceLoader.getResource("classpath:static/loot-model-folder-dictionary.json").getInputStream().readAllBytes(),
        StandardCharsets.UTF_8
    );
  }
}
