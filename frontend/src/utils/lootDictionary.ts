const DICTIONARY_URLS = [
  "/api/loot/dictionary",
  "http://localhost:8080/api/loot/dictionary"
];

const PREVIEWS_URLS = [
  "/api/loot/item-previews",
  "http://localhost:8080/api/loot/item-previews"
];

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

export function resolvePreviewUrl(path: string): string {
  if (!path) return "";
  if (path.startsWith("data:") || path.startsWith("http://") || path.startsWith("https://")) {
    return path;
  }
  return `http://localhost:8080${path.startsWith("/") ? "" : "/"}${path}`;
}

export async function loadItemPreviews(): Promise<Record<string, string[]>> {
  if (cachedPreviews) {
    return cachedPreviews;
  }

  let lastError: unknown;

  for (const url of PREVIEWS_URLS) {
    try {
      const response = await fetch(url, { cache: "no-store" });
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      const data: Record<string, string[]> = await response.json();
      cachedPreviews = {};
      Object.entries(data).forEach(([key, items]) => {
        cachedPreviews![key.toUpperCase()] = Array.isArray(items)
          ? items.map(resolvePreviewUrl)
          : [];
      });
      return cachedPreviews;
    } catch (error) {
      lastError = error;
    }
  }

  console.error("Error loading item previews:", lastError);
  return {};
}

export async function loadLootDictionary(): Promise<Record<string, LootFolderInfo>> {
  if (cachedDictionary) {
    return cachedDictionary;
  }

  let lastError: unknown;

  for (const url of DICTIONARY_URLS) {
    try {
      const response = await fetch(url, { cache: "no-store" });
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
      lastError = error;
    }
  }

  console.error("Error loading loot dictionary:", lastError);
  return {};
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
