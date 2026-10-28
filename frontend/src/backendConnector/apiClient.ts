import { invoke } from "@tauri-apps/api/core";

type BackendConnection = { url: string; token: string };

export async function backendFetch(path: string, options: RequestInit = {}): Promise<Response> {
  // Only relative backend paths are accepted; never send a bearer token to external URLs.
  if (!path.startsWith("/") || path.startsWith("//") || path.includes("\\") || path.includes("#")) {
    throw new Error("Invalid backend path.");
  }
  const connection = await invoke<BackendConnection>("get_backend_connection");
  const headers = new Headers(options.headers);
  headers.set("Authorization", `Bearer ${connection.token}`);
  return fetch(`${connection.url}${path}`, {
    ...options,
    headers,
    cache: "no-store",
    credentials: "omit",
    redirect: "error"
  });
}

export async function apiFetch(path: string, options?: RequestInit): Promise<Response> {
  return backendFetch(`/api${path}`, options);
}

export async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers);
  headers.set("Content-Type", "application/json");
  const response = await apiFetch(path, { ...options, headers });

  if (!response.ok) {
    const body = await response.text();

    let parsedMessage: string | undefined;

    try {
      const parsed = JSON.parse(body) as { message?: string };
      parsedMessage = parsed.message;
    } catch {
      parsedMessage = undefined;
    }

    throw new Error(
        parsedMessage ||
        body ||
        `Request failed (${response.status})`
    );
  }

  const bodyText = await response.text();

  return (bodyText ? JSON.parse(bodyText) : {}) as T;
}