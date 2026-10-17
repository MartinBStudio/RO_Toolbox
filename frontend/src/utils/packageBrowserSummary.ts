import type {AvailableProfile, ProfileInfo} from "../types";
import {isProfileAlreadyInstalled, resolveProfileName} from "../formatting";

export function packageBrowserSummary(profiles: AvailableProfile[], visible: AvailableProfile[], installed: ProfileInfo | null | undefined, isFavorite: (name: string) => boolean, favoritesOnly: boolean): string {
    const count = (value: number, singular: string, plural = `${singular}s`) => `${value} ${value === 1 ? singular : plural}`;
    if (!favoritesOnly) {
        return visible.length === profiles.length
            ? `Displaying ${count(profiles.length, "package")} in total.`
            : `Displaying ${visible.length} of ${count(profiles.length, "package")}.`;
    }
    const favorite = (profile: AvailableProfile) => isFavorite(resolveProfileName(profile.name, profile.id));
    const shownFavorites = visible.filter(favorite).length;
    const installedOnly = visible.filter(profile => !favorite(profile) && isProfileAlreadyInstalled(profile, installed)).length;
    return `Displaying ${installedOnly ? `${installedOnly} installed ` : ""}${count(shownFavorites, "favorite")} out of total ${profiles.length}`;
}
