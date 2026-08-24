# Changelog

All notable changes to this project will be documented in this file.

## [0.0.3] - 2026-08-24

### Added
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

### Improved
- **User Experience**: Quickly see what models are installed and who created them
- **Application Performance**: Lighter startup and faster component loading
- **Future Scalability**: New table-based layout makes it easy to add more model types (weapons, NPCs, etc.) later

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
