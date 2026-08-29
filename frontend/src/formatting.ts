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
