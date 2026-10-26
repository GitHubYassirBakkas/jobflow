import { describe, expect, it } from "vitest";
import { getNextFocusIndex } from "./focusManagement";

describe("focus management helpers", () => {
  it("moves forward through focusable items", () => {
    expect(getNextFocusIndex(0, 3, false)).toBe(1);
    expect(getNextFocusIndex(1, 3, false)).toBe(2);
  });

  it("wraps forward from the final focusable item", () => {
    expect(getNextFocusIndex(2, 3, false)).toBe(0);
  });

  it("moves backward through focusable items", () => {
    expect(getNextFocusIndex(2, 3, true)).toBe(1);
    expect(getNextFocusIndex(1, 3, true)).toBe(0);
  });

  it("wraps backward from the first focusable item", () => {
    expect(getNextFocusIndex(0, 3, true)).toBe(2);
  });

  it("chooses an edge item when current focus is outside the container", () => {
    expect(getNextFocusIndex(-1, 3, false)).toBe(0);
    expect(getNextFocusIndex(-1, 3, true)).toBe(2);
  });

  it("returns no target when there are no focusable items", () => {
    expect(getNextFocusIndex(-1, 0, false)).toBe(-1);
    expect(getNextFocusIndex(0, 0, true)).toBe(-1);
  });
});
