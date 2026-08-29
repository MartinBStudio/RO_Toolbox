import type { ResourcesUpdateCheckResult } from "../types.ts";
import { request } from "./apiClient.ts";

export function downloadUserInterfaceProfiles() {
  return request<{ message: string }>("/userinterface/download", { method: "POST" });
}

export function installUserInterfaceProfile(profileId: string) {
  return request<{ message: string }>("/userinterface/install", {
    method: "POST",
    body: JSON.stringify({ profileId })
  });
}

export function clearUserInterfaceResources() {
  return request<{ message: string }>("/userinterface/clear-resources", { method: "POST" });
}

export function clearUserInterfaceInstalled() {
  return request<{ message: string }>("/userinterface/clear-installed", { method: "POST" });
}

export function openUserInterfaceResourcesFolder() {
  return request<{ message: string }>("/userinterface/folders/open/resources", { method: "POST" });
}

export function openUserInterfaceItemFolder() {
  return request<{ message: string }>("/userinterface/folders/open/item", { method: "POST" });
}

export function checkUserInterfaceResourcesUpdate() {
  return request<ResourcesUpdateCheckResult>("/userinterface/check-update");
}
