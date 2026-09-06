# RO Toolbox – Release Notes

## v0.4.2 – Stability and package browsing polish

### ✨ Improvements
- Improved long-session stability to reduce cases where the app could turn into a white screen after running for a while.
- Added a safer fallback screen with a quick **Reload app** action if the UI crashes unexpectedly.
- Package installation modals are now easier to use on smaller screens:
  - about **50% app width**
  - about **80% app height**
- Updated the package modal opener to a clearer blue **Browse packages** icon instead of an accordion-style arrow.

## v0.4.1 – Buff icons and UI improvements

### ✨ Improvements
- Added a new Buff icons service with download, install, clear, and open-folder actions.
- Buff icons now uses `manifestBuffIcons.json` for profile metadata and keeps `manifest.json` for resource versioning.
- Manifest timestamps are now shown in a human-readable format across all package panels.
- Closing the app now shows a confirmation modal before exit.
