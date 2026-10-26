import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { apiBlobClient, apiClient, ApiError } from "./apiClient";
import { SESSION_EXPIRED_EVENT } from "./authStorage";

const storedValues = new Map<string, string>();

function installBrowserGlobals() {
  storedValues.clear();

  const eventTarget = new EventTarget();
  const localStorage = {
    getItem: vi.fn((key: string) => storedValues.get(key) ?? null),
    setItem: vi.fn((key: string, value: string) => {
      storedValues.set(key, value);
    }),
    removeItem: vi.fn((key: string) => {
      storedValues.delete(key);
    })
  };

  vi.stubGlobal("window", {
    localStorage,
    addEventListener: eventTarget.addEventListener.bind(eventTarget),
    removeEventListener: eventTarget.removeEventListener.bind(eventTarget),
    dispatchEvent: eventTarget.dispatchEvent.bind(eventTarget)
  });
}

function makeJsonResponse(status: number, body: object) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "Content-Type": "application/json"
    }
  });
}

describe("apiClient session expiry handling", () => {
  beforeEach(() => {
    installBrowserGlobals();
    vi.stubGlobal("fetch", vi.fn());
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("clears storage and announces session expiry for authenticated JSON requests that return 401", async () => {
    const onSessionExpired = vi.fn();
    storedValues.set(
      "jobflow.auth.session",
      JSON.stringify({
        token: "expired-token",
        user: {
          email: "candidate@example.com",
          role: "CANDIDATE"
        }
      })
    );
    window.addEventListener(SESSION_EXPIRED_EVENT, onSessionExpired);
    vi.mocked(fetch).mockResolvedValueOnce(
      makeJsonResponse(401, { message: "Token expired" })
    );

    await expect(apiClient("/api/candidate/dashboard"))
      .rejects.toMatchObject(new ApiError("Token expired", 401, { message: "Token expired" }));

    expect(storedValues.has("jobflow.auth.session")).toBe(false);
    expect(onSessionExpired).toHaveBeenCalledTimes(1);
  });

  it("does not announce session expiry for unauthenticated 401 responses", async () => {
    const onSessionExpired = vi.fn();
    storedValues.set("jobflow.auth.session", "existing-session");
    window.addEventListener(SESSION_EXPIRED_EVENT, onSessionExpired);
    vi.mocked(fetch).mockResolvedValueOnce(
      makeJsonResponse(401, { message: "Invalid email or password" })
    );

    await expect(apiClient("/api/auth/login", { authenticated: false }))
      .rejects.toMatchObject(
        new ApiError("Invalid email or password", 401, {
          message: "Invalid email or password"
        })
      );

    expect(storedValues.get("jobflow.auth.session")).toBe("existing-session");
    expect(onSessionExpired).not.toHaveBeenCalled();
  });

  it("announces session expiry for authenticated blob requests that return 401", async () => {
    const onSessionExpired = vi.fn();
    storedValues.set("jobflow.auth.session", "existing-session");
    window.addEventListener(SESSION_EXPIRED_EVENT, onSessionExpired);
    vi.mocked(fetch).mockResolvedValueOnce(
      makeJsonResponse(401, { message: "Token expired" })
    );

    await expect(apiBlobClient("/api/applications/me/12/cv"))
      .rejects.toMatchObject(new ApiError("Token expired", 401, { message: "Token expired" }));

    expect(storedValues.has("jobflow.auth.session")).toBe(false);
    expect(onSessionExpired).toHaveBeenCalledTimes(1);
  });
});
