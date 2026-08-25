import type { AppStatus } from "./types";

const API_BASE = "http://localhost:8080/api";

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: {
      "Content-Type": "application/json",
      ...(options?.headers ?? {})
    },
    ...options
  });
  if (!response.ok) {
    const body = await response.text();
    let parsedMessage: string | undefined;
    try {
      const parsed = JSON.parse(body) as { message?: string };
      parsedMessage = parsed.message;
    } catch {
      parsedMessage = undefined;
    }
    throw new Error(parsedMessage || body || `Request failed (${response.status})`);
  }
  const bodyText = await response.text();
  return (bodyText ? JSON.parse(bodyText) : {}) as T;
}

export function getStatus() {
  return request<AppStatus>("/status");
}

export function saveGameFolder(path: string, forceSave = false) {
  return request<{ containsExpectedItemFolder: boolean }>("/settings/game-folder", {
    method: "POST",
    body: JSON.stringify({ path, forceSave })
  });
}

export function clearGameFolder() {
  return request<{ message: string }>("/settings/game-folder/clear", { method: "POST" });
}

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
  return request<{ message: string }>("/folders/open/resources", { method: "POST" });
}

export function openItemFolder() {
  return request<{ message: string }>("/folders/open/item", { method: "POST" });
}
