import type { UpdateCheckResult } from "../types.ts";
import { request } from "./apiClient.ts";
import { apiFetch } from "./apiClient.ts";

export function checkBackendUpdate() {
  return request<UpdateCheckResult>("/update/check");
}

export async function fetchLatestReleaseDownload() {
  const response = await apiFetch("/update/latest-release/download");

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
