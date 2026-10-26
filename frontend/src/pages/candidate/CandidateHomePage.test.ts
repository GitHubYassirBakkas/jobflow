import { describe, expect, it } from "vitest";
import type { CandidateApplication, CandidateDashboard, CandidateInterview } from "../../types/candidate";
import {
  buildCandidateDashboardSummary,
  getCandidateDashboardPreviews
} from "./CandidateHomePage";

function makeApplication(id: number, appliedAt: string): CandidateApplication {
  return {
    id,
    jobId: id + 100,
    jobTitle: `Application Job ${id}`,
    companyName: `Company ${id}`,
    candidateId: 7,
    candidateName: "Candidate User",
    candidateEmail: "candidate@example.com",
    status: id % 2 === 0 ? "REVIEWING" : "APPLIED",
    cvFileName: "candidate-cv.pdf",
    appliedAt
  };
}

function makeInterview(id: number, scheduledAt: string): CandidateInterview {
  return {
    id,
    applicationId: id + 10,
    jobId: id + 20,
    jobTitle: `Interview Job ${id}`,
    candidateId: 7,
    candidateName: "Candidate User",
    candidateEmail: "candidate@example.com",
    scheduledAt,
    type: "ONLINE",
    meetingLink: "https://example.com/meet",
    location: null,
    notes: null,
    status: "SCHEDULED"
  };
}

function makeDashboard(
  overrides: Partial<CandidateDashboard> = {}
): CandidateDashboard {
  return {
    totalApplications: 8,
    activeApplications: 4,
    withdrawnApplications: 2,
    upcomingInterviews: 3,
    savedJobs: 5,
    recentApplications: [
      makeApplication(3, "2026-10-12T12:00:00"),
      makeApplication(2, "2026-10-11T12:00:00"),
      makeApplication(1, "2026-10-10T12:00:00")
    ],
    nextInterviews: [
      makeInterview(1, "2026-10-14T09:00:00"),
      makeInterview(2, "2026-10-15T09:00:00")
    ],
    ...overrides
  };
}

describe("CandidateHomePage dashboard helpers", () => {
  it("maps backend counts into summary cards without deriving from previews", () => {
    const summary = buildCandidateDashboardSummary(
      makeDashboard({
        totalApplications: 42,
        activeApplications: 12,
        withdrawnApplications: 6,
        upcomingInterviews: 2,
        savedJobs: 9,
        recentApplications: [makeApplication(1, "2026-10-10T12:00:00")],
        nextInterviews: []
      })
    );

    expect(summary.map((item) => [item.label, item.value])).toEqual([
      ["Total Applications", 42],
      ["Active Applications", 12],
      ["Withdrawn Applications", 6],
      ["Upcoming Interviews", 2],
      ["Saved Jobs", 9]
    ]);
  });

  it("keeps zero counts visible in summary cards", () => {
    const summary = buildCandidateDashboardSummary(
      makeDashboard({
        totalApplications: 0,
        activeApplications: 0,
        withdrawnApplications: 0,
        upcomingInterviews: 0,
        savedJobs: 0,
        recentApplications: [],
        nextInterviews: []
      })
    );

    expect(summary.every((item) => item.value === 0)).toBe(true);
  });

  it("uses existing candidate navigation routes for summary cards", () => {
    const summary = buildCandidateDashboardSummary(makeDashboard());

    expect(summary.map((item) => item.to)).toEqual([
      "/candidate/applications",
      "/candidate/applications",
      "/candidate/applications",
      "/candidate/interviews",
      "/candidate/saved-jobs"
    ]);
  });

  it("preserves recent application and upcoming interview preview ordering", () => {
    const recentApplications = [
      makeApplication(5, "2026-10-15T12:00:00"),
      makeApplication(4, "2026-10-14T12:00:00")
    ];
    const nextInterviews = [
      makeInterview(8, "2026-10-20T09:00:00"),
      makeInterview(9, "2026-10-21T09:00:00")
    ];

    const previews = getCandidateDashboardPreviews(
      makeDashboard({ recentApplications, nextInterviews })
    );

    expect(previews.recentApplications.map((application) => application.id))
      .toEqual([5, 4]);
    expect(previews.nextInterviews.map((interview) => interview.id))
      .toEqual([8, 9]);
  });

  it("reports empty application, interview, and saved-job states", () => {
    const previews = getCandidateDashboardPreviews(
      makeDashboard({
        savedJobs: 0,
        recentApplications: [],
        nextInterviews: []
      })
    );

    expect(previews.hasRecentApplications).toBe(false);
    expect(previews.hasUpcomingInterviews).toBe(false);
    expect(previews.hasSavedJobs).toBe(false);
  });
});
