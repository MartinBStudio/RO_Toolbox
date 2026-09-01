import type { AvailableProfile, ProfileInfo } from "./types.ts";

export type ProfileOption = {
  profile: AvailableProfile;
  label: string;
};

export type ProfileOptionGroup = {
  label: string;
  options: ProfileOption[];
};

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

export function buildProfileOptionGroups(profiles: AvailableProfile[]): ProfileOptionGroup[] {
  if (profiles.length === 0) {
    return [];
  }

  const parsed = profiles.map((profile) => {
    const fullLabel = resolveProfileName(profile.name, profile.id).trim();
    const tokens = extractProfileTokens(fullLabel);
    return {
      profile,
      fullLabel,
      tokens
    };
  });

  const groupKeywordCounts = new Map<string, number>();
  const groupKeywordDisplay = new Map<string, string>();
  for (const item of parsed) {
    const uniqueKeywords = new Set<string>();
    for (const token of item.tokens) {
      if (!isGroupKeywordCandidate(token.normalized)) {
        continue;
      }
      uniqueKeywords.add(token.normalized);
      if (!groupKeywordDisplay.has(token.normalized)) {
        groupKeywordDisplay.set(token.normalized, token.original);
      }
    }
    for (const keyword of uniqueKeywords) {
      groupKeywordCounts.set(keyword, (groupKeywordCounts.get(keyword) ?? 0) + 1);
    }
  }

  const groupedByLabel = new Map<string, ProfileOption[]>();
  const orderedGroupLabels: string[] = [];
  const otherOptions: ProfileOption[] = [];

  for (const item of parsed) {
    const groupKeyword = selectBestGroupKeyword(item.tokens, groupKeywordCounts);
    if (!groupKeyword) {
      otherOptions.push({
        profile: item.profile,
        label: capitalizeFirstLetter(item.fullLabel) ?? item.fullLabel
      });
      continue;
    }

    const groupLabelRaw = groupKeywordDisplay.get(groupKeyword) ?? groupKeyword;
    const groupLabel = capitalizeFirstLetter(groupLabelRaw) ?? groupLabelRaw;
    const variantLabel = buildVariantLabel(item.fullLabel, groupKeyword);
    if (!groupedByLabel.has(groupLabel)) {
      groupedByLabel.set(groupLabel, []);
      orderedGroupLabels.push(groupLabel);
    }
    groupedByLabel.get(groupLabel)!.push({
      profile: item.profile,
      label: capitalizeFirstLetter(variantLabel) ?? variantLabel
    });
  }

  if (orderedGroupLabels.length === 0) {
    return [{
      label: "",
      options: otherOptions
    }];
  }

  const groups: ProfileOptionGroup[] = orderedGroupLabels.map((label) => ({
    label,
    options: groupedByLabel.get(label) ?? []
  }));
  if (otherOptions.length > 0) {
    groups.push({
      label: "Other",
      options: otherOptions
    });
  }
  return groups;
}

const PROFILE_GROUP_STOPWORDS = new Set([
  "small",
  "sm",
  "medium",
  "med",
  "large",
  "larg",
  "lrg",
  "xl",
  "xxl",
  "xxxl",
  "critical",
  "crit",
  "only",
  "mode",
  "profile",
  "profiles",
  "loot",
  "model",
  "models",
  "combat",
  "text",
  "interface",
  "ui",
  "pack",
  "set",
  "v",
  "ver",
  "version"
]);

type ProfileToken = {
  original: string;
  normalized: string;
  index: number;
};

function extractProfileTokens(label: string): ProfileToken[] {
  const matches = [...label.matchAll(/[A-Za-z0-9]+/g)];
  return matches
    .map((match, index) => {
      const original = match[0];
      const normalized = original.toLowerCase();
      return { original, normalized, index };
    })
    .filter((token) => token.normalized.length > 0);
}

function isGroupKeywordCandidate(keyword: string) {
  if (keyword.length < 3) {
    return false;
  }
  if (/^\d+$/.test(keyword)) {
    return false;
  }
  return !PROFILE_GROUP_STOPWORDS.has(keyword);
}

function selectBestGroupKeyword(tokens: ProfileToken[], counts: Map<string, number>) {
  let bestKeyword: string | null = null;
  let bestCount = 0;
  let bestIndex = Number.MAX_SAFE_INTEGER;

  for (const token of tokens) {
    if (!isGroupKeywordCandidate(token.normalized)) {
      continue;
    }
    const count = counts.get(token.normalized) ?? 0;
    if (count < 2) {
      continue;
    }
    if (
      count > bestCount
      || (count === bestCount && token.index < bestIndex)
    ) {
      bestKeyword = token.normalized;
      bestCount = count;
      bestIndex = token.index;
    }
  }

  return bestKeyword;
}

function buildVariantLabel(fullLabel: string, groupKeyword: string) {
  const keywordPattern = new RegExp(`\\b${escapeRegExp(groupKeyword)}\\b`, "ig");
  const withoutKeyword = fullLabel.replace(keywordPattern, " ");
  const compacted = withoutKeyword
    .replace(/\s{2,}/g, " ")
    .replace(/\s*[-–—|:]\s*/g, " ")
    .trim();

  if (!compacted) {
    return fullLabel;
  }
  return compacted;
}

function escapeRegExp(value: string) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

function normalize(value: string | null | undefined) {
  if (!value) {
    return null;
  }
  const trimmed = value.trim().toLowerCase();
  return trimmed || null;
}
