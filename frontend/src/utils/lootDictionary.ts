const DICTIONARY_URLS = [
  "/api/loot/dictionary",
  "http://localhost:8080/api/loot/dictionary"
];

export interface LootFolderInfo {
  key: string;
  label: string;
  description?: string;
  confidence?: string;
  examples?: string[];
}

let cachedDictionary: Record<string, LootFolderInfo> | null = null;

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
