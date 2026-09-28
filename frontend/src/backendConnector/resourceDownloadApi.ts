import { invoke } from "@tauri-apps/api/core";

export type ResourceDownloadProgress = {
  type: "started" | "progress" | "complete" | "error";
  downloadedBytes: number;
  totalBytes: number;
  message?: string;
};

export async function downloadResourceWithProgress(
  path: string,
  onProgress: (progress: ResourceDownloadProgress) => void,
  completeMessage: string
) {
  const apiBase = await invoke<string>("get_backend_url");
  const response = await fetch(`${apiBase}${path}`, { method: "POST" });

  if (!response.ok) {
    const body = await response.text();
    throw new Error(body || `Request failed (${response.status})`);
  }
  if (!response.body) {
    throw new Error("Download progress stream is unavailable.");
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";
  let failedMessage: string | undefined;

  while (true) {
    const { done, value } = await reader.read();
    buffer += decoder.decode(value ?? new Uint8Array(), { stream: !done });
    const lines = buffer.split("\n");
    buffer = lines.pop() ?? "";

    for (const line of lines) {
      const trimmed = line.trim();
      if (!trimmed) {
        continue;
      }
      const event = JSON.parse(trimmed) as ResourceDownloadProgress;
      onProgress(event);
      if (event.type === "error") {
        failedMessage = event.message || "Resource update failed.";
      }
    }

    if (done) {
      break;
    }
  }

  if (failedMessage) {
    throw new Error(failedMessage);
  }

  return { message: completeMessage };
}
