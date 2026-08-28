# RO Toolbox – Release Notes

---

## v0.1.9 – Resource Updates, UI Refresh & Startup Improvements

### 🔄 Automatic resource update checks
- Both **Loot Models** and **Combat Text** services now check for updates on startup by fetching `manifest.json` from their remote repositories
- A **download button** (↓, highlighted green) appears in the service header when a newer version of the resources is available
- A **refresh button** (⟳) appears when resources are already up to date — click it to re-check manually
- Version comparison uses the `version` field in the repository root `manifest.json`

### 🎮 First-launch setup modal
- On first run (or when no game folder is configured), a **setup modal** is shown that forces the user to select the ROSE Online installation folder before using the app
- The modal cannot be dismissed — the folder must be set to continue
- Validates that `3ddata` exists in the selected path, with an option to save anyway

### 🗂️ Improved profile install section
- Profile **select dropdown** now shows the human-readable profile name instead of the folder ID
- Selected profile is shown as a **profile card** with: name, author, creation date, description, and an optional 🔗 link to the profile page
- Clean card layout replaces the old plain text display

### 🧭 Smarter accordion behavior
- The accordion expand button (▾) is now **disabled** when no profiles are downloaded, with a tooltip: *"Download profiles first"*
- The "1) Download" section has been removed from inside the accordion — download is handled from the header button
- Download, open-folder, and clear buttons are still accessible from the header, styled as subtle debug-level controls with a visual separator before the primary actions

### 🎨 UI polish
- Secondary (debug) buttons — open downloaded, clear downloaded, open installed, clear installed — are smaller and dimmed (45% opacity, full opacity on hover)
- A thin vertical separator clearly divides debug buttons from the primary download and expand buttons
- Improved **Settings modal**: shows only the game folder path in a monospace display, Clear button only visible when a folder is set, styled section label
- Profile card uses a dark bordered style consistent with the rest of the UI

### 🐛 Startup stability
- Fixed white flash on app launch by setting dark background directly in `index.html` before any JS or CSS loads
- The Tauri backend spawn is now **fault-tolerant**: if `javaw.exe` or the JAR cannot be started (e.g. Windows Defender scanning files on fresh install/update), the app no longer crashes — the frontend will prompt to restart instead
- On first launch after an update, the app now stays open instead of silently closing

## 📥 Installation / Update
Download and install using the latest MSI from Releases.  
If you're already installed, use the in-app updater from the header icon.

---

**Made by BStudio** – [GitHub](https://github.com/MartinBStudio) | [Forum Thread](https://forum.roseonlinegame.com/topic/7761-ro_toolbox-loot-models/#comment-26680)

---

## v0.0.9 – Combat Text Manager

### ✨ New: Combat Text Manager
- Added a dedicated **Combat Text** manager alongside the existing Loot Models manager
- Uses the new repository: **RO_CombatText_resources**
- Supports: download profiles, install a selected profile, clear downloaded resources, clear installed combat text files, open the downloaded or installed folders

### 🧩 Model systems are now independent
- Loot and Combat Text profiles now manage their own manifest files (`manifestLoot.json`, `manifestCombatText.json`)
- Legacy `manifest.json` files remain readable for compatibility
- Installing one system no longer marks the other as active

### 🛡️ Safer installation behavior
- Install actions are restricted to profile-managed paths — unrelated files in `3ddata/item` are preserved

### 📥 Installation / Update
Download and install using the latest MSI from Releases.  
If you're already installed, use the in-app updater from the header icon.

---

**Made by BStudio** – [GitHub](https://github.com/MartinBStudio) | [Forum Thread](https://forum.roseonlinegame.com/topic/7761-ro_toolbox-loot-models/#comment-26680)
