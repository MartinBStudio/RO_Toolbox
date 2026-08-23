package com.bstudio.ro_toolbox;

import java.io.InputStream;
import java.util.Properties;

public final class AppInfo {
    private static final String DEFAULT_VERSION = "dev";

    private AppInfo() {
    }

    public static String getVersion() {
        String version = System.getProperty("app.version");
        if (version == null || version.isBlank()) {
            version = readVersionFromProperties();
        }
        if (version == null || version.isBlank()) {
            Package pkg = AppInfo.class.getPackage();
            if (pkg != null && pkg.getImplementationVersion() != null) {
                version = pkg.getImplementationVersion();
            }
        }
        if (version == null || version.isBlank()) {
            version = DEFAULT_VERSION;
        }
        return version;
    }

    private static String readVersionFromProperties() {
        try (InputStream in = AppInfo.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in == null) return null;
            Properties props = new Properties();
            props.load(in);
            String value = props.getProperty("app.version");
            return (value == null || value.isBlank()) ? null : value.trim();
        } catch (Exception ignored) {
            return null;
        }
    }
}
