import type { ResourcesUpdateCheckResult } from "../types.ts";
import { request } from "./apiClient.ts";
import { downloadResourceWithProgress } from "./resourceDownloadApi.ts";
import type { ResourceDownloadProgress } from "./resourceDownloadApi.ts";

export function downloadBuffsProfiles(
  onProgress: (progress: ResourceDownloadProgress) => void
) {
  return downloadResourceWithProgress(
    "/buffs/download/progress",
    onProgress,
    "Buff packages downloaded."
  );
}

export function installBuffsProfile(profileId: string) {
  return request<{ message: string }> ("/buffs/install", {
    method: "POST",
    body: JSON.stringify({ profileId })
  });
}

export function clearBuffsResources() {
  return request<{ message: string }> ("/buffs/clear-resources", { method: "POST" });
}

export function clearBuffsInstalled() {
  return request<{ message: string }> ("/buffs/clear-installed", { method: "POST" });
}

export function openBuffsResourcesFolder() {
  return request<{ message: string }> ("/buffs/folders/open/resources", { method: "POST" });
}

export function openBuffsItemFolder() {
  return request<{ message: string }> ("/buffs/folders/open/item", { method: "POST" });
}

export function checkBuffsResourcesUpdate() {
  return request<ResourcesUpdateCheckResult>("/buffs/check-update");
}
