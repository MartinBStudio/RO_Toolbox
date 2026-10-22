import type {AvailableProfile} from "../types";

export type MetadataFilters = {tags: string[]};
export type MetadataOptions = {tags: string[]};
export const emptyMetadataFilters = (): MetadataFilters => ({tags: []});
const normalize = (value: string | null | undefined) => value?.trim().toLowerCase() ?? "";
const tagsOf = (profile: AvailableProfile) => (profile.tags ?? []).filter(tag => typeof tag === "string" && tag.trim());

export function packageMetadataOptions(profiles: AvailableProfile[]): MetadataOptions {
    const unique = (values: (string | null | undefined)[]) => {
        const options = new Map<string, string>();
        values.forEach(value => {if (value?.trim() && !options.has(normalize(value))) options.set(normalize(value), value.trim());});
        return [...options.values()].sort((a, b) => a.localeCompare(b, undefined, {sensitivity: "base"}));
    };
    return {tags: unique(profiles.flatMap(tagsOf))};
}

export function matchesPackageMetadata(profile: AvailableProfile, filters: MetadataFilters): boolean {
    const tags = new Set(tagsOf(profile).map(normalize));
    return filters.tags.every(tag => tags.has(normalize(tag)));
}

export function groupPackageTags(tags: string[]): {id: string; name: string; tags: {value: string; label: string}[]}[] {
    const groups = new Map<string, {value: string; label: string}[]>();
    const seen = new Set<string>();
    for (const raw of tags) {
        const value = raw.trim();
        if (!value || seen.has(normalize(value))) continue;
        seen.add(normalize(value));
        const colon = value.indexOf(":");
        const categorized = colon > 0 && value.slice(colon + 1).trim().length > 0;
        const key = categorized ? normalize(value.slice(0, colon)) : "";
        const group = groups.get(key) ?? [];
        group.push({value, label: categorized ? value.slice(colon + 1).trim() : value});
        groups.set(key, group);
    }
    return [...groups.entries()].sort(([a], [b]) => a === "" ? 1 : b === "" ? -1 : a.localeCompare(b))
        .map(([key, values]) => ({id: key, name: key ? key[0].toUpperCase() + key.slice(1) : "Other", tags: values.sort((a, b) => a.label.localeCompare(b.label, undefined, {sensitivity: "base"}))}));
}
