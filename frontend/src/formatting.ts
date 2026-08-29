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
