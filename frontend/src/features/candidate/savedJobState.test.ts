import { describe, expect, it } from "vitest";
import {
  createSavedJobIdSet,
  isJobSaved,
  updateSavedJobIdSet
} from "./savedJobState";

describe("saved job state helpers", () => {
  it("maps saved job IDs into lookup state", () => {
    const savedJobIds = createSavedJobIdSet([3, 5, 5]);

    expect(isJobSaved(savedJobIds, 3)).toBe(true);
    expect(isJobSaved(savedJobIds, 5)).toBe(true);
    expect(isJobSaved(savedJobIds, 7)).toBe(false);
    expect(savedJobIds.size).toBe(2);
  });

  it("adds a job after a successful save transition", () => {
    const current = createSavedJobIdSet([3]);
    const next = updateSavedJobIdSet(current, 7, true);

    expect(isJobSaved(next, 3)).toBe(true);
    expect(isJobSaved(next, 7)).toBe(true);
    expect(isJobSaved(current, 7)).toBe(false);
  });

  it("removes a job after a successful unsave transition", () => {
    const current = createSavedJobIdSet([3, 7]);
    const next = updateSavedJobIdSet(current, 7, false);

    expect(isJobSaved(next, 3)).toBe(true);
    expect(isJobSaved(next, 7)).toBe(false);
    expect(isJobSaved(current, 7)).toBe(true);
  });

  it("keeps state unchanged when applying the same saved value", () => {
    const current = createSavedJobIdSet([3]);

    expect([...updateSavedJobIdSet(current, 3, true)]).toEqual([3]);
    expect([...updateSavedJobIdSet(current, 7, false)]).toEqual([3]);
  });
});
