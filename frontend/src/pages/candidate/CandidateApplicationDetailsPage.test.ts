import { describe, expect, it } from "vitest";
import { ApiError } from "../../lib/apiClient";
import type { CandidateApplication } from "../../types/candidate";
import {
  canSubmitWithdrawal,
  canWithdrawApplication,
  getApplicationCvDownloadName,
  getNextTimelineReloadKey,
  getSafeApplicationsReturnPath,
  getWithdrawalErrorMessage
} from "./CandidateApplicationDetailsPage";

function makeApplication(overrides: Partial<CandidateApplication> = {}): CandidateApplication {
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

describe("CandidateApplicationDetailsPage helpers", () => {
  it("allows application list return paths with pagination", () => {
    expect(getSafeApplicationsReturnPath("/candidate/applications?page=2"))
      .toBe("/candidate/applications?page=2");
  });

  it("rejects unsafe application return paths", () => {
    expect(getSafeApplicationsReturnPath("https://evil.example/candidate/applications"))
      .toBe("/candidate/applications");
    expect(getSafeApplicationsReturnPath("/candidate/jobs"))
      .toBe("/candidate/applications");
    expect(getSafeApplicationsReturnPath(null))
      .toBe("/candidate/applications");
  });

  it("uses the submitted CV filename for downloads", () => {
    expect(getApplicationCvDownloadName(makeApplication()))
      .toBe("submitted-cv.pdf");
  });

  it("falls back to an application-specific CV download filename", () => {
    expect(getApplicationCvDownloadName(makeApplication({ cvFileName: " " })))
      .toBe("application-42-cv.pdf");
    expect(getApplicationCvDownloadName(makeApplication({ cvFileName: null })))
      .toBe("application-42-cv.pdf");
  });

  it("shows withdrawal only for active candidate-controlled statuses", () => {
    expect(canWithdrawApplication("APPLIED")).toBe(true);
    expect(canWithdrawApplication("REVIEWING")).toBe(true);
    expect(canWithdrawApplication("INTERVIEW")).toBe(true);
  });

  it("hides withdrawal for terminal or already withdrawn statuses", () => {
    expect(canWithdrawApplication("ACCEPTED")).toBe(false);
    expect(canWithdrawApplication("REJECTED")).toBe(false);
    expect(canWithdrawApplication("WITHDRAWN")).toBe(false);
  });

  it("only submits withdrawal from an open non-pending confirmation", () => {
    expect(canSubmitWithdrawal(true, false)).toBe(true);
    expect(canSubmitWithdrawal(false, false)).toBe(false);
    expect(canSubmitWithdrawal(true, true)).toBe(false);
  });

  it("uses friendly withdrawal-specific API errors", () => {
    expect(getWithdrawalErrorMessage(new ApiError("Forbidden", 403)))
      .toBe("You do not have access to withdraw this application.");
    expect(getWithdrawalErrorMessage(new ApiError("Application not found", 404)))
      .toBe("This application is no longer available.");
    expect(getWithdrawalErrorMessage(new ApiError("Conflict", 409)))
      .toBe("This application can no longer be withdrawn because its status changed.");
  });

  it("increments the timeline reload key after server-confirmed changes", () => {
    expect(getNextTimelineReloadKey(0)).toBe(1);
    expect(getNextTimelineReloadKey(4)).toBe(5);
  });
});
