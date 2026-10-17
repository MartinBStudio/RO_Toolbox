import {useEffect, useState} from "react";
import {request} from "../backendConnector/apiClient";

export type ResourceType = "loot" | "combatText" | "userInterface" | "buffIcons" | "buffs";

export function usePackageFavorites(resourceType: ResourceType, active: boolean, onMessage: (message: string) => void) {
    const [favorites, setFavorites] = useState<string[]>([]);
    const [sortingFavorites, setSortingFavorites] = useState<string[]>([]);
    const [ready, setReady] = useState(false);
    const [saving, setSaving] = useState(false);
    const [favoritesOnly, setFavoritesOnly] = useState(false);

    useEffect(() => {
        if (!active) return;
        let cancelled = false;
        setReady(false);
        Promise.all([
            request<string[]>(`/settings/favorite-packages?resourceType=${encodeURIComponent(resourceType)}`),
            request<{enabled: boolean}>(`/settings/favorites-only?resourceType=${encodeURIComponent(resourceType)}`)
        ])
            .then(([names, setting]) => {if (!cancelled) {setFavorites(names); setSortingFavorites(names); setFavoritesOnly(setting.enabled); setReady(true);}})
            .catch(error => {if (!cancelled) onMessage(error instanceof Error ? error.message : "Failed to load favorites.");});
        return () => {cancelled = true;};
    }, [resourceType, active]);

    async function saveFavoritesOnly(enabled: boolean) {
        if (!ready || saving) return;
        setSaving(true);
        try {
            const setting = await request<{enabled: boolean}>("/settings/favorites-only", {
                method: "POST",
                body: JSON.stringify({resourceType, enabled})
            });
            setFavoritesOnly(setting.enabled);
        } catch (error) {
            onMessage(error instanceof Error ? error.message : "Failed to save favorites filter.");
        } finally {
            setSaving(false);
        }
    }

    async function toggleFavorite(name: string) {
        if (!ready || saving) return;
        setSaving(true);
        try {
            const favorite = !favorites.includes(name);
            const names = await request<string[]>("/settings/favorite-packages", {
                method: "POST",
                body: JSON.stringify({resourceType, name, favorite})
            });
            setFavorites(names);
            onMessage(`Package "${name}" ${favorite ? "added to" : "removed from"} favorites.`);
        } catch (error) {
            onMessage(error instanceof Error ? error.message : "Failed to save favorite.");
        } finally {
            setSaving(false);
        }
    }

    // Keep the current browsing order stable; refresh favorite ordering on the next open.
    return {isFavorite: (name: string) => favorites.includes(name), isSortingFavorite: (name: string) => sortingFavorites.includes(name), toggleFavorite, favoritesDisabled: !ready || saving, hasFavorites: favorites.length > 0, favoritesOnly: favoritesOnly && favorites.length > 0, setFavoritesOnly: saveFavoritesOnly};
}
