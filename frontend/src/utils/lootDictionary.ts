import { apiFetch, backendFetch } from "../backendConnector/apiClient.ts";

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

export async function loadItemPreviews(): Promise<Record<string, string[]>> {
  if (cachedPreviews) {
    return cachedPreviews;
  }

  try {
    const response = await apiFetch("/loot/item-previews");
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }
    const data: Record<string, string[]> = await response.json();
    const previews: Record<string, string[]> = {};
    await Promise.all(Object.entries(data).map(async ([key, items]) => {
      previews[key.toUpperCase()] = await Promise.all(
        (Array.isArray(items) ? items : []).map(async (path) => {
          if (!path.startsWith("/ITEM_previews/")) throw new Error("Invalid preview path.");
          const image = await backendFetch(path);
          if (!image.ok) throw new Error(`HTTP ${image.status}`);
          return URL.createObjectURL(await image.blob());
        })
      );
    }));
    cachedPreviews = previews;
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
    const response = await apiFetch("/loot/dictionary");
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
