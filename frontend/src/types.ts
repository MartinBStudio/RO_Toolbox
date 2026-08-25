export type ProfileInfo = {
  name: string | null;
  author: string | null;
  description: string | null;
  url: string | null;
  createdAt: string | null;
};

export type AvailableProfile = {
  id: string;
  name: string | null;
  author: string | null;
  description: string | null;
  url: string | null;
  createdAt: string | null;
};

export type AppStatus = {
  version: string;
  selectedGameBase: string | null;
  selectedGameItemFolder: string | null;
  installedProfile: ProfileInfo | null;
  downloadedProfiles: string[];
  availableProfiles: AvailableProfile[];
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
