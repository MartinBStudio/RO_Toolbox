package com.bstudio.ro_toolbox;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

@SpringBootApplication
@Component
public class RoToolboxApplication {
    
    @Value("${app.version:dev}")
    private String version;

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(RoToolboxApplication.class);
        app.setHeadless(false);
        app.run(args);
    }

    public String getVersion() {
        return version;
    }

}
