import usefulStuffLinksData from "../data/useful-stuff-links.json";

export interface UsefulStuffLink {
  id: string;
  title: string;
  url: string;
  description: string;
  accentFrom: string;
  accentTo: string;
}

let cachedUsefulStuffLinks: UsefulStuffLink[] | null = null;

function isNonEmptyString(value: unknown): value is string {
  return typeof value === "string" && value.trim().length > 0;
}

function readStringField(record: Record<string, unknown>, field: keyof UsefulStuffLink, index: number) {
  const value = record[field];
  if (!isNonEmptyString(value)) {
    throw new Error(`Useful link #${index + 1} is missing "${field}".`);
  }
  return value.trim();
}

function parseUsefulStuffLink(item: unknown, index: number): UsefulStuffLink {
  if (!item || typeof item !== "object") {
    throw new Error(`Useful link #${index + 1} is invalid.`);
  }

  const record = item as Record<string, unknown>;

  return {
    id: readStringField(record, "id", index),
    title: readStringField(record, "title", index),
    url: readStringField(record, "url", index),
    description: readStringField(record, "description", index),
    accentFrom: readStringField(record, "accentFrom", index),
    accentTo: readStringField(record, "accentTo", index)
  };
}

export function loadUsefulStuffLinks() {
  if (!cachedUsefulStuffLinks) {
    const payload: unknown = usefulStuffLinksData;
    if (!Array.isArray(payload)) {
      throw new Error("Useful links file must contain an array.");
    }

    const links = payload.map((item, index) => parseUsefulStuffLink(item, index));
    if (links.length === 0) {
      throw new Error("Useful links file is empty.");
    }
    cachedUsefulStuffLinks = links;
  }

  return Promise.resolve(cachedUsefulStuffLinks);
}
