import { describe, expect, it } from "vitest";
import type { CandidateApplication, PageResponse } from "../../types/candidate";
import {
  buildApplicationsSearchParams,
  getRecoveredApplicationsPage,
  parseApplicationsPage
} from "./CandidateApplicationsPage";

function makeApplication(id: number): CandidateApplication {
  return {
    id,
    jobId: id + 100,
    jobTitle: `Job ${id}`,
    companyName: "Acme",
    candidateId: 7,
    candidateName: "Candidate User",
    candidateEmail: "candidate@example.com",
    status: "APPLIED",
    cvFileName: "candidate-cv.pdf",
    appliedAt: "2026-10-10T12:00:00"
  };
}

function makePage(
  content: CandidateApplication[],
  page: number,
  totalElements: number,
  totalPages: number
): PageResponse<CandidateApplication> {
  return {
    content,
    page,
    size: 10,
    totalElements,
    totalPages,
    first: page === 0,
    last: page === totalPages - 1 || totalPages === 0
  };
}

describe("CandidateApplicationsPage pagination helpers", () => {
  it("parses valid and invalid page parameters", () => {
    expect(parseApplicationsPage(null)).toBe(0);
    expect(parseApplicationsPage("0")).toBe(0);
    expect(parseApplicationsPage("4")).toBe(4);
    expect(parseApplicationsPage("-1")).toBe(0);
    expect(parseApplicationsPage("1.5")).toBe(0);
    expect(parseApplicationsPage("abc")).toBe(0);
  });

  it("builds clean application search params", () => {
    expect(buildApplicationsSearchParams(0).toString()).toBe("");
    expect(buildApplicationsSearchParams(3).toString()).toBe("page=3");
  });

  it("keeps a populated page in place", () => {
    expect(getRecoveredApplicationsPage(makePage([makeApplication(1)], 2, 21, 3)))
      .toBeNull();
  });

  it("moves an empty out-of-range page to the final page", () => {
    expect(getRecoveredApplicationsPage(makePage([], 4, 21, 3))).toBe(2);
  });

  it("keeps a genuinely empty application result on page zero", () => {
    expect(getRecoveredApplicationsPage(makePage([], 0, 0, 0))).toBeNull();
  });
});
