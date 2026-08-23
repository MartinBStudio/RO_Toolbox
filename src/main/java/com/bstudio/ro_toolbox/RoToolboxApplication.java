package com.bstudio.ro_toolbox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RoToolboxApplication {

    public static void main(String[] args) {
        // If java.awt.headless was set to true by environment/IDE, and a desktop is available,
        // prefer to allow AWT/Swing to initialize so the GUI opens when running locally.
        String headless = System.getProperty("java.awt.headless");
        if ("true".equalsIgnoreCase(headless)) {
            System.err.println("System property java.awt.headless=true detected; forcing to false to allow GUI startup.");
            System.setProperty("java.awt.headless", "false");
        }

        SpringApplication.run(RoToolboxApplication.class, args);
    }

}
