import { request } from "./apiClient.ts";

export type AppConfigBackupStatus = {
  available: boolean;
  providerName: string | null;
  oneDrivePath: string | null;
  backupRootPath: string | null;
  backups: AppConfigBackupEntry[];
};

export type AppConfigBackupEntry = {
  name: string;
  path: string;
  files: string[];
};

export type AppConfigBackupResult = {
  backupPath: string;
  copiedFiles: number;
  files: string[];
};

export type AppConfigRestoreResult = {
  restoredFrom: string;
  restoredFiles: number;
  files: string[];
  safetyBackupPath: string | null;
  safetyBackupFiles: string[];
};

export type BackupSetRestoreResult = {
  backupName: string;
  restoredSections: string[];
};

export type AppConfigBackupCleanupResult = {
  deletedBackups: number;
  keptBackupName: string | null;
};

export type BackupSetDeleteResult = {
  backupName: string;
  deletedPath: string;
};

export function getAppConfigBackupStatus() {
  return request<AppConfigBackupStatus>("/app-config-backup/status");
}

export function backupAppConfig(backupName?: string) {
  return request<AppConfigBackupResult>("/app-config-backup", {
    method: "POST",
    body: backupName ? JSON.stringify({ backupName }) : undefined
  });
}

export function openAppConfigBackupFolder() {
  return request<{ message: string }>("/app-config-backup/folder/open", { method: "POST" });
}

export function openAppConfigBackupSetFolder(backupName: string) {
  return request<{ message: string }>("/app-config-backup/folder/open-backup", {
    method: "POST",
    body: JSON.stringify({ backupName })
  });
}

export function restoreAppConfigBackup(backupName: string) {
  return request<AppConfigRestoreResult>("/app-config-backup/restore", {
    method: "POST",
    body: JSON.stringify({ backupName })
  });
}

export function restoreBackupSet(backupName: string) {
  return request<BackupSetRestoreResult>("/app-config-backup/restore/backup", {
    method: "POST",
    body: JSON.stringify({ backupName })
  });
}

export function cleanupAppConfigBackups() {
  return request<AppConfigBackupCleanupResult>("/app-config-backup/cleanup", { method: "POST" });
}

export function deleteAllAppConfigBackups() {
  return request<AppConfigBackupCleanupResult>("/app-config-backup/cleanup/all", { method: "POST" });
}

export function deleteAppConfigBackupSet(backupName: string) {
  return request<BackupSetDeleteResult>("/app-config-backup/cleanup/backup", {
    method: "POST",
    body: JSON.stringify({ backupName })
  });
}
