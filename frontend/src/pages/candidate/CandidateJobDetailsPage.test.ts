import { describe, expect, it } from "vitest";
import type { CandidateApplication } from "../../types/candidate";
import {
  getSafeJobsReturnPath,
  isApplicationAlreadySubmitted
} from "./CandidateJobDetailsPage";

function makeApplication(
  overrides: Partial<CandidateApplication> = {}
): CandidateApplication {
  return {
    id: 42,
    jobId: 7,
    jobTitle: "Backend Engineer",
    companyName: "Acme",
    candidateId: 3,
    candidateName: "Candidate User",
    candidateEmail: "candidate@example.com",
    status: "APPLIED",
    cvFileName: "submitted-cv.pdf",
    appliedAt: "2026-10-10T12:00:00",
    ...overrides
  };
}

describe("CandidateJobDetailsPage navigation helpers", () => {
  it("allows returning to paginated candidate applications", () => {
    expect(getSafeJobsReturnPath("/candidate/applications?page=2"))
      .toBe("/candidate/applications?page=2");
  });

  it("keeps existing job discovery and saved jobs return paths", () => {
    expect(getSafeJobsReturnPath("/candidate/jobs?search=java"))
      .toBe("/candidate/jobs?search=java");
    expect(getSafeJobsReturnPath("/candidate/saved-jobs?page=1"))
      .toBe("/candidate/saved-jobs?page=1");
  });

  it("rejects unsafe return paths", () => {
    expect(getSafeJobsReturnPath("https://evil.example/candidate/applications"))
      .toBe("/candidate/jobs");
    expect(getSafeJobsReturnPath("/admin/users")).toBe("/candidate/jobs");
  });

  it("treats withdrawn applications as already submitted for the job", () => {
    expect(isApplicationAlreadySubmitted(
      makeApplication({ status: "WITHDRAWN" }),
      false
    )).toBe(true);
  });

  it("keeps submitted applications as already applied after local success", () => {
    expect(isApplicationAlreadySubmitted(null, true)).toBe(true);
    expect(isApplicationAlreadySubmitted(null, false)).toBe(false);
  });
});
