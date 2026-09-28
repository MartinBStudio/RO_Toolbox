import { useEffect, useState } from "react";
import { open as openDialog } from "@tauri-apps/plugin-dialog";
import {
  ArrowDownTrayIcon,
  CloudArrowUpIcon,
  FolderOpenIcon
} from "@heroicons/react/24/outline";
import {
  backupAppConfig,
  backupConfigEditorToOneDrive,
  backupLoginAccountsToOneDrive,
  cleanupAppConfigBackups,
  cleanupConfigEditorBackups,
  cleanupLoginAccountBackups,
  deleteAllAppConfigBackups,
  deleteAllConfigEditorBackups,
  deleteAllLoginAccountBackups,
  getAppConfigBackupStatus,
  getConfigEditorStatus,
  getBackupProviderSettings,
  getLoginOneDriveBackupStatus,
  openAppConfigBackupFolder,
  openConfigEditorOneDriveBackupFolder,
  openLoginOneDriveBackupFolder,
  restoreAppConfigBackup,
  restoreConfigEditorFromOneDrive,
  restoreLoginAccountsFromOneDrive,
  saveBackupProviderSettings,
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
  status: LoginOneDriveBackupStatus | OneDriveBackupStatus | AppConfigBackupStatus | null;
};

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
  const [deleteAllConfirmOpen, setDeleteAllConfirmOpen] = useState(false);
  const [selectedBackups, setSelectedBackups] = useState<Record<BackupKind, string>>({
    login: "",
    "rose-config": "",
    "app-config": ""
  });

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

  useEffect(() => {
    syncSelectedBackup("login", loginStatus);
  }, [loginStatus]);

  useEffect(() => {
    syncSelectedBackup("rose-config", configStatus);
  }, [configStatus]);

  useEffect(() => {
    syncSelectedBackup("app-config", appConfigStatus);
  }, [appConfigStatus]);

  const rows: BackupRow[] = [
    { id: "login", title: "Accounts", label: "login", status: loginStatus },
    { id: "rose-config", title: "ROSE config", label: "ROSE config", status: configStatus },
    { id: "app-config", title: "RO Toolbox config", label: "app config", status: appConfigStatus }
  ];
  const selectedBackupProvider = backupProviderSettings?.providers.find(
    (provider) => provider.id === backupProviderSettings.selectedProviderId
  );
  const backupProviderDisplay =
    selectedBackupProvider?.path ??
    (selectedBackupProvider?.id === "auto" ? "Auto uses the first detected cloud folder." : "Not set");

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

  function syncSelectedBackup(kind: BackupKind, status: LoginOneDriveBackupStatus | OneDriveBackupStatus | AppConfigBackupStatus | null) {
    const backups = status?.backups ?? [];
    setSelectedBackups((current) => {
      if (backups.length === 0) {
        return current[kind] ? { ...current, [kind]: "" } : current;
      }
      if (backups.some((backup) => backup.name === current[kind])) {
        return current;
      }
      return { ...current, [kind]: backups[0].name };
    });
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
      onMessage(toErrorMessage(err, "Failed to load backup providers."));
    }
  }

  async function onBackupProviderChange(providerId: string) {
    onBusyChange(true, "Saving backup provider...");
    try {
      const nextSettings = await saveBackupProviderSettings(providerId, backupProviderSettings?.localPath ?? null);
      setBackupProviderSettings(nextSettings);
      window.dispatchEvent(new Event("roToolbox:backup-provider-changed"));
      await loadStatuses(false);
      onMessage("Backup provider saved.");
    } catch (err) {
      onMessage(toErrorMessage(err, "Failed to save backup provider."));
    } finally {
      onBusyChange(false);
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
        const nextSettings = await saveBackupProviderSettings("local", selected);
        setBackupProviderSettings(nextSettings);
        window.dispatchEvent(new Event("roToolbox:backup-provider-changed"));
        await loadStatuses(false);
        onMessage("Backup folder saved.");
      } finally {
        onBusyChange(false);
      }
    } catch (err) {
      onMessage(toErrorMessage(err, "Backup folder selection failed."));
    }
  }

  async function onBackupAll() {
    const backupTasks: Array<{ label: string; run: () => Promise<unknown> }> = [
      { label: "accounts", run: backupLoginAccountsToOneDrive },
      { label: "ROSE config", run: backupConfigEditorToOneDrive },
      { label: "RO Toolbox config", run: backupAppConfig }
    ];

    onBusyChange(true, "Creating backups...");
    try {
      const results = await Promise.allSettled(backupTasks.map((task) => task.run()));
      await loadStatuses(false);

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

  async function onDeleteAllButLatest() {
    const confirmed = window.confirm("Delete older backups and keep only the latest backup for each section?");
    if (!confirmed) {
      return;
    }

    const beforeCount = rows.reduce((total, row) => total + Math.max((row.status?.backups.length ?? 0) - 1, 0), 0);
    onBusyChange(true, "Deleting old backups...");
    try {
      const results = await Promise.allSettled([
        cleanupLoginAccountBackups(),
        cleanupConfigEditorBackups(),
        cleanupAppConfigBackups()
      ]);
      await loadStatuses(false);
      const reportedDeletedCount = results.reduce((total, result) => {
        if (result.status !== "fulfilled") {
          return total;
        }
        return total + result.value.deletedBackups;
      }, 0);
      const afterStatuses = await loadBackupStatuses();
      applyBackupStatuses(afterStatuses);
      const afterCount = [
        afterStatuses.loginBackupStatus,
        afterStatuses.configEditorStatus.oneDriveBackup,
        afterStatuses.appConfigBackupStatus
      ].reduce((total, status) => total + Math.max(status.backups.length - 1, 0), 0);
      const deletedCount = Math.max(reportedDeletedCount, beforeCount - afterCount);
      const failedCount = results.filter((result) => result.status === "rejected").length;
      if (failedCount > 0 && deletedCount === 0) {
        onMessage(`Deleted 0 old backups. ${failedCount} cleanup task${failedCount === 1 ? "" : "s"} failed.`);
      } else {
        onMessage(`Deleted ${deletedCount} old backup${deletedCount === 1 ? "" : "s"}.`);
      }
    } catch (err) {
      onMessage(toErrorMessage(err, "Failed to delete old backups."));
    } finally {
      onBusyChange(false);
    }
  }

  async function onDeleteAllConfirmed() {
    setDeleteAllConfirmOpen(false);
    const beforeCount = rows.reduce((total, row) => total + (row.status?.backups.length ?? 0), 0);
    onBusyChange(true, "Deleting all backups...");
    try {
      const results = await Promise.allSettled([
        deleteAllLoginAccountBackups(),
        deleteAllConfigEditorBackups(),
        deleteAllAppConfigBackups()
      ]);
      const reportedDeletedCount = results.reduce((total, result) => {
        if (result.status !== "fulfilled") {
          return total;
        }
        return total + result.value.deletedBackups;
      }, 0);
      const afterStatuses = await loadBackupStatuses();
      applyBackupStatuses(afterStatuses);
      const afterCount = [
        afterStatuses.loginBackupStatus,
        afterStatuses.configEditorStatus.oneDriveBackup,
        afterStatuses.appConfigBackupStatus
      ].reduce((total, status) => total + status.backups.length, 0);
      const deletedCount = Math.max(reportedDeletedCount, beforeCount - afterCount);
      const failedCount = results.filter((result) => result.status === "rejected").length;
      if (failedCount > 0 && deletedCount === 0) {
        onMessage(`Deleted 0 backups. ${failedCount} delete task${failedCount === 1 ? "" : "s"} failed.`);
      } else {
        onMessage(`Deleted ${deletedCount} backup${deletedCount === 1 ? "" : "s"}.`);
      }
    } catch (err) {
      onMessage(toErrorMessage(err, "Failed to delete backups."));
    } finally {
      onBusyChange(false);
    }
  }

  async function onOpenFolder(kind: BackupKind) {
    onBusyChange(true, "Opening backup folder...");
    try {
      if (kind === "login") {
        await openLoginOneDriveBackupFolder();
      } else if (kind === "rose-config") {
        await openConfigEditorOneDriveBackupFolder();
      } else {
        await openAppConfigBackupFolder();
      }
      onMessage("Opened backup folder.");
    } catch (err) {
      onMessage(toErrorMessage(err, "Failed to open backup folder."));
    } finally {
      onBusyChange(false);
    }
  }

  async function onRestore(kind: BackupKind) {
    const selectedBackupName = selectedBackups[kind];
    if (!selectedBackupName) {
      onMessage("Choose a backup to restore.");
      return;
    }

    const confirmed = window.confirm(
      restoreConfirmationMessage(kind, selectedBackupName)
    );
    if (!confirmed) {
      return;
    }

    onBusyChange(true, restoreLoadingMessage(kind));
    try {
      if (kind === "login") {
        await restoreLoginAccountsFromOneDrive(selectedBackupName);
        await onAccountsChanged();
        onMessage("Login accounts restored.");
      } else if (kind === "rose-config") {
        const result = await restoreConfigEditorFromOneDrive(selectedBackupName);
        onMessage(`Restored ${result.restoredFiles} ROSE config file${result.restoredFiles === 1 ? "" : "s"}.`);
      } else {
        await restoreAppConfigBackup(selectedBackupName);
        await onStatusRefresh();
        await loadProviderSettings();
        onMessage("RO Toolbox config restored.");
      }
      await loadStatuses(false);
    } catch (err) {
      onMessage(toErrorMessage(err, restoreErrorMessage(kind)));
    } finally {
      onBusyChange(false);
    }
  }

  function restoreConfirmationMessage(kind: BackupKind, backupName: string) {
    if (kind === "login") {
      return `Restore login accounts from "${backupName}"? Current accounts will be backed up first.`;
    }
    if (kind === "rose-config") {
      return `Restore ROSE config files from "${backupName}"? Current files will be backed up first.`;
    }
    return `Restore RO Toolbox config from "${backupName}"? Current app settings will be backed up first.`;
  }

  function restoreLoadingMessage(kind: BackupKind) {
    if (kind === "login") {
      return "Restoring accounts...";
    }
    if (kind === "rose-config") {
      return "Restoring ROSE config...";
    }
    return "Restoring RO Toolbox config...";
  }

  function restoreErrorMessage(kind: BackupKind) {
    if (kind === "login") {
      return "Failed to restore accounts.";
    }
    if (kind === "rose-config") {
      return "Failed to restore ROSE config files.";
    }
    return "Failed to restore RO Toolbox config.";
  }

  return (
    <section className="backupsManager">
      <div className="backupsManagerHeader">
        <p className="sectionTitle">Backups</p>
        <p className="activeProfileMeta">Manage account, ROSE config, and RO Toolbox config backups.</p>
      </div>

        <div className="backupProviderPicker">
          <div className="backupProviderPickerMain">
            <p className="settingsSectionLabel">Provider</p>
            <select
              value={backupProviderSettings?.selectedProviderId ?? "auto"}
              disabled={loading || !backupProviderSettings}
              onChange={(event) => {
                void onBackupProviderChange(event.target.value);
              }}
              aria-label="Backup provider"
            >
              {backupProviderSettings?.providers.map((provider) => (
                <option
                  key={provider.id}
                  value={provider.id}
                  disabled={provider.id !== "auto" && !provider.available}
                >
                  {provider.name}
                  {provider.id !== "auto" && !provider.available ? " (not detected)" : ""}
                </option>
              ))}
            </select>
          </div>
          <div
            className={`backupProviderPath${selectedBackupProvider?.path ? "" : " backupProviderPathEmpty"}`}
            title={backupProviderDisplay}
          >
            {backupProviderDisplay}
          </div>
          <button
            type="button"
            className="iconBtn iconBtnSubtle backupProviderBrowseButton"
            disabled={loading}
            onClick={onBrowseBackupFolder}
            title="Choose custom backup folder"
            aria-label="Choose custom backup folder"
          >
            <FolderOpenIcon className="heroIcon" aria-hidden="true" />
          </button>
        </div>

        <div className="backupGroup">
          <div className="backupGroupHeader">
            <div>
              <p className="backupGroupTitle">Backup</p>
              <p className="backupGroupMeta">Includes accounts, ROSE config, and RO Toolbox config.</p>
            </div>
            <span className="backupGroupCount">
              {rows.reduce((total, row) => total + (row.status?.backups.length ?? 0), 0)} stored
            </span>
          </div>

          <div className="backupChildList">
            {rows.map((row) => {
              const status = row.status;
              const available = Boolean(status?.available);
              const backups = status?.backups ?? [];
              return (
                <div key={row.id} className="backupChildItem">
                  <div className="backupChildInfo">
                    <p className="backupChildTitle">{row.title}</p>
                    <p className="backupChildMeta">
                      {backups.length === 0 ? `No ${row.label} backups` : `${backups.length} backup${backups.length === 1 ? "" : "s"}`}
                    </p>
                  </div>
                  <select
                    value={selectedBackups[row.id]}
                    onChange={(event) => setSelectedBackups((current) => ({ ...current, [row.id]: event.target.value }))}
                    disabled={loading || !available || backups.length === 0}
                    aria-label={`${row.title} backup`}
                  >
                    {backups.length === 0 ? (
                      <option value="">No backups yet</option>
                    ) : (
                      backups.map((backup) => (
                        <option key={backup.name} value={backup.name}>
                          {backup.name}
                        </option>
                      ))
                    )}
                  </select>
                  <div className="backupChildActions">
                    <button
                      type="button"
                      className="iconBtn iconBtnSubtle"
                      disabled={loading || !available}
                      onClick={() => onOpenFolder(row.id)}
                      title={status?.backupRootPath ? `Open backup folder: ${status.backupRootPath}` : "Open backup folder"}
                      aria-label={`Open ${row.label} backup folder`}
                    >
                      <FolderOpenIcon className="heroIcon" aria-hidden="true" />
                    </button>
                    <button
                      type="button"
                      className="iconBtn iconBtnSubtle"
                      disabled={loading || !available || !selectedBackups[row.id]}
                      onClick={() => onRestore(row.id)}
                      title={`Restore selected ${row.label} backup`}
                      aria-label={`Restore selected ${row.label} backup`}
                    >
                      <ArrowDownTrayIcon className="heroIcon" aria-hidden="true" />
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        <div className="backupsManagerFooter">
          <button type="button" className="buttonStrong backupsMakeButton" disabled={loading} onClick={onBackupAll}>
            <CloudArrowUpIcon className="heroIcon" aria-hidden="true" />
            Make backup
          </button>
          <button type="button" className="buttonSubtle" disabled={loading} onClick={onDeleteAllButLatest}>
            Delete old backups
          </button>
          <button type="button" className="buttonDanger" disabled={loading} onClick={() => setDeleteAllConfirmOpen(true)}>
            Delete all backups
          </button>
          <button type="button" className="buttonSubtle" disabled={loading} onClick={() => loadStatuses(true)}>
            Refresh
          </button>
        </div>
        <ConfirmationModal
          open={deleteAllConfirmOpen}
          title="Delete all backups"
          message="Delete every backup for accounts, ROSE config, and RO Toolbox config? This cannot be undone."
          confirmLabel="Delete all"
          cancelLabel="Cancel"
          onConfirm={onDeleteAllConfirmed}
          onClose={() => setDeleteAllConfirmOpen(false)}
        />
    </section>
  );
}
