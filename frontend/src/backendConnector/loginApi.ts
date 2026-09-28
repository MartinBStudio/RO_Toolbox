import { request } from "./apiClient.ts";

export type LoginAccount = {
  id: string;
  name: string;
  email: string;
  password: string;
  displayInQuick: boolean;
  icon: string;
};

export type LoginOneDriveBackupStatus = {
  available: boolean;
  providerName: string | null;
  oneDrivePath: string | null;
  backupRootPath: string | null;
  backups: LoginOneDriveBackupEntry[];
};

export type LoginOneDriveBackupEntry = {
  name: string;
  path: string;
  files: string[];
};

export type LoginOneDriveBackupResult = {
  backupPath: string;
  copiedFiles: number;
  files: string[];
};

export type LoginOneDriveRestoreResult = {
  restoredFrom: string;
  restoredFiles: number;
  files: string[];
};

export type LoginBackupCleanupResult = {
  deletedBackups: number;
  keptBackupName: string | null;
};

export function listLoginAccounts() {
  return request<LoginAccount[]>("/login");
}

export function listQuickLoginAccounts() {
  return request<LoginAccount[]>("/login/quick");
}

export function createLoginAccount(data: {
  name: string;
  email: string;
  password: string;
  displayInQuick?: boolean;
  icon?: string;
}) {
  return request<LoginAccount>("/login", {
    method: "POST",
    body: JSON.stringify(data)
  });
}

export function updateLoginAccount(id: string, data: {
  name: string;
  email: string;
  password: string;
  displayInQuick?: boolean;
  icon?: string;
}) {
  return request<LoginAccount>(`/login/${id}`, {
    method: "PUT",
    body: JSON.stringify(data)
  });
}

export function deleteLoginAccount(id: string) {
  return request<LoginAccount>(`/login/${id}`, { method: "DELETE" });
}

export function quickLaunchLoginAccount(id: string) {
  return request<{ message: string }>(`/login/${id}/launch`, { method: "POST" });
}

export function getLoginOneDriveBackupStatus() {
  return request<LoginOneDriveBackupStatus>("/login/onedrive-backup/status");
}

export function backupLoginAccountsToOneDrive(backupName?: string) {
  return request<LoginOneDriveBackupResult>("/login/onedrive-backup", {
    method: "POST",
    body: backupName ? JSON.stringify({ backupName }) : undefined
  });
}

export function openLoginOneDriveBackupFolder() {
  return request<{ message: string }>("/login/onedrive-backup/folder/open", { method: "POST" });
}

export function restoreLoginAccountsFromOneDrive(backupName: string) {
  return request<LoginOneDriveRestoreResult>("/login/onedrive-backup/restore", {
    method: "POST",
    body: JSON.stringify({ backupName })
  });
}

export function restoreLatestLoginAccountsBackup() {
  return request<LoginOneDriveRestoreResult>("/login/onedrive-backup/restore/latest", { method: "POST" });
}

export function cleanupLoginAccountBackups() {
  return request<LoginBackupCleanupResult>("/login/onedrive-backup/cleanup", { method: "POST" });
}

export function deleteAllLoginAccountBackups() {
  return request<LoginBackupCleanupResult>("/login/onedrive-backup/cleanup/all", { method: "POST" });
}
