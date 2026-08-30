import type { ConfigEditorFileState, ConfigEditorStatus, IgnoreListState } from "../types.ts";
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

export function getIgnoreList() {
  return request<IgnoreListState>("/config-editor/ignore");
}

export function addIgnoreListEntry(name: string) {
  return request<IgnoreListState>("/config-editor/ignore", {
    method: "POST",
    body: JSON.stringify({ name })
  });
}

export function deleteIgnoreListEntry(name: string) {
  return request<IgnoreListState>("/config-editor/ignore", {
    method: "DELETE",
    body: JSON.stringify({ name })
  });
}
