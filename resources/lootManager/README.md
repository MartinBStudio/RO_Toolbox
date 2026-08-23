RO_LootFilter_resources
=======================

Overview
--------
This repository stores resource folders containing the contents of the `3ddata/item` folder from a Rose Online installation. Each top-level folder is a separate "loot items profile" that adjusts 3D models and item sizes for use with RO_LootManager.

Current profiles
----------------
- original — unmodified items pulled from a Rose Online installation
- poc — proof-of-concept adjustments (modified sizes/models)

Purpose
-------
Use these folders as the source-of-truth for different loot item profiles in RO_LootManager. The manager can read a chosen profile folder to apply model files and size adjustments when generating loot displays or patches.

Repository layout
-----------------
Root/
- original/    (contents of 3ddata/item from installation)
- poc/         (modified items for testing)
- .idea/       (IDE metadata)

Usage notes
-----------
- Each profile folder should mirror the directory structure of `poc`.
- Keep filenames and subfolders consistent with the game installation so RO_LootManager can map items correctly.
- When adding a new profile, create a new top-level folder (profile name) and place the copied/adjusted files inside.

Contributing
------------
- Add or update profiles by creating a new folder or editing an existing one.
- Document any size/model tweaks in a small text file (e.g., `CHANGES.txt`) inside the profile folder.

Support
-------
For questions about integration with RO_LootManager, contact the repository owner or the project maintainers.
