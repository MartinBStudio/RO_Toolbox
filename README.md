# RO Toolbox

A desktop application for managing Rose Online game modifications and custom models.

## Features

### 📦 Loot Models Manager
- **Download Loot Profiles** - Browse and download curated loot item model profiles from the online repository
- **Install Profiles** - Apply downloaded profiles to your game files
- **Profile Information** - View author, description, and creation details for each profile
- **Easy Navigation** - Click profile info to visit the author's page

### 🎯 Models Replacer Dashboard
- Quick overview of installed loot item models from the main window
- See which profiles are currently installed at a glance
- Organized table layout for future expansion (future support for weapons, NPCs, etc.)

### ⚙️ Settings
- Configure your game installation folder
- Automatic validation of game folder structure
- Persistent configuration storage

### 📋 Log Viewer
- Real-time logging of all operations
- Visual indicators for downloads, installations, and errors
- Help with troubleshooting

## Installation

1. Download the latest release of RO Toolbox
2. Run the packaged desktop app for your platform (Java is bundled automatically)
3. On first launch, configure your Rose Online game installation folder in Settings

## Getting Started

### 1. Configure Your Game Folder
- Click the ⚙️ Settings button in the main window
- Select your Rose Online game installation directory
- The app will validate the folder structure

### 2. Download Loot Profiles
- Click "📦 Loot Models" button
- Click "Download" to fetch available profiles from the repository
- Browse the available profiles with author information

### 3. Install a Profile
- Select a downloaded profile from the list
- Click "Install" to apply it to your game files
- The installed profile will appear in the main window dashboard

### 4. Manage Profiles
- View currently installed profiles in the Models Replacer table
- Click on profile info to visit the author's page
- Download new profiles to try different configurations

```

## Configuration

Application settings are stored in `~/.ro_toolbox/config.properties`:
- Game installation folder path
- Downloaded profiles cache
- User preferences

## Troubleshooting

### "Select a game installation folder in Settings"
- The app couldn't find a valid game folder
- Go to Settings and select your RO game directory
- Make sure the folder contains the game executable

### Installation fails
- Check that you have write permissions in the game directory
- Ensure sufficient disk space for the profile files
- Review the Log viewer for detailed error messages

### Profiles won't download
- Check your internet connection
- Verify the repository server is online
- Check the Log viewer for connection errors


## Support

For issues, questions, or suggestions, please check the Log viewer for detailed information about any errors encountered.

## License

RO Toolbox is created by BStudio.

---

**Note**: This tool is designed for managing custom loot item model modifications in Rose Online. Use responsibly and ensure all modifications comply with your game server's policies.
