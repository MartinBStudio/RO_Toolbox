# Changelog

All notable changes to this project will be documented in this file.

## [0.0.3] - 2026-08-24

### Changed
- **Architecture**: Eliminated `AppInfo` class, merged version management into `RoToolboxApplication`
- **Version Management**: Simplified to single source of truth in `application.properties` with `app.version` property
- **Build Configuration**: Gradle now reads version from `application.properties` for JAR naming
- **Spring Configuration**: Removed XML-based configuration file (`application-context.xml`)
  - Converted to annotation-based `@Configuration` with `@ComponentScan` and `@PropertySource`
  - Uses `AnnotationConfigApplicationContext` for lightweight context initialization
- **Dependency Injection**: All components use constructor injection with `@Autowired`

### Removed
- Deleted `AppInfo.java` class
- Removed `application-context.xml` XML configuration file
- Removed Spring Boot's `@SpringBootApplication` in favor of pure Spring `@Configuration`

### Improved
- **Code Cleanliness**: Reduced boilerplate by consolidating version management
- **Build Process**: Single source of truth eliminates version sync issues
- **Application Startup**: Pure Spring context is more lightweight and suitable for Swing desktop app
- **Test Configuration**: Updated to use `@SpringJUnitConfig` instead of `@SpringBootTest`

## [0.0.2] - 2026-08-24

### Added
- **Manifest Fields**: Added `url` and `author` fields to loot profile manifests
- **Created Date**: Added `createdAt` field to track when profiles were created
- **Profile Details Modal**: Enhanced patch selection dialog to display profile metadata (author, description, created date)
- **Visit Button**: Added clickable "Visit" button to open profile URLs in browser
- **Profile Lists**: Added scrollable list to view all downloaded loot profiles in Resources section
- **Installed Profile List**: Added display of currently installed loot profile in Game Files section
- **Improved Button Labels**: Renamed main buttons for clarity:
  - "Pull resources" → "⬇ Download Loot Profiles"
  - "Patch loot models" → "📦 Install Loot Profile"

### Changed
- **Section Names**: Renamed UI sections for better clarity:
  - "Resources" → "📥 Downloaded Models"
  - "Game Files" → "📦 Installed Models"
- **Window Height**: Increased from 290px to 520px to accommodate profile lists
- **Profile Selection**: Removed description from dropdown labels, now shows only folder name for cleaner UI
- **Logging**: Enhanced log messages with emoji icons for better visibility:
  - "⬇ Downloading..." for downloads
  - "📦 Installing..." for installations
  - "✓" for success and "✗" for errors

### Improved
- **User Experience**: Better visual organization with dedicated profile lists
- **Information Display**: More detailed metadata display when selecting profiles
- **Log Clarity**: More descriptive logging with visual indicators

## [0.0.1] - Initial Release

### Features
- Loot Manager service for downloading and managing loot filter profiles
- Profile patching to game files
- Game installation folder configuration
- Resource management (download, browse, clear)
- Log viewer for debugging
