package com.bstudio.ro_toolbox.config;

import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class BackendPortFileWriter {

  @EventListener
  public void onWebServerInitialized(WebServerInitializedEvent event) {
    int port = event.getWebServer().getPort();

    // Startup information travels through the private child stdout pipe, never a shared temp file.
    System.out.println("RO_TOOLBOX_BACKEND_PORT=" + port);
  }
}
