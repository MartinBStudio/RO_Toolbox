import { BACKEND_BASE as BASE } from "./backendBase.ts";

export async function saveGameFolder(
    path: string,
    force: boolean
): Promise<{ containsExpectedItemFolder: boolean }> {
    const res = await fetch(`${BASE}/api/settings/game-folder`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ path, forceSave: force })
    });
    if (!res.ok) {
        const error = await res.json().catch(() => ({}));
        throw new Error((error as { message?: string }).message || "Failed to save game folder.");
    }
    const data = await res.json();
    return { containsExpectedItemFolder: data.containsExpectedFolder ?? false };
}

export async function clearGameFolder(): Promise<void> {
    const res = await fetch(`${BASE}/api/settings/game-folder/clear`, {
        method: "POST"
    });
    if (!res.ok) {
        const error = await res.json().catch(() => ({}));
        throw new Error((error as { message?: string }).message || "Failed to clear game folder.");
    }
}
