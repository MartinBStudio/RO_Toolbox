package com.bstudio.ro_toolbox.gui;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.swing.SwingUtilities;

@Component
public class GuiStarter {

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(ApplicationReadyEvent ev) {
        // Always start GUI when Spring Boot is up (spring.application.setHeadless(false) is applied).
        try {
            SwingUtilities.invokeLater(() -> {
                try {
                    // Launch a small services launcher window; it will open specific service GUIs when buttons are pressed
                    ServicesLauncher.main(new String[0]);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            });
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
