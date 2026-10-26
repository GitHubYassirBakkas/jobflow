import { describe, expect, it } from "vitest";
import {
  getRequestedPathFromLoginState,
  getSessionExpiryMessage
} from "./LoginPage";

describe("LoginPage session state helpers", () => {
  it("extracts the requested path from router state", () => {
    expect(getRequestedPathFromLoginState({
      from: {
        pathname: "/candidate/applications/12"
      }
    })).toBe("/candidate/applications/12");
  });

  it("returns no requested path when router state is missing", () => {
    expect(getRequestedPathFromLoginState(null)).toBeUndefined();
  });

  it("shows a session expiry message only for expired-session redirects", () => {
    expect(getSessionExpiryMessage({ sessionExpired: true }))
      .toBe("Your session expired. Please sign in again.");
    expect(getSessionExpiryMessage({ sessionExpired: false })).toBe("");
    expect(getSessionExpiryMessage(null)).toBe("");
  });
});
