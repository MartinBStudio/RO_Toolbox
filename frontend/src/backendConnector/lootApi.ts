import { BACKEND_BASE as BASE } from "./backendBase.ts";

export async function downloadProfiles(): Promise<void> {
    const res = await fetch(`${BASE}/api/loot/download`, { method: "POST" });
    if (!res.ok) {
        const error = await res.json().catch(() => ({}));
        throw new Error((error as { message?: string }).message || "Failed to download profiles.");
    }
}

export async function installProfile(profileId: string): Promise<void> {
    const res = await fetch(`${BASE}/api/loot/install`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ profileId })
    });
    if (!res.ok) {
        const error = await res.json().catch(() => ({}));
        throw new Error((error as { message?: string }).message || "Failed to install profile.");
    }
}

export async function clearResources(): Promise<void> {
    const res = await fetch(`${BASE}/api/loot/clear-resources`, { method: "POST" });
    if (!res.ok) {
        const error = await res.json().catch(() => ({}));
        throw new Error((error as { message?: string }).message || "Failed to clear resources.");
    }
}

export async function clearInstalled(): Promise<void> {
    const res = await fetch(`${BASE}/api/loot/clear-installed`, { method: "POST" });
    if (!res.ok) {
        const error = await res.json().catch(() => ({}));
        throw new Error((error as { message?: string }).message || "Failed to clear installed models.");
    }
}

export async function openResourcesFolder(): Promise<void> {
    const res = await fetch(`${BASE}/api/loot/folders/open/resources`, { method: "POST" });
    if (!res.ok) {
        const error = await res.json().catch(() => ({}));
        throw new Error((error as { message?: string }).message || "Failed to open resources folder.");
    }
}

export async function openItemFolder(): Promise<void> {
    const res = await fetch(`${BASE}/api/loot/folders/open/item`, { method: "POST" });
    if (!res.ok) {
        const error = await res.json().catch(() => ({}));
        throw new Error((error as { message?: string }).message || "Failed to open item folder.");
    }
}
