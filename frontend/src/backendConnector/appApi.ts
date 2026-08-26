import type { AppStatus } from "../types.ts";
import { request } from "./apiClient.ts";

type ApiAppStatus = {
  version: string;
  lootService: {
    endpoint: string;
  };
  combatTextService: {
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

export function getStatus() {
  return Promise.all([
    request<ApiAppStatus>("/status"),
    request<ApiLootStatus>("/loot/status"),
    request<ApiCombatTextStatus>("/combattext/status")
  ]).then(([appStatus, lootStatus, combatTextStatus]) => ({
    version: appStatus.version,
    lootServiceEndpoint: appStatus.lootService.endpoint,
    combatTextServiceEndpoint: appStatus.combatTextService.endpoint,
    selectedGameBase: lootStatus.selectedGameBase,
    selectedGameItemFolder: lootStatus.selectedGameItemFolder,
    installedProfile: lootStatus.installedProfile,
    downloadedProfiles: lootStatus.downloadedProfiles,
    availableProfiles: lootStatus.availableProfiles,
    combatTextSelectedGameBase: combatTextStatus.selectedGameBase,
    combatTextSelectedGameItemFolder: combatTextStatus.selectedGameItemFolder,
    combatTextInstalledProfile: combatTextStatus.installedProfile,
    combatTextDownloadedProfiles: combatTextStatus.downloadedProfiles,
    combatTextAvailableProfiles: combatTextStatus.availableProfiles
  }));
}
