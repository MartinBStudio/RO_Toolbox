package com.bstudio.ro_toolbox.service.app;

import com.bstudio.ro_toolbox.util.AppDataPaths;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AppConfigService {
  private static final String SELECTED_GAME_KEY = "selectedGame";
  private static final String SELECTED_SERVICE_KEY = "selectedService";
  private static final String QUICK_LAUNCH_ONLY_MODE_KEY = "quickLaunchOnlyMode";
  private static final String CONFIG_COMMENT = "RO Toolbox config";
  private static final String USEFUL_STUFF_COLLAPSED_KEY = "usefulStuffCollapsed";
  private static final String IGNORE_CONFIG_WARNINGS_KEY = "ignoreConfigWarnings";
  private static final String BACKUP_PROVIDER_ID_KEY = "backupProviderId";
  private static final String BACKUP_LOCAL_PATH_KEY = "backupLocalPath";

  private final Path configDir;
  private final Path configFile;

  public AppConfigService() {
    this(AppDataPaths.resolveRoToolboxAppDataRoot());
  }

  public AppConfigService(Path appDataRoot) {
    Path root = appDataRoot == null ? AppDataPaths.resolveRoToolboxAppDataRoot() : appDataRoot;
    this.configDir = root.resolve("config");
    this.configFile = this.configDir.resolve("config.properties");
  }

  public Path getSelectedGameBase() {
    String selected = getPropertyQuietly(SELECTED_GAME_KEY);
    if (selected == null || selected.isBlank()) {
      return null;
    }
    Path candidate = Path.of(selected.trim()).toAbsolutePath().normalize();
    return Files.exists(candidate) ? candidate : null;
  }

  public void saveSelectedGameBase(Path base) throws IOException {
    if (base == null) {
      clearSelectedGameBase();
      return;
    }
    setProperty(SELECTED_GAME_KEY, base.toAbsolutePath().normalize().toString());
  }

  public void clearSelectedGameBase() throws IOException {
    removeProperty(SELECTED_GAME_KEY);
  }

  public String getSelectedService() throws IOException {
    String selectedService = getProperty(SELECTED_SERVICE_KEY);
    return (selectedService == null || selectedService.isBlank()) ? null : selectedService.trim();
  }

  public void saveSelectedService(String serviceId) throws IOException {
    if (serviceId == null || serviceId.isBlank()) {
      throw new IllegalArgumentException("Service id is required.");
    }
    setProperty(SELECTED_SERVICE_KEY, serviceId.trim());
  }

  public boolean getQuickLaunchOnlyMode() throws IOException {
    String modeValue = getProperty(QUICK_LAUNCH_ONLY_MODE_KEY);
    return modeValue != null && Boolean.parseBoolean(modeValue.trim());
  }

  public void saveQuickLaunchOnlyMode(boolean enabled) throws IOException {
    setProperty(QUICK_LAUNCH_ONLY_MODE_KEY, String.valueOf(enabled));
  }

  public void clearAppConfig() throws IOException {
    if (Files.exists(configFile)) {
      Files.deleteIfExists(configFile);
      log.info("Deleted app config: {}", configFile.toAbsolutePath());
    }
  }

  public Path getConfigDir() {
    return configDir;
  }

  public synchronized java.util.List<String> getFavoritePackages(String resourceType)
      throws IOException {
    String prefix = favoritePrefix(resourceType);
    return loadProperties().stringPropertyNames().stream()
        .filter(key -> key.startsWith(prefix))
        .map(key -> key.substring(prefix.length()))
        .sorted()
        .toList();
  }

  public synchronized void saveFavoritePackage(String resourceType, String name, boolean favorite)
      throws IOException {
    String prefix = favoritePrefix(resourceType);
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Package name is required.");
    }
    if (favorite) {
      setProperty(prefix + name, "true");
    } else {
      removeProperty(prefix + name);
    }
  }

  private String favoritePrefix(String resourceType) {
    if (resourceType == null
        || !java.util.Set.of("loot", "combatText", "userInterface", "buffIcons", "buffs")
            .contains(resourceType)) {
      throw new IllegalArgumentException("Unknown resource type.");
    }
    return "favorite." + resourceType + ".";
  }

  public boolean getFavoritesOnly(String resourceType) throws IOException {
    favoritePrefix(resourceType);
    return Boolean.parseBoolean(getProperty("favoritesOnly." + resourceType));
  }

  public void saveFavoritesOnly(String resourceType, boolean enabled) throws IOException {
    favoritePrefix(resourceType);
    setProperty("favoritesOnly." + resourceType, String.valueOf(enabled));
  }

  public Path getConfigFile() {
    return configFile;
  }

  private String getPropertyQuietly(String key) {
    try {
      return getProperty(key);
    } catch (IOException | RuntimeException ignored) {
      return null;
    }
  }

  private String getProperty(String key) throws IOException {
    Properties properties = loadProperties();
    return properties.getProperty(key);
  }

  private synchronized void setProperty(String key, String value) throws IOException {
    ensureConfigDir();
    Properties properties = loadProperties();
    properties.setProperty(key, value);
    storeProperties(properties);
  }

  private synchronized void removeProperty(String key) throws IOException {
    ensureConfigDir();
    Properties properties = loadProperties();
    properties.remove(key);
    storeProperties(properties);
  }

  private void ensureConfigDir() throws IOException {
    Files.createDirectories(configDir);
  }

  private Properties loadProperties() throws IOException {
    Properties properties = new Properties();
    if (!Files.exists(configFile)) {
      return properties;
    }
    try (InputStream in = Files.newInputStream(configFile)) {
      properties.load(in);
    }
    return properties;
  }

  private void storeProperties(Properties properties) throws IOException {
    try (OutputStream out = Files.newOutputStream(configFile)) {
      properties.store(out, CONFIG_COMMENT);
    }
  }

  public boolean getUsefulStuffCollapsed() throws IOException {
    return Boolean.parseBoolean(getProperty(USEFUL_STUFF_COLLAPSED_KEY));
  }

  public void saveUsefulStuffCollapsed(boolean collapsed) throws IOException {
    setProperty(USEFUL_STUFF_COLLAPSED_KEY, String.valueOf(collapsed));
  }

  public boolean getIgnoreConfigWarnings() throws IOException {
    return Boolean.parseBoolean(getProperty(IGNORE_CONFIG_WARNINGS_KEY));
  }

  public void saveIgnoreConfigWarnings(boolean enabled) throws IOException {
    setProperty(IGNORE_CONFIG_WARNINGS_KEY, String.valueOf(enabled));
  }

  public String getBackupProviderId() {
    String providerId = getPropertyQuietly(BACKUP_PROVIDER_ID_KEY);
    return providerId == null || providerId.isBlank() ? "auto" : providerId.trim();
  }

  public void saveBackupProviderId(String providerId) throws IOException {
    if (providerId == null || providerId.isBlank()) {
      throw new IllegalArgumentException("Backup provider is required.");
    }
    setProperty(BACKUP_PROVIDER_ID_KEY, providerId.trim());
  }

  public Path getBackupLocalPath() {
    String path = getPropertyQuietly(BACKUP_LOCAL_PATH_KEY);
    if (path == null || path.isBlank()) {
      return null;
    }
    return Path.of(path.trim()).toAbsolutePath().normalize();
  }

  public void saveBackupLocalPath(Path path) throws IOException {
    if (path == null) {
      removeProperty(BACKUP_LOCAL_PATH_KEY);
      return;
    }
    setProperty(BACKUP_LOCAL_PATH_KEY, path.toAbsolutePath().normalize().toString());
  }
}
