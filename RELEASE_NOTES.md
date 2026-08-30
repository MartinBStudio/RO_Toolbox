# RO Toolbox – Release Notes

## v0.3.0 – Login Manager, Quick Launch, and major UX upgrades

### 🔐 Login Manager (new service)
- Added a full **Login Manager** service with local account CRUD
- Accounts include: **name**, **email**, **password**, **displayInQuick**, and **icon**
- Passwords are stored encrypted locally (not plaintext)
- Added account-level quick toggle (star) for **Display in quick launch**
- Delete actions now use the shared confirmation modal

### 🚀 Quick Launch improvements
- Quick Launch now shows saved accounts marked for quick display
- Clicking a quick account launches `trose.exe` with stored login parameters
- Added a separate **Launch Rose Launcher** action for `rose-updater.exe`
- Updated launch/open process handling on Windows for more reliable external app start behavior

### 📦 Login backup and restore
- Added **Export** and **Import** buttons in Login Manager
- Export now uses a native **Save As** flow (choose destination)
- Exported password values are encrypted (`enc:`), not plaintext
- Import supports restoring exported account backups back into local storage

### 🧹 Factory reset and state consistency
- Factory reset now also clears saved login accounts (`accounts.properties`)
- Reset flow keeps app-managed cleanup behavior and avoids `.default` dependency errors
- After reset, UI account state is refreshed immediately so stale entries are not shown

### 🧭 App UX and layout polish
- App now remembers the **last selected service** between launches
- Sidebar service icons updated to match service purpose
- Quick Launch visuals refined: compact account row, centered layout, horizontal scrolling for many accounts
- Footer behavior improved to stay anchored with scrollable content area

### ⚙️ Config Editor refinements
- Added clearer service heading/description
- Added **See files / Hide files** behavior so TOML source and parsed preview are optional
- Ignore manager and boolean manager remain visible by default
- Improved fullscreen alignment for parsed preview vs TOML source
