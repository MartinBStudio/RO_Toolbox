# RO Toolbox

Desktop app for managing ROSE Online profile packs, combat text packs, and user interface profiles.

## Features

### Loot Models
- Download and update loot profile packs from the resources repository
- Install a selected profile into the chosen ROSE Online installation
- View profile metadata such as name, author, version, and date
- Open author/profile links when available
- Clear installed profiles or downloaded resources from the app

### Combat Text
- Download, install, and manage independent combat text profiles
- Keep combat text resources separate from loot model installations
- Check for upgrades and install selected packs with one click

### User Interface Profiles
- Manage UI profile packs in the same workflow as loot and combat text
- Keep installed UI profiles separate from the game’s other data folders
- Refresh downloaded resources and clear installed files when needed

### App UX
- First-run setup modal to select your game folder
- Settings modal to change, clear, or reset the app state
- Auto-detection and validation of the game root using `trose.exe`
- Factory reset option for clearing downloaded profiles and app config safely
- Auto update checks after reset and other state changes

## Installation

1. Download the latest release from GitHub Releases.
2. Install and launch the app.
3. On first launch, select the ROSE Online installation folder that contains `trose.exe`.
4. The app will use that folder as the root for profile installs and update checks.

## How to Use

1. Open **Settings** and choose the game folder.
2. Ensure the selected folder is the root folder that contains `trose.exe`.
3. In **Loot Models**, **Combat Text**, or **User Interface**, check for updates or download resources.
4. Select a profile and click **Install**.
5. Restart the game client after installing a new profile.

## Configuration and reset behavior

The app stores its own settings under:

- Windows: `%APPDATA%\RO_Toolbox\config\config.properties`
- Other systems: `~/.ro_toolbox/config/config.properties`

The selected game folder is treated as the installed game root; it must contain `trose.exe`.

The app does not modify the real ROSE Online config files under the game’s own installation or `AppData\Rednim Games`. A **Factory reset** clears RO_Toolbox app data, downloaded resources, and local mod state, then removes the `.default` folder as the final cleanup step.

## Support

For issues or suggestions, use GitHub Issues:

- https://github.com/MartinBStudio/RO_Toolbox/issues

## License

RO Toolbox is created by BStudio.
