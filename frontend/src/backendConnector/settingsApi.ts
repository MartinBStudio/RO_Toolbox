import { request } from "./apiClient.ts";

export function saveGameFolder(path: string, forceSave = false) {
  return request<{ containsExpectedItemFolder: boolean }>("/settings/game-folder", {
    method: "POST",
    body: JSON.stringify({ path, forceSave })
  });
}

export function clearGameFolder() {
  return request<{ message: string }>("/settings/game-folder/clear", { method: "POST" });
}

export function factoryReset() {
  return request<{ message: string }>("/settings/factory-reset", { method: "POST" });
}

export function quickLaunchGame() {
  return request<{ message: string }>("/settings/quick-launch", { method: "POST" });
}

export function getReleaseNotes() {
  return request<{ content: string }>("/settings/release-notes");
}

export function getSelectedServiceSetting() {
  return request<{ serviceId: string | null }>("/settings/selected-service");
}

export function saveSelectedServiceSetting(serviceId: string) {
  return request<{ serviceId: string }>("/settings/selected-service", {
    method: "POST",
    body: JSON.stringify({ serviceId })
  });
}

export function getUsefulStuffCollapsedSetting() {
  return request<{ collapsed: boolean }>("/settings/useful-stuff-collapsed");
}

export function saveUsefulStuffCollapsedSetting(collapsed: boolean) {
  return request<{ collapsed: boolean }>("/settings/useful-stuff-collapsed", {
    method: "POST",
    body: JSON.stringify({ collapsed })
  });
}

export function getQuickLaunchOnlyModeSetting() {
  return request<{ enabled: boolean }>("/settings/quick-launch-only-mode");
}

export function saveQuickLaunchOnlyModeSetting(enabled: boolean) {
  return request<{ enabled: boolean }>("/settings/quick-launch-only-mode", {
    method: "POST",
    body: JSON.stringify({ enabled })
  });
}

export function getIgnoreConfigWarningsSetting() {
  return request<{ enabled: boolean }>("/settings/ignore-config-warnings");
}

export function saveIgnoreConfigWarningsSetting(enabled: boolean) {
  return request<{ enabled: boolean }>("/settings/ignore-config-warnings", {
    method: "POST",
    body: JSON.stringify({ enabled })
  });
}
