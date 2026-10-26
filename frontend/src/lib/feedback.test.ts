import { describe, expect, it } from "vitest";
import { getFeedbackRole } from "./feedback";

describe("feedback accessibility helpers", () => {
  it("announces success feedback as a status", () => {
    expect(getFeedbackRole("success")).toBe("status");
  });

  it("announces error feedback as an alert", () => {
    expect(getFeedbackRole("error")).toBe("alert");
  });
});
