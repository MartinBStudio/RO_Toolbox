import type { UpdateCheckResult } from "../types";
import { BACKEND_BASE as BASE } from "./backendBase.ts";

export async function checkBackendUpdate(): Promise<UpdateCheckResult> {
    const res = await fetch(`${BASE}/api/update/check`);
    if (!res.ok) {
        const error = await res.json().catch(() => ({}));
        throw new Error((error as { message?: string }).message || "Update check failed.");
    }
    return res.json();
}
