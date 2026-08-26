import { request } from "./apiClient.ts";

export function downloadCombatTextProfiles() {
  return request<{ message: string }>("/combattext/download", { method: "POST" });
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
