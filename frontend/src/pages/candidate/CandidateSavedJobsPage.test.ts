import { describe, expect, it } from "vitest";
import {
  buildSavedJobsSearchParams,
  getPageAfterSavedJobRemoval,
  parseSavedJobsPage
} from "./CandidateSavedJobsPage";

describe("CandidateSavedJobsPage pagination helpers", () => {
  it("parses valid and invalid page parameters", () => {
    expect(parseSavedJobsPage(null)).toBe(0);
    expect(parseSavedJobsPage("0")).toBe(0);
    expect(parseSavedJobsPage("3")).toBe(3);
    expect(parseSavedJobsPage("-1")).toBe(0);
    expect(parseSavedJobsPage("1.5")).toBe(0);
    expect(parseSavedJobsPage("abc")).toBe(0);
  });

  it("builds clean saved-jobs search params", () => {
    expect(buildSavedJobsSearchParams(0).toString()).toBe("");
    expect(buildSavedJobsSearchParams(2).toString()).toBe("page=2");
  });

  it("stays on the current page after removing from a page with remaining items", () => {
    expect(getPageAfterSavedJobRemoval(2, 3)).toBe(2);
  });

  it("moves back one page after removing the last item from a non-first page", () => {
    expect(getPageAfterSavedJobRemoval(2, 1)).toBe(1);
  });

  it("stays on the first page after removing its last item", () => {
    expect(getPageAfterSavedJobRemoval(0, 1)).toBe(0);
  });
});
