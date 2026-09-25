# RO Toolbox – Release Notes


## v1.0.5 – Bug fix

This update improves uninstall behavior for resource managers.

### Resource Managers

* **Uninstall resource fix**: Fixed an issue where uninstalling a resource would sometimes do not delete all files.



## v1.0.4 – Resource Update Cleanup

This update improves package downloads, package browsing, and cleanup behavior for resource managers.

### Resource Managers

* **Clean resource updates**: Downloading updated resources now replaces the local downloaded resource folder instead of merging over old files, preventing stale packages or removed files from lingering.
* **Safer update extraction**: Resource archives are extracted into a temporary folder first, then moved into place only after extraction succeeds.
* **Consistent package cleanup**: File-list based packages now use `.pack/FILE_LIST.txt` consistently when restoring or deleting managed files.

### Package Browser

* **Recommended loot package**: `Farming meta` now appears in a top `Recommended` group and is selected by default when available.
* **Recommended combat text package**: `No combat text` now appears in a top `Recommended` group and is selected by default when available.
* **Better empty-preview layout**: Package cards without preview images no longer stretch their sections inside the browser modal.

## v1.0.3 – Loot Manager Modal Polish

This update refines the loot manage modal and fixes packaged-app loading for loot helper data.

### Loot Manager

* **One-click scale limits**: Added `Min` and `Max` actions for quickly setting loot models to about `0.04x` size or up to `11x` size.
* **Friendlier size display**: Model size now shows as a multiplier such as `1x`, `0.04x`, or `1.35x`, with the percentage difference available on hover.
* **Compact scale controls**: The scale actions now appear as a single segmented control for a cleaner table layout.
* **Better modal scrolling**: The modal header and footer stay visible while only the folder table scrolls, with sticky table headers.
* **More table space**: Save and cancel actions were tightened so the table can use more of the modal height.

### Fixes

* **Installed app previews**: Loot dictionary and item preview loading now uses the active backend port, so previews and labels work when the packaged app starts the backend on a random free port.

## v1.0.2 – Loot Model Scale Tools

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
