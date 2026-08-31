# RO Toolbox – Release Notes

## v0.3.3 – Bug Fix

### 🐛 Bug Fixes
- **UI resources update not detected**: Fixed the user interface resource update check always reporting "up to date" even when a newer version was available in the remote repository. The update check was looking for the wrong manifest file.

---

## v0.3.2 – Launcher Fix & Preview Support

### 🐛 Bug Fixes
- **Launcher not opening from the header**: Fixed the Windows launch logic so the app now starts `rose-updater.exe` reliably from the selected game folder.
- **Resource preview support**: Added support for profile resource folders containing a hidden `.preview` directory, while still excluding those files from being copied into the game installation.

### ✨ Improvements
- Downloaded profile metadata can now include preview image thumbnails for loot, combat text, and UI profiles.
- Preview images render in the profile card and can be opened fullscreen when clicked.

---

## v0.3.1 – Bug Fix

### 🐛 Bug Fixes
- **User Interface restore on clear/factory reset**: Clearing or factory resetting the installed UI profile now correctly restores the original game UI files from the `.default` folder and removes the installed profile marker. Previously, the app still showed the UI as installed and the original files were not copied back, leaving the game UI in a broken state.

---

## v0.3.0 – Login Manager & Quick Launch

### ✨ New Features
- **Login Manager**: Store and manage multiple game accounts locally with encrypted passwords
- **Quick Launch**: Start the game instantly with saved account credentials
- **Account Backup/Restore**: Export and import your accounts for safekeeping
- **App remembers your last selected service** on next launch

### 🔧 Improvements
- Factory reset now clears saved accounts
- Better app stability and cleaner UI organization
- How to use guide updated with Login Manager and Quick Launch instructions
- ROSE Online themed visual design with dark backgrounds and red accents
- Official ROSE logo integrated into app header
- Quick Launch buttons positioned on header for easy access
