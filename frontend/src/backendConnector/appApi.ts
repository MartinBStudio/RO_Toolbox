import type { AppStatus } from "../types.ts";
import { request } from "./apiClient.ts";

type ApiAppStatus = {
  version: string;
  troseRunning: boolean;
  lootService: {
    endpoint: string;
  };
  combatTextService: {
    endpoint: string;
  };
  userInterfaceService: {
    endpoint: string;
  };
  buffIconsService: {
    endpoint: string;
  };
  buffsService: {
    endpoint: string;
  };
};

type ApiLootStatus = {
  selectedGameBase: string | null;
  selectedGameItemFolder: string | null;
  installedProfile: AppStatus["installedProfile"];
  downloadedProfiles: string[];
  availableProfiles: AppStatus["availableProfiles"];
};

type ApiCombatTextStatus = {
  selectedGameBase: string | null;
  selectedGameItemFolder: string | null;
  installedProfile: AppStatus["combatTextInstalledProfile"];
  downloadedProfiles: string[];
  availableProfiles: AppStatus["combatTextAvailableProfiles"];
};

type ApiUserInterfaceStatus = {
  selectedGameBase: string | null;
  selectedGameItemFolder: string | null;
  installedProfile: AppStatus["userInterfaceInstalledProfile"];
  downloadedProfiles: string[];
  availableProfiles: AppStatus["userInterfaceAvailableProfiles"];
};

type ApiBuffIconsStatus = {
  selectedGameBase: string | null;
  selectedGameItemFolder: string | null;
  installedProfile: AppStatus["buffIconsInstalledProfile"];
  downloadedProfiles: string[];
  availableProfiles: AppStatus["buffIconsAvailableProfiles"];
};

type ApiBuffsStatus = {
  selectedGameBase: string | null;
  selectedGameItemFolder: string | null;
  installedProfile: AppStatus["buffsInstalledProfile"];
  downloadedProfiles: string[];
  availableProfiles: AppStatus["buffsAvailableProfiles"];
};

function asNullableString(value: unknown): string | null {
  return typeof value === "string" ? value : null;
}

function asString(value: unknown, fallback = ""): string {
  return typeof value === "string" ? value : fallback;
}

function asStringArray(value: unknown): string[] {
  if (!Array.isArray(value)) {
    return [];
  }
  return value.filter((item): item is string => typeof item === "string");
}

function asProfileInfo(value: unknown): AppStatus["installedProfile"] {
  if (!value || typeof value !== "object") {
    return null;
  }
  const record = value as Record<string, unknown>;
  return {
    name: asNullableString(record.name),
    author: asNullableString(record.author),
    description: asNullableString(record.description),
    url: asNullableString(record.url),
    createdAt: asNullableString(record.createdAt),
    version: asNullableString(record.version),
    managedSubfolders: asStringArray(record.managedSubfolders),
    disabledManagedSubfolders: asStringArray(record.disabledManagedSubfolders)
  };
}

function asAvailableProfiles(value: unknown): AppStatus["availableProfiles"] {
  if (!Array.isArray(value)) {
    return [];
  }
  return value
    .filter((item): item is Record<string, unknown> => Boolean(item) && typeof item === "object")
    .map((profile) => ({
      id: asString(profile.id),
      name: asNullableString(profile.name),
      author: asNullableString(profile.author),
      description: asNullableString(profile.description),
      url: asNullableString(profile.url),
      createdAt: asNullableString(profile.createdAt),
      version: asNullableString(profile.version),
      managedSubfolders: asStringArray(profile.managedSubfolders),
      previewImages: asStringArray(profile.previewImages)
    }))
    .filter((profile) => profile.id.length > 0);
}

export function getStatus() {
  return Promise.all([
    request<ApiAppStatus>("/status"),
    request<ApiLootStatus>("/loot/status"),
    request<ApiCombatTextStatus>("/combattext/status"),
    request<ApiUserInterfaceStatus>("/userinterface/status"),
    request<ApiBuffIconsStatus>("/bufficons/status"),
    request<ApiBuffsStatus>("/buffs/status")
  ]).then(([appStatus, lootStatus, combatTextStatus, userInterfaceStatus, buffIconsStatus, buffsStatus]) => ({
    version: asString(appStatus?.version),
    troseRunning: Boolean(appStatus?.troseRunning),
    lootServiceEndpoint: asString(appStatus?.lootService?.endpoint),
    combatTextServiceEndpoint: asString(appStatus?.combatTextService?.endpoint),
    userInterfaceServiceEndpoint: asString(appStatus?.userInterfaceService?.endpoint),
    buffIconsServiceEndpoint: asString(appStatus?.buffIconsService?.endpoint),
    buffsServiceEndpoint: asString(appStatus?.buffsService?.endpoint),
    selectedGameBase: asNullableString(lootStatus?.selectedGameBase),
    selectedGameItemFolder: asNullableString(lootStatus?.selectedGameItemFolder),
    installedProfile: asProfileInfo(lootStatus?.installedProfile),
    downloadedProfiles: asStringArray(lootStatus?.downloadedProfiles),
    availableProfiles: asAvailableProfiles(lootStatus?.availableProfiles),
    combatTextSelectedGameBase: asNullableString(combatTextStatus?.selectedGameBase),
    combatTextSelectedGameItemFolder: asNullableString(combatTextStatus?.selectedGameItemFolder),
    combatTextInstalledProfile: asProfileInfo(combatTextStatus?.installedProfile),
    combatTextDownloadedProfiles: asStringArray(combatTextStatus?.downloadedProfiles),
    combatTextAvailableProfiles: asAvailableProfiles(combatTextStatus?.availableProfiles),
    userInterfaceSelectedGameBase: asNullableString(userInterfaceStatus?.selectedGameBase),
    userInterfaceSelectedGameItemFolder: asNullableString(userInterfaceStatus?.selectedGameItemFolder),
    userInterfaceInstalledProfile: asProfileInfo(userInterfaceStatus?.installedProfile),
    userInterfaceDownloadedProfiles: asStringArray(userInterfaceStatus?.downloadedProfiles),
    userInterfaceAvailableProfiles: asAvailableProfiles(userInterfaceStatus?.availableProfiles),
    buffIconsSelectedGameBase: asNullableString(buffIconsStatus?.selectedGameBase),
    buffIconsSelectedGameItemFolder: asNullableString(buffIconsStatus?.selectedGameItemFolder),
    buffIconsInstalledProfile: asProfileInfo(buffIconsStatus?.installedProfile),
    buffIconsDownloadedProfiles: asStringArray(buffIconsStatus?.downloadedProfiles),
    buffIconsAvailableProfiles: asAvailableProfiles(buffIconsStatus?.availableProfiles),
    buffsSelectedGameBase: asNullableString(buffsStatus?.selectedGameBase),
    buffsSelectedGameItemFolder: asNullableString(buffsStatus?.selectedGameItemFolder),
    buffsInstalledProfile: asProfileInfo(buffsStatus?.installedProfile),
    buffsDownloadedProfiles: asStringArray(buffsStatus?.downloadedProfiles),
    buffsAvailableProfiles: asAvailableProfiles(buffsStatus?.availableProfiles)
  }));
}

export function drainNotifications() {
  return request<{ messages?: unknown }>("/notifications/drain")
    .then((response) => asStringArray(response?.messages));
}
