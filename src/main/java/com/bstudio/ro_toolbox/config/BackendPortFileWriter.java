package com.bstudio.ro_toolbox.config;

import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class BackendPortFileWriter {

    @EventListener
    public void onWebServerInitialized(WebServerInitializedEvent event) {
        int port = event.getWebServer().getPort();

        try {
            Path path = Paths.get(
                    System.getProperty("java.io.tmpdir"),
                    "ro_toolbox_backend_port"
            );

            Files.writeString(path, String.valueOf(port));

            System.out.println(
                    "RO_TOOLBOX_BACKEND_PORT=" + port
            );
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to write backend port",
                    e
            );
        }
    }
}