import type { UpdateCheckResult } from "../types.ts";
import { request } from "./apiClient.ts";

const UPDATE_API_BASE = "http://localhost:8080/api/update";

export function checkBackendUpdate() {
  return request<UpdateCheckResult>("/update/check");
}

export async function fetchLatestReleaseDownload() {
  const response = await fetch(`${UPDATE_API_BASE}/latest-release/download`);

  if (!response.ok) {
    const body = await response.text();
    let parsedMessage: string | undefined;
    try {
      const parsed = JSON.parse(body) as { message?: string };
      parsedMessage = parsed.message;
    } catch {
      parsedMessage = undefined;
    }
    throw new Error(parsedMessage || body || `Request failed (${response.status})`);
  }

  return response;
}
