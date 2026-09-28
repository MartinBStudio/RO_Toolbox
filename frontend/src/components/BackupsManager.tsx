import { useEffect, useState } from "react";
import { open as openDialog } from "@tauri-apps/plugin-dialog";
import {
  ArrowDownTrayIcon,
  CloudArrowUpIcon,
  FolderOpenIcon,
  TrashIcon
} from "@heroicons/react/24/outline";
import {
  backupAppConfig,
  backupConfigEditorToOneDrive,
  backupLoginAccountsToOneDrive,
  clearBackupFolder,
  deleteAppConfigBackupSet,
  getAppConfigBackupStatus,
  getConfigEditorStatus,
  getBackupProviderSettings,
  getLoginOneDriveBackupStatus,
  openAppConfigBackupSetFolder,
  restoreBackupSet,
  saveBackupFolder,
  type BackupProviderSettings
} from "../backendConnector/api.ts";
import { ConfirmationModal } from "../elements/ConfirmationModal.tsx";
import type { LoginOneDriveBackupStatus } from "../backendConnector/loginApi.ts";
import type { AppConfigBackupStatus } from "../backendConnector/appConfigBackupApi.ts";
import type { OneDriveBackupStatus } from "../types.ts";

type BackupsManagerProps = {
  loading: boolean;
  onBusyChange: (busy: boolean, message?: string) => void;
  onAccountsChanged: () => void | Promise<void>;
  onStatusRefresh: () => Promise<void>;
  onMessage: (message: string) => void;
};

type BackupKind = "login" | "rose-config" | "app-config";

type BackupRow = {
  id: BackupKind;
  title: string;
  label: string;
  folderName: string;
  status: LoginOneDriveBackupStatus | OneDriveBackupStatus | AppConfigBackupStatus | null;
};

type BackupSet = {
  name: string;
  subfolders: BackupRow[];
};

type BackupStatusWithEntries =
  | LoginOneDriveBackupStatus
  | OneDriveBackupStatus
  | AppConfigBackupStatus;

