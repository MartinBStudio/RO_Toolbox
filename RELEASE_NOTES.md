# RO Toolbox – Release Notes

## v0.2.9 – Game folder validation, reset safety, debug controls, and release notes

### 🔧 Game folder validation
- Fixed game folder selection to require the actual ROSE install root containing `trose.exe`
- Corrected nested install handling such as `D:\Games\ROSE Online\ROSE Online`
- Ensured the selected game root is saved consistently across Loot, Combat Text, and User Interface managers
- Updated folder validation and UX messages to reflect the real root requirement instead of outdated `3ddata` assumptions

### 🧹 Factory reset and app safety
- Added a reusable confirmation modal for destructive actions
- Added a red **Factory reset** action under Settings
- Reset now clears RO_Toolbox app data, downloaded resources, and local mod state without touching the real ROSE config files in the game install / `AppData\Rednim Games`
- Removed the `.default` folder as the final reset cleanup step
- After reset, the app automatically refreshes profile state and re-checks updates without needing a manual click

### 🧪 Debug controls and hidden admin actions
- Debug mode now hides advanced folder-management actions such as browse/clear downloaded and browse/clear installed actions
- Kept the debug toggle available from the settings panel for power users and troubleshooting

### ℹ️ Release notes in-app
- Added a **What’s new** link in the footer that opens the release notes in an in-app modal
- Release notes are read from the project `RELEASE_NOTES.md` file so updates stay easy to maintain

### ▶️ Quick launch and UX polish
- Added a **Quick launch** section above the footer
- Added **Play** to start `rose-updater.exe` from the selected game folder
- Improved install-state UX and cleaned up loading/update messaging for a sharper experience

### ⚙️ Config editor enhancements
- Added dedicated ignore-list management for `ignore.toml`
- Added a **Boolean values manager** for `rose.toml` to quickly toggle all `true/false` values
- Added search in the boolean manager to filter by section path, key, or value

