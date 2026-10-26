import { isValidElement } from "react";
import { describe, expect, it } from "vitest";
import { ApplicationStatusBadge } from "./StatusBadge";

describe("ApplicationStatusBadge", () => {
  it("displays withdrawn applications with accessible text", () => {
    const badge = ApplicationStatusBadge({ status: "WITHDRAWN" });

    expect(isValidElement(badge)).toBe(true);

    if (!isValidElement<{ "aria-label": string; className: string; children: string }>(badge)) {
      throw new Error("Expected a valid badge element");
    }

    expect(badge.props.children).toBe("Withdrawn");
    expect(badge.props["aria-label"]).toBe("Withdrawn");
    expect(badge.props.className).toContain("status-neutral");
  });
});
