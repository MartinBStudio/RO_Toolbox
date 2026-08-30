import { request } from "./apiClient.ts";

export type LoginAccount = {
  id: string;
  name: string;
  email: string;
  password: string;
  displayInQuick: boolean;
  icon: string;
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

export function exportLoginAccounts() {
  return request<{ version: number; accounts: LoginAccount[] }>("/login/export");
}

export function importLoginAccounts(data: {
  accounts: Array<{
    id?: string;
    name: string;
    email: string;
    password: string;
    displayInQuick?: boolean;
    icon?: string;
  }>;
  replaceExisting?: boolean;
}) {
  return request<{ totalAccounts: number; message: string }>("/login/import", {
    method: "POST",
    body: JSON.stringify(data)
  });
}

export function saveLoginAccountsExportFile(data: { filePath: string; content: string }) {
  return request<{ message: string }>("/login/export/save", {
    method: "POST",
    body: JSON.stringify(data)
  });
}
