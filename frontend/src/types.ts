export type ProfileInfo = {
  name: string | null;
  author: string | null;
  description: string | null;
  url: string | null;
  createdAt: string | null;
  version: string | null;
  managedSubfolders: string[];
  disabledManagedSubfolders: string[];
};

export type AvailableProfile = {
  id: string;
  name: string | null;
  author: string | null;
  description: string | null;
  url: string | null;
  createdAt: string | null;
  version: string | null;
  managedSubfolders: string[];
  previewImages: string[];
};

export type AppStatus = {
  version: string;
  lootServiceEndpoint: string;
  combatTextServiceEndpoint: string;
  userInterfaceServiceEndpoint: string;
  selectedGameBase: string | null;
  selectedGameItemFolder: string | null;
  installedProfile: ProfileInfo | null;
  downloadedProfiles: string[];
  availableProfiles: AvailableProfile[];
  combatTextSelectedGameBase: string | null;
  combatTextSelectedGameItemFolder: string | null;
  combatTextInstalledProfile: ProfileInfo | null;
  combatTextDownloadedProfiles: string[];
  combatTextAvailableProfiles: AvailableProfile[];
  userInterfaceSelectedGameBase: string | null;
  userInterfaceSelectedGameItemFolder: string | null;
  userInterfaceInstalledProfile: ProfileInfo | null;
  userInterfaceDownloadedProfiles: string[];
  userInterfaceAvailableProfiles: AvailableProfile[];
};

export type UpdateCheckResult = {
  currentVersion: string;
  releaseVersion: string;
  currentNumericVersion: number;
  releaseNumericVersion: number;
  updateAvailable: boolean;
  releaseUrl: string;
  message: string;
  success: boolean;
  assetUrl: string | null;
};

export type UpdateInstallResult = {
  success: boolean;
  message: string;
};

export type ResourcesUpdateCheckResult = {
  localVersion: string;
  remoteVersion: string;
  localExists: boolean;
  updateAvailable: boolean;
  success: boolean;
  message: string;
};

export type TomlNode = string | number | boolean | null | TomlNode[] | { [key: string]: TomlNode };

export type ConfigEditorFileState = {
  id: "ignore" | "rose";
  fileName: string;
  filePath: string;
  exists: boolean;
  content: string | null;
  parsed: { [key: string]: TomlNode } | null;
  parseError: string | null;
};

export type ConfigEditorStatus = {
  configDir: string;
  configDirExists: boolean;
  files: ConfigEditorFileState[];
};

export type IgnoreListState = {
  names: string[];
  file: ConfigEditorFileState;
};
