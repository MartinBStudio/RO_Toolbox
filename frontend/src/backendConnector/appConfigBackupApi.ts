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

export type AppConfigBackupCleanupResult = {
  deletedBackups: number;
  keptBackupName: string | null;
};

export function getAppConfigBackupStatus() {
  return request<AppConfigBackupStatus>("/app-config-backup/status");
}

export function backupAppConfig() {
  return request<AppConfigBackupResult>("/app-config-backup", { method: "POST" });
}

export function openAppConfigBackupFolder() {
  return request<{ message: string }>("/app-config-backup/folder/open", { method: "POST" });
}

export function restoreAppConfigBackup(backupName: string) {
  return request<AppConfigRestoreResult>("/app-config-backup/restore", {
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
