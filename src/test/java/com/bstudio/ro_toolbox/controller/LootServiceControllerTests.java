package com.bstudio.ro_toolbox.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.bstudio.ro_toolbox.controller.resourceReplacer.LootServiceController;
import com.bstudio.ro_toolbox.service.app.AppConfigService;
import com.bstudio.ro_toolbox.service.resourceReplacer.service.loot.LootManager;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

class LootServiceControllerTests {

  private LootServiceController controller;

  @BeforeEach
  void setUp() {
    controller =
        new LootServiceController(
            mock(LootManager.class),
            mock(AppConfigService.class),
            new DefaultResourceLoader());
  }

  @Test
  void returnsDictionaryContent() throws IOException {
    String dictionary = controller.getLootDictionary();
    assertNotNull(dictionary);
    assertTrue(dictionary.contains("\"ARM\"") || dictionary.contains("\"key\": \"ARM\""));
  }

  @Test
  void returnsItemPreviewsMap() throws IOException {
    Map<String, List<String>> previews = controller.getItemPreviews();
    assertNotNull(previews);
    assertFalse(previews.isEmpty());
    assertTrue(previews.containsKey("DI55") || previews.containsKey("di55"));
    List<String> di55Previews = previews.get("DI55") != null ? previews.get("DI55") : previews.get("di55");
    assertNotNull(di55Previews);
    assertFalse(di55Previews.isEmpty());
    assertTrue(di55Previews.get(0).startsWith("/ITEM_previews/"));
  }

  @Test
  void returnsItemPreviewsForSpecificFolder() throws IOException {
    List<String> di55Previews = controller.getItemPreviewsForFolder("DI55");
    assertNotNull(di55Previews);
    assertFalse(di55Previews.isEmpty());
    assertTrue(di55Previews.get(0).contains("DI55"));

    List<String> missing = controller.getItemPreviewsForFolder("NON_EXISTENT_FOLDER_XYZ");
    assertNotNull(missing);
    assertTrue(missing.isEmpty());
  }
}
