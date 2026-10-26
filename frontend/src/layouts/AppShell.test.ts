import { describe, expect, it } from "vitest";
import { getUserDisplayName } from "./AppShell";
import type { AuthUser } from "../types/auth";

function makeUser(firstName: string, lastName: string): AuthUser {
  return {
    id: 1,
    firstName,
    lastName,
    email: "candidate@example.com",
    role: "CANDIDATE",
    createdAt: "2026-10-10T10:00:00"
  };
}

describe("AppShell account helpers", () => {
  it("formats the signed-in user display name", () => {
    expect(getUserDisplayName(makeUser("Ada", "Lovelace"))).toBe("Ada Lovelace");
  });

  it("falls back when the signed-in user name is empty", () => {
    expect(getUserDisplayName(makeUser("", ""))).toBe("User");
    expect(getUserDisplayName(null)).toBe("User");
  });
});
