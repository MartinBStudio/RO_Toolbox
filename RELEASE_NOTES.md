# RO Toolbox – Release Notes

## v1.1.0 - Backup Flow Polish

This update refines the backup experience so creating, restoring, and managing backups is clearer and more reliable.

### Backups

* **One backup folder per timestamp**: Each backup now creates a single timestamped folder that contains all backed-up sections.
* **Full backup restore**: Restoring a backup now restores all available sections together, including accounts, ROSE config, and RO Toolbox config.
* **Custom backup folder**: Choose exactly where backups are stored. RO Toolbox remembers the path and creates a RO Toolbox backups folder inside it.
* **Initial setup restore**: After a factory reset, the setup screen can load an existing backup folder and restore from it.
* **Cleaner backup list**: Backups now show included sections as badges, use one restore button, and provide clearer open-folder and delete actions.
* **Better refresh behavior**: Restored accounts now appear immediately in Quick Launch and Login Manager.
* **Backup warnings**: The Backups service shows a warning when no backups exist or when the newest backup is older than 30 days.





## v1.0.9 - ROSE Data Backup

This update adds support for backing up important ROSE Online user data, including game configuration and saved accounts.

### Backup

* **ROSE Online config backup**: RO Toolbox can now back up your ROSE Online configuration data.
* **Account backup**: Saved ROSE Online accounts can now be included in the backup.
* **Backup provider selection**: Choose where your backup data is stored.

## v1.0.8 - Footer Link Refresh

This update refreshes the app footer links.

### App

* **RO Toolbox website link**: The footer now links to the RO Toolbox website.
* **Official ROSE Online link**: Replaced the forum thread link with the official ROSE Online website.
* **Startup close option**: If initial loading takes too long, the startup screen now shows a close button so the app can be exited cleanly.
* **Backend cleanup**: The bundled Java backend is now tied to the app process on Windows, helping prevent OpenJDK from staying open after RO Toolbox is closed or interrupted.

