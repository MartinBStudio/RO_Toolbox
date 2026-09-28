import type { ResourcesUpdateCheckResult } from "../types.ts";
import { request } from "./apiClient.ts";
import { downloadResourceWithProgress } from "./resourceDownloadApi.ts";
import type { ResourceDownloadProgress } from "./resourceDownloadApi.ts";

export function downloadCombatTextProfiles(
  onProgress: (progress: ResourceDownloadProgress) => void
) {
  return downloadResourceWithProgress(
    "/combattext/download/progress",
    onProgress,
    "Combat text packages downloaded."
  );
}

export function installCombatTextProfile(profileId: string) {
  return request<{ message: string }>("/combattext/install", {
    method: "POST",
    body: JSON.stringify({ profileId })
  });
}

export function clearCombatTextResources() {
  return request<{ message: string }>("/combattext/clear-resources", { method: "POST" });
}

export function clearCombatTextInstalled() {
  return request<{ message: string }>("/combattext/clear-installed", { method: "POST" });
}

export function openCombatTextResourcesFolder() {
  return request<{ message: string }>("/combattext/folders/open/resources", { method: "POST" });
}

export function openCombatTextItemFolder() {
  return request<{ message: string }>("/combattext/folders/open/item", { method: "POST" });
}

export function checkCombatTextResourcesUpdate() {
  return request<ResourcesUpdateCheckResult>("/combattext/check-update");
}
