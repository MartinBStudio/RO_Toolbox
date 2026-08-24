package com.bstudio.ro_toolbox;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import com.bstudio.ro_toolbox.service.mainMenu.MainMenu;
import com.bstudio.ro_toolbox.utils.UiTheme;

import javax.swing.*;

@Configuration
@ComponentScan(basePackages = "com.bstudio.ro_toolbox")
@PropertySource("classpath:application.properties")
@Component
public class RoToolboxApplication {
    
    @Value("${app.version:dev}")
    private String version;

    public static void main(String[] args) {
        UiTheme.install();
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(RoToolboxApplication.class);
        MainMenu launcher = context.getBean(MainMenu.class);
        SwingUtilities.invokeLater(launcher::createAndShow);
    }

    public String getVersion() {
        return version;
    }

}
