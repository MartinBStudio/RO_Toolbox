import type { UpdateCheckResult } from "../types.ts";
import { request } from "./apiClient.ts";
import { invoke } from "@tauri-apps/api/core";

export function checkBackendUpdate() {
  return request<UpdateCheckResult>("/update/check");
}

export async function fetchLatestReleaseDownload() {
  const apiBase = await invoke<string>("get_backend_url");
  const response = await fetch(`${apiBase}/update/latest-release/download`);

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
