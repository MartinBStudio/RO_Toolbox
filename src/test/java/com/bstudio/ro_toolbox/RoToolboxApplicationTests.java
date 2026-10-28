package com.bstudio.ro_toolbox;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(RoToolboxApplication.class)
@org.springframework.test.context.TestPropertySource(
    properties =
        "RO_TOOLBOX_BACKEND_TOKEN=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
class RoToolboxApplicationTests {

  @Test
  void contextLoads() {}
}
