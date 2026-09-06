import type { ResourcesUpdateCheckResult } from "../types.ts";
import { request } from "./apiClient.ts";

export function downloadBuffIconsProfiles() {
  return request<{ message: string }>("/bufficons/download", { method: "POST" });
}

export function installBuffIconsProfile(profileId: string) {
  return request<{ message: string }>("/bufficons/install", {
    method: "POST",
    body: JSON.stringify({ profileId })
  });
}

export function clearBuffIconsResources() {
  return request<{ message: string }>("/bufficons/clear-resources", { method: "POST" });
}

export function clearBuffIconsInstalled() {
  return request<{ message: string }>("/bufficons/clear-installed", { method: "POST" });
}

export function openBuffIconsResourcesFolder() {
  return request<{ message: string }>("/bufficons/folders/open/resources", { method: "POST" });
}

export function openBuffIconsItemFolder() {
  return request<{ message: string }>("/bufficons/folders/open/item", { method: "POST" });
}

export function checkBuffIconsResourcesUpdate() {
  return request<ResourcesUpdateCheckResult>("/bufficons/check-update");
}
