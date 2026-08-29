# RO Toolbox – Release Notes

## v0.2.4 – Updater Stability Improvements

### 🔄 In-app updater artifact update
- Updated updater metadata generation to prefer the NSIS setup artifact (`*_x64-setup.exe`) for `latest.json`
- Keeps compatibility by falling back to MSI metadata when NSIS artifacts are not present

### 🛑 Backend/JRE shutdown reliability
- Improved desktop app shutdown flow to terminate the bundled backend Java process more reliably
- Added stronger Windows process-tree termination on app exit to prevent lingering JRE processes after closing the app

### 🧩 General stability
- Unified backend stop handling for both explicit updater shutdown and normal app exit paths