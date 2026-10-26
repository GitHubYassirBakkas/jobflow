import { expireStoredSession, loadStoredSession } from "./authStorage";

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

type ApiOptions = RequestInit & {
  authenticated?: boolean;
};

export class ApiError extends Error {
  status: number;
  details: unknown;

  constructor(message: string, status: number, details?: unknown) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.details = details;
  }
}

async function parseResponse(response: Response) {
  const contentType = response.headers.get("content-type");

  if (response.status === 204) {
    return null;
  }

  if (contentType?.includes("application/json")) {
    return response.json();
  }

  return response.text();
}

async function request(path: string, options: ApiOptions = {}) {
  const { authenticated = true, headers, body, ...requestOptions } = options;
  const requestHeaders = new Headers(headers);
  const isFormData = typeof FormData !== "undefined" && body instanceof FormData;

  if (body && !isFormData && !requestHeaders.has("Content-Type")) {
    requestHeaders.set("Content-Type", "application/json");
  }

  if (authenticated) {
    const session = loadStoredSession();

    if (session?.token) {
      requestHeaders.set("Authorization", `Bearer ${session.token}`);
    }
  }

  return fetch(`${API_BASE_URL}${path}`, {
    ...requestOptions,
    headers: requestHeaders,
    body
  });
}

function handleUnauthorizedResponse(response: Response, authenticated: boolean) {
  if (response.status === 401 && authenticated) {
    expireStoredSession();
  }
}

async function throwApiError(response: Response, authenticated: boolean) {
  const data = await parseResponse(response);

  handleUnauthorizedResponse(response, authenticated);

  const message =
    typeof data === "string"
      ? data
      : data && typeof data === "object" && "message" in data
        ? String(data.message)
        : "Something went wrong. Please try again.";

  throw new ApiError(message, response.status, data);
}

export async function apiClient<T>(path: string, options: ApiOptions = {}): Promise<T> {
  const authenticated = options.authenticated !== false;
  const response = await request(path, options);

  const data = await parseResponse(response);

  if (!response.ok) {
    handleUnauthorizedResponse(response, authenticated);

    const message =
      typeof data === "string"
        ? data
        : data && typeof data === "object" && "message" in data
          ? String(data.message)
          : "Something went wrong. Please try again.";

    throw new ApiError(message, response.status, data);
  }

  return data as T;
}

export async function apiBlobClient(path: string, options: ApiOptions = {}): Promise<Blob> {
  const authenticated = options.authenticated !== false;
  const response = await request(path, options);

  if (!response.ok) {
    await throwApiError(response, authenticated);
  }

  return response.blob();
}
