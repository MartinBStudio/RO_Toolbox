import { request } from "./apiClient.ts";

export function downloadProfiles() {
  return request<{ message: string }>("/loot/download", { method: "POST" });
}

export function installProfile(profileId: string) {
  return request<{ message: string }>("/loot/install", {
    method: "POST",
    body: JSON.stringify({ profileId })
  });
}

export function clearResources() {
  return request<{ message: string }>("/loot/clear-resources", { method: "POST" });
}

export function clearInstalled() {
  return request<{ message: string }>("/loot/clear-installed", { method: "POST" });
}

export function openResourcesFolder() {
  return request<{ message: string }>("/loot/folders/open/resources", { method: "POST" });
}

export function openItemFolder() {
  return request<{ message: string }>("/loot/folders/open/item", { method: "POST" });
}
