# RO Toolbox v0.0.9 – What's New

## ✨ New: Combat Text Manager
- Added a dedicated **Combat Text** manager alongside the existing Loot Models manager
- Uses the new repository: **RO_CombatText_resources**
- Supports:
  - download profiles
  - install a selected profile
  - clear downloaded resources
  - clear installed combat text files
  - open the downloaded or installed folders

## 🧩 Model systems are now independent
- Loot and Combat Text profiles now manage their own manifest files
- Each service uses its own profile tracking instead of reusing the same shared state
- Installing one system no longer marks the other as active
- Unrelated files in the game installation are preserved; only managed subfolders are cleared

## 🛡️ Safer installation behavior
- Install actions are now restricted to profile-managed paths instead of wiping the whole `3ddata/item` folder
- Removes the risk of one model set deleting another model set’s files
- Keeps installation behavior cleaner and safer for mixed setups

## 🧠 Improved backend/data handling
- Added service-specific manifest naming:
  - `manifestLoot.json`
  - `manifestCombatText.json`
- Legacy `manifest.json` files remain readable for compatibility
- Active profile detection is now separated by feature instead of sharing the same installed state

## 🎨 Frontend improvements
- Added the Combat Text UI in the same style as the Loot Models manager
- Both profile systems are visible in the main app without conflicting status entries
- Styling and control layout remain consistent across both managers

## 📥 Installation / Update
Download and install using the latest MSI from Releases.  
If you're already installed, use the in-app updater from the header icon.

---

**Made by BStudio** – [GitHub](https://github.com/MartinBStudio) | [Forum Thread](https://forum.roseonlinegame.com/topic/7761-ro_toolbox-loot-models/#comment-26680)
