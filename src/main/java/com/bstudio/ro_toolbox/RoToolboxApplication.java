package com.bstudio.ro_toolbox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RoToolboxApplication {

    public static void main(String[] args) {
        // Force SpringApplication to not run in headless mode so AWT/Swing can initialize.
        SpringApplication app = new SpringApplication(RoToolboxApplication.class);
        app.setHeadless(false);
        app.run(args);
    }

}
