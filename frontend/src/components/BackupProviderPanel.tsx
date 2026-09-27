import {
  ArrowDownTrayIcon,
  CloudIcon,
  CloudArrowUpIcon,
  FolderOpenIcon
} from "@heroicons/react/24/outline";

export type BackupProviderPanelEntry = {
  name: string;
  files: string[];
};

type BackupProviderPanelProps = {
  available: boolean;
  providerName?: string | null;
  backupRootPath: string | null;
  backups: BackupProviderPanelEntry[];
  selectedBackupName: string;
  busy: boolean;
  label: string;
  onBackup: () => void;
  onOpenFolder: () => void;
  onRestore: () => void;
  onSelectedBackupChange: (backupName: string) => void;
};

export function BackupProviderPanel({
  available,
  providerName,
  backupRootPath,
  backups,
  selectedBackupName,
  busy,
  label,
  onBackup,
  onOpenFolder,
  onRestore,
  onSelectedBackupChange
}: BackupProviderPanelProps) {
  if (!available) {
    return null;
  }

  const providerLabel = providerName || "Cloud";

  return (
    <div className="backupProviderPanel">
      <div className="backupProviderPanelMeta">
        <CloudIcon className="backupProviderPanelLogo" aria-hidden="true" />
        <div>
          <p className="backupProviderPanelTitle">{providerLabel} backups</p>
          <p className="backupProviderPanelSubtitle">
            {backups.length === 0 ? `No ${label} backups yet.` : `${backups.length} ${label} backup${backups.length === 1 ? "" : "s"} available.`}
          </p>
        </div>
      </div>

      <div className="backupProviderPanelControls">
        <button
          type="button"
          className="iconBtn iconBtnSubtle"
          disabled={busy}
          onClick={onBackup}
          title={`Back up ${label} to ${providerLabel}`}
          aria-label={`Back up ${label} to ${providerLabel}`}
        >
          <CloudArrowUpIcon className="heroIcon" aria-hidden="true" />
        </button>
        <button
          type="button"
          className="iconBtn iconBtnSubtle"
          disabled={busy}
          onClick={onOpenFolder}
          title={backupRootPath ? `Open backup folder: ${backupRootPath}` : "Open backup folder"}
          aria-label={`Open ${providerLabel} backup folder`}
        >
          <FolderOpenIcon className="heroIcon" aria-hidden="true" />
        </button>
        <select
          value={selectedBackupName}
          onChange={(event) => onSelectedBackupChange(event.target.value)}
          disabled={busy || backups.length === 0}
          aria-label={`${providerLabel} ${label} backup`}
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
        <button
          type="button"
          className="iconBtn iconBtnSubtle"
          disabled={busy || !selectedBackupName}
          onClick={onRestore}
          title={`Restore selected ${label} backup`}
          aria-label={`Restore selected ${label} backup`}
        >
          <ArrowDownTrayIcon className="heroIcon" aria-hidden="true" />
        </button>
      </div>
    </div>
  );
}
