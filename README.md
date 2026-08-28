# RO Toolbox

Desktop app for managing ROSE Online texture replacer profiles.

## Features

### Loot Models
- Download and update profiles from the resources repository
- Install a selected profile into your game folder
- See profile details (name, author, date, description)
- Open author/profile links when available

### Combat Text
- Separate profile system from Loot Models
- Download, install, and clear combat text profiles independently
- Same profile card/details UX as Loot Models

### App UX
- First-run setup modal to select your game folder
- Settings modal to change or clear folder later
- In-app guide modal (**?**) with quick usage steps
- Auto-check for app updates and resource updates

## Installation

1. Download the latest release from GitHub Releases.
2. Install and launch the app.
3. On first launch, select your ROSE Online installation folder.

## How to Use

1. Open **Settings** (⚙) and set your game folder.
2. In **Loot Models** or **Combat Text**, use **⟳** to check updates or **↓** to download resources.
3. Expand the section, choose a profile, and click **Install**.
4. Restart your game client after installing a new profile.

## Configuration

The selected game folder is stored in app config at:

- Windows: `%APPDATA%\RO_Toolbox\config\config.properties`
- Other systems: `~/.ro_toolbox/config/config.properties`

## Support

For issues or suggestions, use GitHub Issues:

- https://github.com/MartinBStudio/RO_Toolbox/issues

## License

RO Toolbox is created by BStudio.
