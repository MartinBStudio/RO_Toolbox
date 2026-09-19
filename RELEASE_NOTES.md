# RO Toolbox – Release Notes

## v1.0.1 – Loot Model Scale Tools

This update improves loot model management with built-in model scale controls and a cleaner manage packages table.

### Loot Manager

* **Model scale display**: The manage installed packages modal now shows each loot model size as a percentage difference compared to the original downloaded package.
* **Scale controls**: Added `-`, `R`, and `+` actions for decreasing, resetting, or increasing managed loot model size directly from the table.
* **Disabled folder support**: Size scanning and scale actions now work for disabled managed folders too.
* **Safer ZMS editing**: Scaling updates ZMS vertex positions and bounds while preserving model proportions.

### Interface

* **Cleaner previews**: The manage modal now shows only the first preview image per folder.
* **Compact table layout**: Reduced the preview column width and kept scale actions grouped under the Size column.

## 🎉 v1.0.0 – First Stable Release

RO Toolbox has reached **v1.0.0**! This release focuses on stability, application architecture, and final polish.

### 🔗 Application

* **Dynamic backend port**: The backend now automatically uses an available port, preventing conflicts with other applications.
* **Bundled Java runtime**: Java is included with RO Toolbox and no longer needs to be installed separately.
* **Improved installer**: Updated installer visuals for a more polished installation experience.
* **Application stability**: Various fixes and improvements made in preparation for the first stable release.
