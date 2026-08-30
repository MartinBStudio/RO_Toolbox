import type { AvailableProfile, ProfileInfo } from "./types.ts";

export function capitalizeFirstLetter(value: string | null | undefined) {
  if (!value) return value;
  return value.charAt(0).toUpperCase() + value.slice(1);
}

export function formatManifestVersion(version: string | null | undefined) {
  return version ? `v${version}` : null;
}

export function joinMeta(parts: Array<string | null | undefined>, separator = " • ") {
  return parts.filter((part): part is string => Boolean(part)).join(separator);
}

export function resolveProfileName(name: string | null | undefined, fallback: string): string {
  const resolved = capitalizeFirstLetter(name || fallback);
  return resolved || fallback;
}

export function buildProfileMeta({
  version,
  author,
  createdAt,
  separator = " • "
}: {
  version?: string | null;
  author?: string | null;
  createdAt?: string | null;
  separator?: string;
}) {
  return joinMeta([
    formatManifestVersion(version),
    author ? `by ${author}` : null,
    createdAt
  ], separator);
}

export function isProfileAlreadyInstalled(
  selectedProfile: AvailableProfile | null | undefined,
  installedProfile: ProfileInfo | null | undefined
) {
  if (!selectedProfile || !installedProfile) {
    return false;
  }

  const selectedVersion = normalize(selectedProfile.version);
  const installedVersion = normalize(installedProfile.version);
  if (!selectedVersion || !installedVersion || selectedVersion !== installedVersion) {
    return false;
  }

  const selectedName = normalize(selectedProfile.name);
  const installedName = normalize(installedProfile.name);
  if (selectedName && installedName) {
    return selectedName === installedName;
  }

  const selectedUrl = normalize(selectedProfile.url);
  const installedUrl = normalize(installedProfile.url);
  if (selectedUrl && installedUrl) {
    return selectedUrl === installedUrl;
  }

  return true;
}

function normalize(value: string | null | undefined) {
  if (!value) {
    return null;
  }
  const trimmed = value.trim().toLowerCase();
  return trimmed || null;
}
