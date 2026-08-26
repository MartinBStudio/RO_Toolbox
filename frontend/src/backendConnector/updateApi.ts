import type { UpdateCheckResult } from "../types.ts";
import { request } from "./apiClient.ts";

export function checkBackendUpdate() {
  return request<UpdateCheckResult>("/update/check");
}
