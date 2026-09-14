import type { AppStatus } from "../types.ts";
import { request } from "./apiClient.ts";

type ApiAppStatus = {
  version: string;
  troseRunning: boolean;
};

interface ApiResourceServiceStatus {
  selectedGameBase: string | null;
  selectedGameItemFolder: string | null;
  installedProfile: AppStatus["installedProfile"];
  downloadedProfiles: string[];
  availableProfiles: AppStatus["availableProfiles"];
}

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

type ResourceServiceStatus = Pick<
  AppStatus,
  "selectedGameBase" | "selectedGameItemFolder" | "installedProfile" | "downloadedProfiles" | "availableProfiles"
>;

function asResourceServiceStatus(status: ApiResourceServiceStatus): ResourceServiceStatus {
  return {
    selectedGameBase: asNullableString(status.selectedGameBase),
    selectedGameItemFolder: asNullableString(status.selectedGameItemFolder),
    installedProfile: asProfileInfo(status.installedProfile),
    downloadedProfiles: asStringArray(status.downloadedProfiles),
    availableProfiles: asAvailableProfiles(status.availableProfiles)
  };
}

function asCombatTextStatus(status: ApiResourceServiceStatus): Pick<
  AppStatus,
  | "combatTextSelectedGameBase"
  | "combatTextSelectedGameItemFolder"
  | "combatTextInstalledProfile"
  | "combatTextDownloadedProfiles"
  | "combatTextAvailableProfiles"
> {
  const resourceStatus = asResourceServiceStatus(status);
  return {
    combatTextSelectedGameBase: resourceStatus.selectedGameBase,
    combatTextSelectedGameItemFolder: resourceStatus.selectedGameItemFolder,
    combatTextInstalledProfile: resourceStatus.installedProfile,
    combatTextDownloadedProfiles: resourceStatus.downloadedProfiles,
    combatTextAvailableProfiles: resourceStatus.availableProfiles
  };
}

function asUserInterfaceStatus(status: ApiResourceServiceStatus): Pick<
  AppStatus,
  | "userInterfaceSelectedGameBase"
  | "userInterfaceSelectedGameItemFolder"
  | "userInterfaceInstalledProfile"
  | "userInterfaceDownloadedProfiles"
  | "userInterfaceAvailableProfiles"
> {
  const resourceStatus = asResourceServiceStatus(status);
  return {
    userInterfaceSelectedGameBase: resourceStatus.selectedGameBase,
    userInterfaceSelectedGameItemFolder: resourceStatus.selectedGameItemFolder,
    userInterfaceInstalledProfile: resourceStatus.installedProfile,
    userInterfaceDownloadedProfiles: resourceStatus.downloadedProfiles,
    userInterfaceAvailableProfiles: resourceStatus.availableProfiles
  };
}

function asBuffIconsStatus(status: ApiResourceServiceStatus): Pick<
  AppStatus,
  | "buffIconsSelectedGameBase"
  | "buffIconsSelectedGameItemFolder"
  | "buffIconsInstalledProfile"
  | "buffIconsDownloadedProfiles"
  | "buffIconsAvailableProfiles"
> {
  const resourceStatus = asResourceServiceStatus(status);
  return {
    buffIconsSelectedGameBase: resourceStatus.selectedGameBase,
    buffIconsSelectedGameItemFolder: resourceStatus.selectedGameItemFolder,
    buffIconsInstalledProfile: resourceStatus.installedProfile,
    buffIconsDownloadedProfiles: resourceStatus.downloadedProfiles,
    buffIconsAvailableProfiles: resourceStatus.availableProfiles
  };
}

function asBuffsStatus(status: ApiResourceServiceStatus): Pick<
  AppStatus,
  | "buffsSelectedGameBase"
  | "buffsSelectedGameItemFolder"
  | "buffsInstalledProfile"
  | "buffsDownloadedProfiles"
  | "buffsAvailableProfiles"
> {
  const resourceStatus = asResourceServiceStatus(status);
  return {
    buffsSelectedGameBase: resourceStatus.selectedGameBase,
    buffsSelectedGameItemFolder: resourceStatus.selectedGameItemFolder,
    buffsInstalledProfile: resourceStatus.installedProfile,
    buffsDownloadedProfiles: resourceStatus.downloadedProfiles,
    buffsAvailableProfiles: resourceStatus.availableProfiles
  };
}

export async function getStatus(): Promise<AppStatus> {
  const [appStatus, lootStatus, combatTextStatus, userInterfaceStatus, buffIconsStatus, buffsStatus] = await Promise.all([
    request<ApiAppStatus>("/status"),
    request<ApiResourceServiceStatus>("/loot/status"),
    request<ApiResourceServiceStatus>("/combattext/status"),
    request<ApiResourceServiceStatus>("/userinterface/status"),
    request<ApiResourceServiceStatus>("/bufficons/status"),
    request<ApiResourceServiceStatus>("/buffs/status")
  ]);

  return {
    version: asString(appStatus?.version),
    troseRunning: Boolean(appStatus?.troseRunning),
    ...asResourceServiceStatus(lootStatus),
    ...asCombatTextStatus(combatTextStatus),
    ...asUserInterfaceStatus(userInterfaceStatus),
    ...asBuffIconsStatus(buffIconsStatus),
    ...asBuffsStatus(buffsStatus)
  };
}

export function drainNotifications() {
  return request<{ messages?: unknown }>("/notifications/drain")
    .then((response) => asStringArray(response?.messages));
}
