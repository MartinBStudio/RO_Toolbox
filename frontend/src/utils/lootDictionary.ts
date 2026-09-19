import { invoke } from "@tauri-apps/api/core";

async function getApiBase(): Promise<string> {
  return await invoke<string>("get_backend_url");
}

export interface LootFolderInfo {
  key: string;
  label: string;
  description?: string;
  confidence?: string;
  examples?: string[];
  previews?: string[];
}

let cachedDictionary: Record<string, LootFolderInfo> | null = null;
let cachedPreviews: Record<string, string[]> | null = null;

export function resolvePreviewUrl(path: string, apiBase?: string): string {
  if (!path) return "";
  if (path.startsWith("data:") || path.startsWith("http://") || path.startsWith("https://")) {
    return path;
  }
  if (!apiBase) {
    return path;
  }
  const staticBase = apiBase.endsWith("/api") ? apiBase.slice(0, -4) : apiBase;
  return `${staticBase}${path.startsWith("/") ? "" : "/"}${path}`;
}

export async function loadItemPreviews(): Promise<Record<string, string[]>> {
  if (cachedPreviews) {
    return cachedPreviews;
  }

  try {
    const apiBase = await getApiBase();
    const response = await fetch(`${apiBase}/loot/item-previews`, { cache: "no-store" });
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }
    const data: Record<string, string[]> = await response.json();
    cachedPreviews = {};
    Object.entries(data).forEach(([key, items]) => {
      cachedPreviews![key.toUpperCase()] = Array.isArray(items)
        ? items.map((path) => resolvePreviewUrl(path, apiBase))
        : [];
    });
    return cachedPreviews;
  } catch (error) {
    console.error("Error loading item previews:", error);
    return {};
  }
}

export async function loadLootDictionary(): Promise<Record<string, LootFolderInfo>> {
  if (cachedDictionary) {
    return cachedDictionary;
  }

  try {
    const apiBase = await getApiBase();
    const response = await fetch(`${apiBase}/loot/dictionary`, { cache: "no-store" });
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }
    const items: LootFolderInfo[] = await response.json();
    cachedDictionary = {};
    items.forEach((item) => {
      cachedDictionary![item.key.toUpperCase()] = item;
    });
    return cachedDictionary;
  } catch (error) {
    console.error("Error loading loot dictionary:", error);
    return {};
  }
}

export function getFolderLabel(folderKey: string, dictionary: Record<string, LootFolderInfo>): string {
  const key = folderKey.toUpperCase();
  return dictionary[key]?.label || folderKey;
}

export function getFolderDescription(folderKey: string, dictionary: Record<string, LootFolderInfo>): string | undefined {
  const key = folderKey.toUpperCase();
  return dictionary[key]?.description;
}

export function getFolderPreviews(
  folderKey: string,
  previews: Record<string, string[]>
): string[] {
  const key = folderKey.toUpperCase();
  return previews[key] || [];
}
