import type {AvailableProfile, ProfileInfo} from "../types";
import {isProfileAlreadyInstalled, resolveProfileName} from "../formatting.ts";
import {matchesPackageMetadata, type MetadataFilters} from "./packageMetadata.ts";

type Filters = {
    installedProfile?: ProfileInfo | null;
    search: string;
    favoritesOnly: boolean;
    isFavorite: (name: string) => boolean;
    metadata: MetadataFilters;
};

export function filterPackageProfiles(profiles: AvailableProfile[], filters: Filters): AvailableProfile[] {
    const search = filters.search.trim().toLowerCase();
    return profiles.filter(profile => {
        if (isProfileAlreadyInstalled(profile, filters.installedProfile)) return true;
        const name = resolveProfileName(profile.name, profile.id);
        return (!filters.favoritesOnly || filters.isFavorite(name))
            && name.toLowerCase().includes(search)
            && matchesPackageMetadata(profile, filters.metadata);
    });
}
