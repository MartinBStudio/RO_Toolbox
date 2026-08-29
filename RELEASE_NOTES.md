# RO Toolbox – Release Notes

## v0.2.7 – Config Editor Service

### ✨ New service: Config editor
- Added a dedicated **Config editor** service (separate from Texture replacer) with sidebar entry and service routing
- Added backend API at `/api/config-editor` to discover, read, and save:
  - `%APPDATA%\Rednim Games\ROSE Online\config\ignore.toml`
  - `%APPDATA%\Rednim Games\ROSE Online\config\rose.toml`
- Added TOML parsing/validation using `tomlj`; invalid TOML is rejected with line/column error details
- Added action to open the ROSE config folder directly from the UI

### 🧾 Config editor UI
- Added file tabs for `ignore.toml` and `rose.toml` with found/missing status badges
- Added editable TOML source panel with save and reload actions
- Added structured parsed preview panel for easier reading of TOML content

### 📘 How to use guide update
- Updated **How to use** modal to include tabbed guides by service
- Added separate guide tabs for:
  - **Texture replacer**
  - **Config editor**

## v0.2.6 – Profile Version Display

### 🔢 Manifest version visibility
- Added manifest version data to installed and downloadable profile status payloads for Loot, Combat Text, and User Interface services
- Updated the frontend to show installed profile versions and the available version beside downloaded profile entries

## v0.2.5 – User Interface Service

### ✨ New service: User Interface profiles
- Added a full **User Interface** service (backend + frontend), following the same structure as Loot and Combat Text managers
- Added profile download/install/clear/open-folder actions and status integration for `/api/userinterface`
- Connected service to `RO_UserInterface_resources` repository

### 🧭 Profile discovery and dropdown behavior
- User Interface profile dropdown now loads from **subfolders only**
- Dropdown entries require `manifestUi.json`
- Hidden dot-prefixed folders (e.g. `.default`, `.idea`) from profile dropdown/display lists

### 🧹 Clear behavior adjustments
- `clear-downloaded` now preserves `.default` resources
- `clear-installed` now clears only files listed in `.default/FILE_LIST.txt`
- `clear-installed` restores listed files from `.default` back to the selected game folder
- Missing files listed in `FILE_LIST.txt` no longer fail clear-installed; they are skipped

### 🔄 Update check behavior
- User Interface update checks use repository root `manifest.json` (local + remote)
- Improved remote manifest fetch reliability for update detection

## v0.2.4 – Updater Stability Improvements

### 🔄 In-app updater artifact update
- Updated updater metadata generation to prefer the NSIS setup artifact (`*_x64-setup.exe`) for `latest.json`
- Keeps compatibility by falling back to MSI metadata when NSIS artifacts are not present

### 🛑 Backend/JRE shutdown reliability
- Improved desktop app shutdown flow to terminate the bundled backend Java process more reliably
- Added stronger Windows process-tree termination on app exit to prevent lingering JRE processes after closing the app

### 🧩 General stability
- Unified backend stop handling for both explicit updater shutdown and normal app exit paths