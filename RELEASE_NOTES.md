# RO Toolbox – Release Notes

## v0.2.8 – Config UX, Quick Launch, and Install-State Improvements

### ⚙️ Config editor enhancements
- Added dedicated ignore-list management for `ignore.toml`:
- Added **Boolean values manager** for `rose.toml` to quickly toggle all `true/false` values
- Added search in boolean manager (filters by section path, key, or value)

### ▶️ Quick launch
- Added a new **Quick launch** section above the footer
- Added **Play** button to start `rose-updater.exe` from the currently selected game folder

### 🧩 Profile install-state UX
- Improved profile cards for Loot, Combat Text, and User Interface:
- Updated collapsed accordion header metadata order to:
  - profile name
  - author
  - version (last, visually less dominant)

### 🪄 Loading/update UI cleanup
- Removed progress bar from loading overlay modal
- Removed percentage text from “Downloading latest release...” status updates

### 🔧 Game folder and reset reliability
- Fixed game folder selection to validate the actual ROSE installation root by requiring `trose.exe` in the selected folder
- Corrected root resolution for nested installs such as `D:\Games\ROSE Online\ROSE Online`
- Synced the selected game folder across Loot, Combat Text, and User Interface managers so installs and update checks behave consistently
- Improved the Factory Reset flow with a reusable confirmation modal and a safer cleanup path that clears app state without touching the real ROSE game config files

