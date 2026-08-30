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

export function quickLaunchGame() {
  return request<{ message: string }>("/settings/quick-launch", { method: "POST" });
}
