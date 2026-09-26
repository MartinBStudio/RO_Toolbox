import type { ResourcesUpdateCheckResult } from "../types.ts";
import { request } from "./apiClient.ts";

export type LootModelScaleVector = {
  x: number;
  y: number;
  z: number;
};

export type LootModelScaleBounds = {
  min: LootModelScaleVector;
  max: LootModelScaleVector;
  size: LootModelScaleVector;
  largestAxis: number;
};

export type LootModelScaleFile = {
  fileName: string;
  relativePath: string;
  version: number | null;
  flags: number | null;
  vertexCount: number;
  triangleCount: number;
  vertexBounds: LootModelScaleBounds | null;
  headerBounds: LootModelScaleBounds | null;
  error: string | null;
};

export type LootModelScaleFolder = {
  folder: string;
  files: LootModelScaleFile[];
  originalFiles: LootModelScaleFile[];
};

export type LootModelScaleReport = {
  itemFolder: string;
  folders: LootModelScaleFolder[];
};

export type LootModelPreview = {
  folder: string;
  fileName: string | null;
  relativePath: string | null;
  version: number | null;
  flags: number | null;
  vertexCount: number;
  triangleCount: number;
  positions: number[];
  normals: number[];
  uvs: number[];
  colors: number[];
  indices: number[];
  bounds: LootModelScaleBounds | null;
  error: string | null;
};

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

export function checkLootResourcesUpdate() {
  return request<ResourcesUpdateCheckResult>("/loot/check-update");
}

export function getLootModelScales() {
  return request<LootModelScaleReport>("/loot/model-scales");
}

export function getLootModelPreview(folder: string, profileId?: string | null) {
  const params = new URLSearchParams({ folder });
  if (profileId) {
    params.set("profileId", profileId);
  }
  return request<LootModelPreview>(`/loot/model-preview?${params.toString()}`);
}

export type LootModelScaleDirection = "increase" | "decrease" | "reset" | "min" | "max";

export function scaleLootModelFolder(folder: string, direction: LootModelScaleDirection) {
  return request<{ message: string }>("/loot/model-scales/scale", {
    method: "POST",
    body: JSON.stringify({ folder, direction })
  });
}

export function manageInstalledProfile(profileId: string, disabledManagedSubfolders: string[]) {
  return request<{ message: string }>("/loot/manage", {
    method: "POST",
    body: JSON.stringify({ profileId, disabledManagedSubfolders })
  });
}

export function getItemPreviews() {
  return request<Record<string, string[]>>("/loot/item-previews");
}
