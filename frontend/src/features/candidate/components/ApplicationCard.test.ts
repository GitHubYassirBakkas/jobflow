import { describe, expect, it } from "vitest";
import type { CandidateApplication } from "../../../types/candidate";
import { getApplicationDetailsPath } from "./ApplicationCard";

function makeApplication(id: number): CandidateApplication {
  return {
    id,
    jobId: 99,
    jobTitle: "Backend Engineer",
    companyName: "Acme",
    candidateId: 3,
    candidateName: "Candidate User",
    candidateEmail: "candidate@example.com",
    status: "APPLIED",
    cvFileName: "submitted-cv.pdf",
    appliedAt: "2026-10-10T12:00:00"
  };
}

describe("ApplicationCard navigation helpers", () => {
  it("links to the candidate application details route", () => {
    expect(getApplicationDetailsPath(makeApplication(42)))
      .toBe("/candidate/applications/42");
  });
});
