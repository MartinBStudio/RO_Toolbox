# Changelog

All notable changes to this project will be documented in this file.

## [0.0.4] - 2026-08-24

### Added
- **Automatic Update Check**: App now checks the configured GitHub release and compares it with the current version
- **Self-Update Flow**: When an update is available, the app can download the new JAR and restart itself automatically
- **Main Window Dashboard**: New dashboard showing all available model types with installed profiles
- **Models Replacer Table**: Clean table view displaying:
  - Load button for each model type
  - Currently installed profile information (name, author, description)
  - Direct link to visit profile author's page
- **Profile Information Display**: Automatically reads and displays installed profile details from game folder

### Changed
- **Main Window Layout**: Redesigned to show installed models at a glance without opening Loot Manager
- **Application Startup**: Optimized startup process for better performance
- **Dependency Management**: Streamlined how components are initialized
- **Settings Dialog**: Added a clear-folder option for removing the selected Rose installation path

### Fixed
- **Game Folder Persistence**: Fixed the issue where selecting a Rose installation folder appeared to save but did not actually persist the config on end-user machines
- **Config File Creation**: Ensured the app creates and updates the config file correctly when the folder is selected or cleared

### Improved
- **User Experience**: Quickly see what models are installed and who created them
- **Application Performance**: Lighter startup and faster component loading
- **Future Scalability**: New table-based layout makes it easy to add more model types (weapons, NPCs, etc.) later

