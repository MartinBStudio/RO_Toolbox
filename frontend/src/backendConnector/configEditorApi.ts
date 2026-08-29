import type { ConfigEditorFileState, ConfigEditorStatus } from "../types.ts";
import { request } from "./apiClient.ts";

export function getConfigEditorStatus() {
  return request<ConfigEditorStatus>("/config-editor/status");
}

export function saveConfigEditorFile(fileId: "ignore" | "rose", content: string) {
  return request<ConfigEditorFileState>(`/config-editor/files/${fileId}`, {
    method: "POST",
    body: JSON.stringify({ content })
  });
}

export function openConfigEditorFolder() {
  return request<{ message: string }>("/config-editor/folders/open", { method: "POST" });
}
