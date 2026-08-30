# RO Toolbox

Desktop app for managing ROSE Online profile packs, combat text packs, user interface profiles, and other client customization resources.

## Features

### Loot Models

* Download and update loot profile packs from the resources repository
* Install a selected profile into the chosen ROSE Online installation
* View profile metadata such as name, author, version, and date
* Open author/profile links when available
* Clear installed profiles or downloaded resources from the app

### Combat Text

* Download, install, and manage independent combat text profiles
* Keep combat text resources separate from loot model installations
* Check for upgrades and install selected packs with one click

### User Interface Profiles

* Manage UI profile packs in the same workflow as loot and combat text
* Keep installed UI profiles separate from the game’s other data folders
* Refresh downloaded resources and clear installed files when needed

### Login Manager

* Store and manage multiple ROSE Online game accounts locally
* Each account stores name, email, password, and a custom icon
* Passwords are encrypted in local storage (not plaintext)
* Mark accounts for **Display in quick launch** with a star toggle
* Export accounts to a backup file (encrypted passwords)
* Import previously exported account backups

### Quick Launch

* Quick Launch section shows all accounts marked for quick display
* Click an account icon to launch `trose.exe` with stored login credentials
* Separate **Launch Rose Launcher** action to start `rose-updater.exe`
* Compact, horizontally scrollable account layout for easy access

### Config Editor

* View and edit game TOML files (`rose.toml`, `ignore.toml`)
* Ignore list manager to add/remove ignored entries
* Boolean values manager with search to quickly toggle `true`/`false` settings
* Optional TOML source and parsed preview display

### App UX

* First-run setup modal to select your game folder
* Settings modal to change, clear, or reset the app state
* Auto-detection and validation of the game root using `trose.exe`
* Factory reset option for clearing downloaded profiles, app config, and saved accounts safely
* App remembers the last selected service on next launch
* Auto update checks after reset and other state changes

## Resource Repositories

RO Toolbox uses separate public repositories for community-created resources:

* **Loot Models:** https://github.com/MartinBStudio/RO_LootFilter_resources
* **Combat Text:** https://github.com/MartinBStudio/RO_CombatText_resources
* **UI Themes:** https://github.com/MartinBStudio/RO_UserInterface_resources

The repositories are intentionally public so anyone can create and contribute their own models, themes, or other customizations.

## Contributing Resources

You do **not** need to modify the RO Toolbox application to contribute a resource.

If you have created your own loot model, combat text, UI theme, or other supported customization, you can contribute it directly to the appropriate resource repository.

### How to contribute

1. Open the appropriate resource repository.
2. Create a new folder for your profile/resource following the structure of the existing profiles.
3. Add your resource files.
4. Add or update the profile metadata, including:

   * Profile name
   * Author
   * Version
   * Date
   * Description, if applicable
   * Author/profile URL, if applicable
5. Commit your changes and create a **Pull Request**.
6. Once the Pull Request is reviewed and merged, the resource will become available to RO Toolbox users through the normal resource update process.

### Adding author and profile links

If your profile has a website, forum thread, Discord, GitHub page, or other relevant link, include it in the profile metadata.

RO Toolbox can display available author/profile information and provide links directly from the application, making it easier for users to find the original creator and learn more about the resource.

### Resource guidelines

Please make sure that:

* You have permission to distribute the resources you contribute.
* Your submission follows the structure used by the existing profiles.
* You provide accurate author information.
* You do not include copyrighted assets that you are not allowed to redistribute.
* Resource names and metadata are clear and understandable.
* Updates are made to the existing profile rather than creating unnecessary duplicates.

The goal is to keep the repositories open and community-driven while giving proper credit to the people who create the resources.

## Installation

1. Download the latest release from GitHub Releases.
2. Install and launch the app.
3. On first launch, select the ROSE Online installation folder that contains `trose.exe`.
4. The app will use that folder as the root for profile installs and update checks.

## How to Use

### Basic Setup

1. Open **Settings** and choose the game folder.
2. Ensure the selected folder is the root folder that contains `trose.exe`.

### Install Profile Packs

1. In **Loot Models**, **Combat Text**, or **User Interface**, check for updates or download resources.
2. Select a profile and click **Install**.
3. Restart the game client after installing a new profile.

### Quick Launch with Saved Accounts

1. Open **Login Manager** and click **+** to add a new account.
2. Enter account name, email, password, and select an icon.
3. Toggle **Display in quick launch** to show the account in the quick launch section.
4. Click an account icon in the **Quick Launch** section to launch ROSE with that account's credentials.
5. Alternatively, click **Launch Rose Launcher** to start `rose-updater.exe`.

### Backup and Restore Accounts

1. In **Login Manager**, click **Export** to download a backup file.
2. Choose where to save the backup (passwords are encrypted).
3. To restore, click **Import**, select the backup file, and confirm to replace current accounts.

### Edit Game Config

1. Open **Config Editor**.
2. Select a `.toml` file tab (e.g., `rose.toml` or `ignore.toml`).
3. For `rose.toml`, use **Boolean values manager** to toggle settings.
4. For `ignore.toml`, use **Ignore list manager** to add/remove entries.
5. Click **See files** to view the raw TOML source and parsed preview.
6. Click **Save** to write changes to disk.

## Configuration and Reset Behavior

The app stores its own settings under:

* Windows: `%APPDATA%\RO_Toolbox\config\config.properties`
* Other systems: `~/.ro_toolbox/config/config.properties`

Saved accounts are stored alongside app config:

* Windows: `%APPDATA%\RO_Toolbox\config\accounts.properties`
* Other systems: `~/.ro_toolbox/config/accounts.properties`

The selected game folder is treated as the installed game root; it must contain `trose.exe`.

The app does not modify the real ROSE Online config files under the game's own installation or `AppData\Rednim Games`.

A **Factory reset** clears:

* RO_Toolbox app data and settings
* Downloaded profile resources
* Saved accounts and Login Manager data
* Local mod installation state
* Selected game folder

After reset, you will need to select your game folder again on the next launch.

## Support

For issues or suggestions, use GitHub Issues:

https://github.com/MartinBStudio/RO_Toolbox/issues

## License

RO Toolbox is created by BStudio.