export function BackupsManager({
  loading,
  onBusyChange,
  onAccountsChanged,
  onStatusRefresh,
  onMessage
}: BackupsManagerProps) {
  const [loginStatus, setLoginStatus] = useState<LoginOneDriveBackupStatus | null>(null);
  const [configStatus, setConfigStatus] = useState<OneDriveBackupStatus | null>(null);
  const [appConfigStatus, setAppConfigStatus] = useState<AppConfigBackupStatus | null>(null);
  const [backupProviderSettings, setBackupProviderSettings] = useState<BackupProviderSettings | null>(null);
  const [deleteBackupTarget, setDeleteBackupTarget] = useState<BackupSet | null>(null);

  useEffect(() => {
    void loadStatuses(false);
    void loadProviderSettings();
  }, []);

  useEffect(() => {
    const reloadBackupStatus = () => {
      void loadStatuses(false);
      void loadProviderSettings();
    };
    window.addEventListener("roToolbox:backup-provider-changed", reloadBackupStatus);
    return () => window.removeEventListener("roToolbox:backup-provider-changed", reloadBackupStatus);
  }, []);

  const rows: BackupRow[] = [
    { id: "login", title: "Accounts", label: "login", folderName: "Login Manager", status: loginStatus },
    { id: "rose-config", title: "ROSE config", label: "ROSE config", folderName: "ROSE Online Config", status: configStatus },
    { id: "app-config", title: "RO Toolbox config", label: "app config", folderName: "RO Toolbox Config", status: appConfigStatus }
  ];
  const backupSets = buildBackupSets(rows);
  const backupFolderPath = backupProviderSettings?.localPath ?? null;
  const backupFolderSelected = Boolean(backupFolderPath);

  function toErrorMessage(err: unknown, fallback: string) {
    if (err instanceof Error && err.message) {
      return err.message;
    }
    if (typeof err === "string" && err.trim()) {
      return err;
    }
    if (err && typeof err === "object" && "message" in err && typeof err.message === "string") {
      return err.message;
    }
    return fallback;
  }

  async function loadStatuses(showBusy: boolean) {
    if (showBusy) {
      onBusyChange(true, "Loading backups...");
    }
    try {
      applyBackupStatuses(await loadBackupStatuses());
    } catch (err) {
      onMessage(toErrorMessage(err, "Failed to load backup status."));
    } finally {
      if (showBusy) {
        onBusyChange(false);
      }
    }
  }

  async function loadBackupStatuses() {
    const [loginBackupStatus, configEditorStatus, appConfigBackupStatus] = await Promise.all([
      getLoginOneDriveBackupStatus(),
      getConfigEditorStatus(),
      getAppConfigBackupStatus()
    ]);
    return { loginBackupStatus, configEditorStatus, appConfigBackupStatus };
  }

  function applyBackupStatuses(statuses: Awaited<ReturnType<typeof loadBackupStatuses>>) {
    setLoginStatus(statuses.loginBackupStatus);
    setConfigStatus(statuses.configEditorStatus.oneDriveBackup);
    setAppConfigStatus(statuses.appConfigBackupStatus);
  }

  async function loadProviderSettings() {
    try {
      setBackupProviderSettings(await getBackupProviderSettings());
    } catch (err) {
      onMessage(toErrorMessage(err, "Failed to load backup folder."));
    }
  }

  async function onBrowseBackupFolder() {
    try {
      const selected = await openDialog({
        directory: true,
        multiple: false
      });
      if (!selected || Array.isArray(selected)) {
        return;
      }
      onBusyChange(true, "Saving backup folder...");
      try {
        const nextSettings = await saveBackupFolder(selected);
        setBackupProviderSettings(nextSettings);
        window.dispatchEvent(new Event("roToolbox:backup-provider-changed"));
        await loadStatuses(false);
        onMessage("Backup folder saved. Backups will be stored in RO Toolbox backups.");
      } finally {
        onBusyChange(false);
      }
    } catch (err) {
      onMessage(toErrorMessage(err, "Backup folder selection failed."));
    }
  }

  async function onClearBackupFolder() {
    onBusyChange(true, "Clearing backup folder...");
    try {
      const nextSettings = await clearBackupFolder();
      setBackupProviderSettings(nextSettings);
      window.dispatchEvent(new Event("roToolbox:backup-provider-changed"));
      await loadStatuses(false);
      onMessage("Backup folder cleared.");
    } catch (err) {
      onMessage(toErrorMessage(err, "Failed to clear backup folder."));
    } finally {
      onBusyChange(false);
    }
  }

  async function onBackupAll() {
    if (!backupFolderSelected) {
      onMessage("Choose a backup folder first.");
      return;
    }
    const backupName = createBackupName();
    const backupTasks: Array<{ label: string; run: () => Promise<unknown> }> = [
      { label: "accounts", run: () => backupLoginAccountsToOneDrive(backupName) },
      { label: "ROSE config", run: () => backupConfigEditorToOneDrive(backupName) },
      { label: "RO Toolbox config", run: () => backupAppConfig(backupName) }
    ];

    onBusyChange(true, "Creating backups...");
    try {
      const results = await Promise.allSettled(backupTasks.map((task) => task.run()));
      await loadStatuses(false);
      notifyBackupsChanged();

      const failedLabels = results
        .map((result, index) => (result.status === "rejected" ? backupTasks[index].label : null))
        .filter((label): label is string => label !== null);
      const skippedLabels = results
        .map((result, index) => {
          if (result.status !== "fulfilled") {
            return null;
          }
          const value = result.value;
          if (value && typeof value === "object" && "copiedFiles" in value && value.copiedFiles === 0) {
            return backupTasks[index].label;
          }
          return null;
        })
        .filter((label): label is string => label !== null);
      const createdCount = backupTasks.length - failedLabels.length - skippedLabels.length;

      if (failedLabels.length === 0) {
        if (skippedLabels.length === 0) {
          onMessage("All backups created.");
        } else if (createdCount === 0) {
          onMessage(`Nothing to back up yet. Skipped: ${skippedLabels.join(", ")}.`);
        } else {
          onMessage(`Created ${createdCount} backup${createdCount === 1 ? "" : "s"}. Skipped: ${skippedLabels.join(", ")}.`);
        }
      } else if (failedLabels.length === backupTasks.length) {
        onMessage("No backups were created.");
      } else {
        const skippedText = skippedLabels.length > 0 ? ` Skipped: ${skippedLabels.join(", ")}.` : "";
        onMessage(`Created ${createdCount} backup${createdCount === 1 ? "" : "s"}. Failed: ${failedLabels.join(", ")}.${skippedText}`);
      }
    } catch (err) {
      onMessage(toErrorMessage(err, "Failed to create backups."));
    } finally {
      onBusyChange(false);
    }
  }

  async function onOpenBackupSetFolder(backupName: string) {
    if (!backupFolderSelected) {
      onMessage("Choose a backup folder first.");
      return;
    }
    onBusyChange(true, "Opening backup folder...");
    try {
      await openAppConfigBackupSetFolder(backupName);
      onMessage("Opened backup folder.");
    } catch (err) {
      onMessage(toErrorMessage(err, "Failed to open backup folder."));
    } finally {
      onBusyChange(false);
    }
  }

  async function onRestoreBackupSet(backupSet: BackupSet) {
    if (!backupFolderSelected) {
      onMessage("Choose a backup folder first.");
      return;
    }
    const confirmed = window.confirm(
      `Restore backup "${backupSet.name}"? Current files will be overwritten.`
    );
    if (!confirmed) {
      return;
    }

    onBusyChange(true, "Restoring backup...");
    try {
      const result = await restoreBackupSet(backupSet.name);
      await onAccountsChanged();
      await onStatusRefresh();
      await loadProviderSettings();
      await loadStatuses(false);
      window.dispatchEvent(new Event("roToolbox:accounts-changed"));
      notifyBackupsChanged();
      onMessage(`Backup restored: ${result.restoredSections.join(", ")}.`);
    } catch (err) {
      onMessage(toErrorMessage(err, "Failed to restore backup."));
    } finally {
      onBusyChange(false);
    }
  }

  async function onDeleteBackupConfirmed() {
    if (!deleteBackupTarget) {
      return;
    }
    const backupName = deleteBackupTarget.name;
    if (!backupFolderSelected) {
      setDeleteBackupTarget(null);
      onMessage("Choose a backup folder first.");
      return;
    }
    setDeleteBackupTarget(null);
    onBusyChange(true, "Deleting backup...");
    try {
      const result = await deleteAppConfigBackupSet(backupName);
      removeBackupSetFromState(result.backupName);
      const statuses = await loadBackupStatuses();
      applyBackupStatuses(statuses);
      notifyBackupsChanged();
      const remainingBackups = buildBackupSets([
        { ...rows[0], status: statuses.loginBackupStatus },
        { ...rows[1], status: statuses.configEditorStatus.oneDriveBackup },
        { ...rows[2], status: statuses.appConfigBackupStatus }
      ]);
      if (remainingBackups.some((backup) => backup.name === backupName)) {
        onMessage("Backup delete did not finish. Try again or check if the folder is open in another app.");
        return;
      }
      onMessage("Backup deleted.");
    } catch (err) {
      onMessage(toErrorMessage(err, "Failed to delete backup."));
    } finally {
      onBusyChange(false);
    }
  }

  function removeBackupSetFromState(backupName: string) {
    setLoginStatus((current) => removeBackupFromStatus(current, backupName));
    setConfigStatus((current) => removeBackupFromStatus(current, backupName));
    setAppConfigStatus((current) => removeBackupFromStatus(current, backupName));
  }

  function removeBackupFromStatus<T extends BackupStatusWithEntries | null>(status: T, backupName: string): T {
    if (!status) {
      return status;
    }
    return {
      ...status,
      backups: status.backups.filter((backup) => backup.name !== backupName)
    };
  }

  function createBackupName() {
    const now = new Date();
    const pad = (value: number) => value.toString().padStart(2, "0");
    return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}_${pad(now.getHours())}-${pad(now.getMinutes())}-${pad(now.getSeconds())}`;
  }

  function notifyBackupsChanged() {
    window.dispatchEvent(new Event("roToolbox:backups-changed"));
  }

  function buildBackupSets(backupRows: BackupRow[]) {
    const sets = new Map<string, BackupSet>();
    for (const row of backupRows) {
      for (const backup of row.status?.backups ?? []) {
        const existing = sets.get(backup.name);
        if (existing) {
          existing.subfolders.push(row);
        } else {
          sets.set(backup.name, { name: backup.name, subfolders: [row] });
        }
      }
    }
    return Array.from(sets.values()).sort((first, second) => second.name.localeCompare(first.name));
  }

  return (
    <section className="backupsManager">
      <div className="backupsManagerHeader">
        <p className="sectionTitle">Backups</p>
        <p className="activeProfileMeta">Manage account, ROSE config, and RO Toolbox config backups.</p>
      </div>

      {!backupFolderSelected ? (
        <div className="backupSetupPanel">
          <div className="setupModalIcon">☁</div>
          <p className="backupSetupTitle">Select your backup folder</p>
          <p className="backupSetupText">
            Choose where RO Toolbox should store backups. A folder named <code>RO Toolbox backups</code> will be used inside the selected location.
          </p>
          <button className="buttonStrong backupsMakeButton" disabled={loading} onClick={onBrowseBackupFolder}>
            <FolderOpenIcon className="heroIcon" aria-hidden="true" />
            Browse...
          </button>
        </div>
      ) : (
        <>

        <div className="backupProviderPicker">
          <p className="settingsSectionLabel">Backup folder</p>
          <div className={`settingsFolderDisplay${backupFolderPath ? "" : " settingsFolderEmpty"}`}>
            {backupFolderPath ?? "Not set"}
          </div>
          <div className="settingsFolderActions">
            <button className="buttonStrong" disabled={loading} onClick={onBrowseBackupFolder}>
              📂 Browse…
            </button>
            {backupFolderSelected ? (
              <button className="buttonSubtle" disabled={loading} onClick={onClearBackupFolder}>
                🗑 Clear
              </button>
            ) : null}
          </div>
        </div>

        <div className="backupGroup">
          <div className="backupGroupHeader">
            <div>
              <p className="backupGroupTitle">Backups</p>
              <p className="backupGroupMeta">Includes accounts, ROSE config, and RO Toolbox config.</p>
            </div>
            <span className="backupGroupCount">
              {backupSets.length} stored
            </span>
          </div>

          <div className="backupChildList">
            {backupSets.length === 0 ? (
              <div className="backupChildItem">
                <div className="backupChildInfo">
                  <p className="backupChildTitle">No backups yet</p>
                  <p className="backupChildMeta">Create a backup to store accounts, ROSE config, and RO Toolbox config.</p>
                </div>
              </div>
            ) : (
              backupSets.map((backupSet) => {
                const openFolderRow = backupSet.subfolders.find((row) => row.status?.available);
              return (
                <div key={backupSet.name} className="backupChildItem">
                  <div className="backupChildInfo">
                    <p className="backupChildTitle">{backupSet.name}</p>
                    <div className="backupIncludedList" aria-label="Included backups">
                      {backupSet.subfolders.map((row) => (
                        <span key={row.id} className="backupIncludedBadge">
                          {row.title}
                        </span>
                      ))}
                    </div>
                  </div>
                  <div className="backupChildActions">
                    <button
                      type="button"
                      className="iconBtn iconBtnSubtle"
                      disabled={loading || !backupFolderSelected || !openFolderRow}
                      onClick={() => onOpenBackupSetFolder(backupSet.name)}
                      title="Open backup folder"
                      aria-label={`Open ${backupSet.name} backup folder`}
                    >
                      <FolderOpenIcon className="heroIcon" aria-hidden="true" />
                    </button>
                    <button
                      type="button"
                      className="iconBtn iconBtnSubtle"
                      disabled={loading || !backupFolderSelected || backupSet.subfolders.length === 0}
                      onClick={() => onRestoreBackupSet(backupSet)}
                      title={`Restore backup ${backupSet.name}`}
                      aria-label={`Restore backup ${backupSet.name}`}
                    >
                      <ArrowDownTrayIcon className="heroIcon" aria-hidden="true" />
                    </button>
                    <button
                      type="button"
                      className="iconBtn iconBtnDanger"
                      disabled={loading || !backupFolderSelected}
                      onClick={() => setDeleteBackupTarget(backupSet)}
                      title={`Delete backup ${backupSet.name}`}
                      aria-label={`Delete backup ${backupSet.name}`}
                    >
                      <TrashIcon className="heroIcon" aria-hidden="true" />
                    </button>
                  </div>
                </div>
              );
            }))}
          </div>
        </div>

        <div className="backupsManagerFooter">
          <button type="button" className="buttonStrong backupsMakeButton" disabled={loading || !backupFolderSelected} onClick={onBackupAll}>
            <CloudArrowUpIcon className="heroIcon" aria-hidden="true" />
            Make backup
          </button>
        </div>
        </>
      )}
        <ConfirmationModal
          open={deleteBackupTarget !== null}
          title="Delete backup"
          message={`Delete backup "${deleteBackupTarget?.name ?? "selected backup"}"? This cannot be undone.`}
          confirmLabel="Delete"
          cancelLabel="Cancel"
          onConfirm={onDeleteBackupConfirmed}
          onClose={() => setDeleteBackupTarget(null)}
        />
    </section>
  );
}
