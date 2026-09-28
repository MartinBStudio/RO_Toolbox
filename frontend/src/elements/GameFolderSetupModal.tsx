import { open as openDialog } from "@tauri-apps/plugin-dialog";
import { useEffect, useState } from "react";
import {
  getAppConfigBackupStatus,
  restoreAppConfigBackup,
  saveGameFolder,
  type AppConfigBackupStatus
} from "../backendConnector/api.ts";

type GameFolderSetupModalProps = {
  onBusyChange: (busy: boolean, message?: string) => void;
  onStatusRefresh: () => Promise<void>;
  onMessage: (message: string) => void;
  loading: boolean;
};

export function GameFolderSetupModal({
  onBusyChange,
  onStatusRefresh,
  onMessage,
  loading
}: GameFolderSetupModalProps) {
  const [backupStatus, setBackupStatus] = useState<AppConfigBackupStatus | null>(null);
  const [selectedBackupName, setSelectedBackupName] = useState("");

  useEffect(() => {
    void loadBackupStatus();
  }, []);

  useEffect(() => {
    const backups = backupStatus?.backups ?? [];
    if (backups.length === 0) {
      setSelectedBackupName("");
      return;
    }
    if (!backups.some((backup) => backup.name === selectedBackupName)) {
      setSelectedBackupName(backups[0].name);
    }
  }, [backupStatus?.backups, selectedBackupName]);

  function toErrorMessage(err: unknown, fallback: string) {
    if (err instanceof Error && err.message) return err.message;
    if (typeof err === "string" && err.trim()) return err;
    if (err && typeof err === "object" && "message" in err && typeof err.message === "string") return err.message;
    return fallback;
  }

  async function loadBackupStatus() {
    try {
      setBackupStatus(await getAppConfigBackupStatus());
    } catch {
      setBackupStatus(null);
    }
  }

  async function saveFolder(path: string) {
    const trimmedPath = path.trim();
    if (!trimmedPath) return;
    onBusyChange(true);
    try {
      const result = await saveGameFolder(trimmedPath, false);
      await onStatusRefresh();
      onMessage(
        result.containsExpectedItemFolder
          ? "Game folder saved."
          : "Game folder saved. Some mod folders may need to be created in this installation."
      );
    } catch (err) {
      const text = toErrorMessage(err, "Request failed.");
      if (text.includes("trose.exe") || text.includes("not valid")) {
        onMessage("The selected folder is not valid. It must contain trose.exe.");
      } else {
        onMessage(text);
      }
    } finally {
      onBusyChange(false);
    }
  }

  async function onBrowseFolder() {
    try {
      const selected = await openDialog({ directory: true, multiple: false });
      if (!selected || Array.isArray(selected)) return;
      await saveFolder(selected);
    } catch (err) {
      onMessage(toErrorMessage(err, "Folder selection failed."));
    }
  }

  async function onRestoreBackup() {
    if (!selectedBackupName) {
      onMessage("Choose a backup to restore.");
      return;
    }

    onBusyChange(true, "Restoring RO Toolbox config...");
    try {
      await restoreAppConfigBackup(selectedBackupName);
      await onStatusRefresh();
      onMessage("RO Toolbox config restored.");
    } catch (err) {
      onMessage(toErrorMessage(err, "Failed to restore RO Toolbox config."));
    } finally {
      onBusyChange(false);
    }
  }

  const backups = backupStatus?.backups ?? [];
  const hasBackups = backups.length > 0;

  return (
    <div className="modalBackdrop">
      <section className="card modalCard setupModal" onClick={(e) => e.stopPropagation()}>
        <div className="setupModalIcon">🎮</div>
        <h2 className="setupModalTitle">Select your game folder</h2>
        <p className="setupModalDesc">
          RO Toolbox needs to know where your ROSE Online is installed.
          Select the game folder that contains <code>trose.exe</code>.
        </p>
        <div className="modalActions">
          <button className="buttonStrong" disabled={loading} onClick={onBrowseFolder}>
            📂 Browse…
          </button>
        </div>
        {hasBackups ? (
          <div className="setupBackupRestore">
            <p className="settingsSectionLabel">Restore from backup</p>
            <p className="setupBackupRestoreText">
              Found {backups.length} RO Toolbox config backup{backups.length === 1 ? "" : "s"} in {backupStatus?.providerName ?? "backup storage"}.
            </p>
            <div className="setupBackupRestoreControls">
              <select
                value={selectedBackupName}
                disabled={loading}
                onChange={(event) => setSelectedBackupName(event.target.value)}
                aria-label="RO Toolbox config backup"
              >
                {backups.map((backup) => (
                  <option key={backup.name} value={backup.name}>
                    {backup.name}
                  </option>
                ))}
              </select>
              <button className="buttonSubtle" disabled={loading || !selectedBackupName} onClick={onRestoreBackup}>
                Restore
              </button>
            </div>
          </div>
        ) : null}
      </section>
    </div>
  );
}
