package com.bstudio.ro_toolbox;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;

@Component
public class GuiStarter {

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(ApplicationReadyEvent ev) {
        // Allow forcing GUI even if GraphicsEnvironment reports headless:
        // - System property: -Dro.gui.force=true
        // - Environment variable: FORCE_GUI=true
        String forceProp = System.getProperty("ro.gui.force");
        String forceEnv = System.getenv("FORCE_GUI");
        boolean forceGui = "true".equalsIgnoreCase(forceProp) || "true".equalsIgnoreCase(forceEnv);

        if (GraphicsEnvironment.isHeadless()) {
            if (!forceGui) {
                System.err.println("Headless environment detected — GUI will not be started.");
                return;
            }
            // Force GUI: try to disable headless mode
            System.err.println("Headless environment detected but FORCE GUI requested — attempting to start GUI.");
            System.setProperty("java.awt.headless", "false");
        }

        try {
            SwingUtilities.invokeLater(() -> {
                try {
                    MainGui.main(new String[0]);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            });
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
