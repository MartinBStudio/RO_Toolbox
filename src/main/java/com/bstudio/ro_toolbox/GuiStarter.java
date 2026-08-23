package com.bstudio.ro_toolbox;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class GuiStarter {

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(ApplicationReadyEvent ev) {
        // Launch GUI on Swing EDT; MainGui.main already schedules on EDT so calling it is fine
        try {
            MainGui.main(new String[0]);
        } catch (Throwable t) {
            // log to stderr as fallback
            t.printStackTrace();
        }
    }
}
