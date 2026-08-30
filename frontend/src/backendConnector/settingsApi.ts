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
