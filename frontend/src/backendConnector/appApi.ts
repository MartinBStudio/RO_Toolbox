import type { AppStatus } from "../types";
import { BACKEND_BASE as BASE } from "./backendBase.ts";

export async function getStatus(): Promise<AppStatus> {
    const [statusRes, lootRes] = await Promise.all([
        fetch(`${BASE}/api/status`),
        fetch(`${BASE}/api/loot/status`)
    ]);

    if (!statusRes.ok) {
        throw new Error(`App status request failed: ${statusRes.status}`);
    }
    if (!lootRes.ok) {
        throw new Error(`Loot status request failed: ${lootRes.status}`);
    }

    const statusData = await statusRes.json();
    const lootData = await lootRes.json();

    return {
        version: statusData.version ?? "",
        lootServiceEndpoint: statusData.lootService?.endpoint ?? "/api/loot",
        selectedGameBase: lootData.selectedGameBase ?? null,
        selectedGameItemFolder: lootData.selectedGameItemFolder ?? null,
        installedProfile: lootData.installedProfile ?? null,
        downloadedProfiles: lootData.downloadedProfiles ?? [],
        availableProfiles: lootData.availableProfiles ?? []
    };
}
