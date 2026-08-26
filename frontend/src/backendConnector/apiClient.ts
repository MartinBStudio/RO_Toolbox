const API_BASE = "http://localhost:8080/api";

export async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: {
      "Content-Type": "application/json",
      ...(options?.headers ?? {})
    },
    ...options
  });

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

  const bodyText = await response.text();
  return (bodyText ? JSON.parse(bodyText) : {}) as T;
}
