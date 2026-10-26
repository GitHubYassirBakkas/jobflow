import { describe, expect, it } from "vitest";
import { assignableApplicationStatuses, isApplicationStatusAssignable } from "./applicationStatusOptions";

describe("applicationStatusOptions", () => {
  it("keeps withdrawn out of recruiter and admin assignable statuses", () => {
    expect(assignableApplicationStatuses).toEqual([
      "APPLIED",
      "REVIEWING",
      "INTERVIEW",
      "ACCEPTED",
      "REJECTED"
    ]);
    expect(assignableApplicationStatuses).not.toContain("WITHDRAWN");
    expect(isApplicationStatusAssignable("WITHDRAWN")).toBe(false);
  });

  it("recognizes existing recruiter and admin target statuses", () => {
    expect(isApplicationStatusAssignable("APPLIED")).toBe(true);
    expect(isApplicationStatusAssignable("REVIEWING")).toBe(true);
    expect(isApplicationStatusAssignable("INTERVIEW")).toBe(true);
    expect(isApplicationStatusAssignable("ACCEPTED")).toBe(true);
    expect(isApplicationStatusAssignable("REJECTED")).toBe(true);
  });
});
