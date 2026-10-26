import { beforeEach, describe, expect, it, vi } from "vitest";
import { apiBlobClient, apiClient } from "../../lib/apiClient";
import type {
  CandidateApplication,
  CandidateDashboard,
  CandidateInterview,
  CandidateApplicationTimelineEvent,
  Job,
  PageResponse,
  SavedJobResponse
} from "../../types/candidate";
import {
  applyToJob,
  getCandidateDashboard,
  getMyApplication,
  getMyApplicationForJob,
  getMyApplicationCv,
  getMyApplicationTimeline,
  getMyApplications,
  getSavedJobIds,
  getSavedJobs,
  saveJob,
  unsaveJob,
  withdrawMyApplication
} from "./candidateApi";

vi.mock("../../lib/apiClient", () => ({
  apiBlobClient: vi.fn(),
  apiClient: vi.fn()
}));

const mockedApiClient = vi.mocked(apiClient);
const mockedApiBlobClient = vi.mocked(apiBlobClient);

function makeJob(id: number): Job {
  return {
    id,
    title: `Job ${id}`,
    description: `Description ${id}`,
    location: "Remote",
    employmentType: "FULL_TIME",
    active: true,
    companyId: 1,
    companyName: "Acme",
    createdById: 2,
    createdByName: "Recruiter User",
    createdAt: "2026-10-10T10:00:00"
  };
}

function makeSavedJob(id: number): SavedJobResponse {
  return {
    id,
    job: makeJob(id + 10),
    savedAt: "2026-10-10T11:00:00"
  };
}

function makeSavedJobsPage(
  content: SavedJobResponse[],
  page: number,
  size: number
): PageResponse<SavedJobResponse> {
  return {
    content,
    page,
    size,
    totalElements: content.length,
    totalPages: content.length > 0 ? 1 : 0,
    first: true,
    last: true
  };
}

function makeApplication(id: number, jobId = id + 100): CandidateApplication {
  return {
    id,
    jobId,
    jobTitle: `Application Job ${jobId}`,
    companyName: "Acme",
    candidateId: 7,
    candidateName: "Candidate User",
    candidateEmail: "candidate@example.com",
    status: "APPLIED",
    cvFileName: "candidate-cv.pdf",
    appliedAt: "2026-10-10T12:00:00"
  };
}

function makeInterview(id: number, applicationId = id + 200): CandidateInterview {
  return {
    id,
    applicationId,
    jobId: applicationId + 100,
    jobTitle: `Interview Job ${applicationId + 100}`,
    candidateId: 7,
    candidateName: "Candidate User",
    candidateEmail: "candidate@example.com",
    scheduledAt: "2026-10-20T12:00:00",
    type: "ONLINE",
    meetingLink: "https://example.com/interview",
    location: null,
    notes: "Interview notes",
    status: "SCHEDULED"
  };
}

function makeDashboard(
  recentApplications: CandidateApplication[] = [makeApplication(1)],
  nextInterviews: CandidateInterview[] = [makeInterview(1)]
): CandidateDashboard {
  return {
    totalApplications: 4,
    activeApplications: 2,
    withdrawnApplications: 1,
    upcomingInterviews: nextInterviews.length,
    savedJobs: 3,
    recentApplications,
    nextInterviews
  };
}

function makeTimelineEvent(
  status: CandidateApplicationTimelineEvent["status"],
  eventType: CandidateApplicationTimelineEvent["eventType"],
  changedAt: string
): CandidateApplicationTimelineEvent {
  return {
    status,
    eventType,
    changedAt
  };
}

function makeApplicationsPage(
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

describe("candidate dashboard API", () => {
  beforeEach(() => {
    mockedApiClient.mockReset();
    mockedApiBlobClient.mockReset();
  });

  it("gets the candidate dashboard with authenticated API defaults", async () => {
    const response = makeDashboard();

    mockedApiClient.mockResolvedValueOnce(response);

    await expect(getCandidateDashboard()).resolves.toEqual(response);

    expect(mockedApiClient).toHaveBeenCalledTimes(1);
    expect(mockedApiClient).toHaveBeenCalledWith("/api/candidate/dashboard");
  });

  it("uses GET by not passing request options", async () => {
    const response = makeDashboard();

    mockedApiClient.mockResolvedValueOnce(response);

    await getCandidateDashboard();

    expect(mockedApiClient).toHaveBeenCalledWith("/api/candidate/dashboard");
    expect(mockedApiClient.mock.calls[0]).toHaveLength(1);
  });

  it("returns an empty dashboard response", async () => {
    const response = makeDashboard([], []);
    response.totalApplications = 0;
    response.activeApplications = 0;
    response.withdrawnApplications = 0;
    response.savedJobs = 0;

    mockedApiClient.mockResolvedValueOnce(response);

    await expect(getCandidateDashboard()).resolves.toEqual({
      totalApplications: 0,
      activeApplications: 0,
      withdrawnApplications: 0,
      upcomingInterviews: 0,
      savedJobs: 0,
      recentApplications: [],
      nextInterviews: []
    });

    expect(mockedApiClient).toHaveBeenCalledWith("/api/candidate/dashboard");
  });

  it("propagates dashboard API errors", async () => {
    const error = new Error("Dashboard failed");

    mockedApiClient.mockRejectedValueOnce(error);

    await expect(getCandidateDashboard()).rejects.toThrow(error);

    expect(mockedApiClient).toHaveBeenCalledTimes(1);
    expect(mockedApiClient).toHaveBeenCalledWith("/api/candidate/dashboard");
  });
});

describe("candidate saved jobs API", () => {
  beforeEach(() => {
    mockedApiClient.mockReset();
    mockedApiBlobClient.mockReset();
  });

  it("gets saved jobs with default pagination", async () => {
    const response = makeSavedJobsPage([makeSavedJob(1)], 0, 10);

    mockedApiClient.mockResolvedValueOnce(response);

    await expect(getSavedJobs()).resolves.toEqual(response);

    expect(mockedApiClient).toHaveBeenCalledTimes(1);
    expect(mockedApiClient).toHaveBeenCalledWith(
      "/api/candidate/saved-jobs?page=0&size=10"
    );
  });

  it("gets saved jobs with explicit pagination", async () => {
    const response = makeSavedJobsPage([makeSavedJob(2)], 2, 5);

    mockedApiClient.mockResolvedValueOnce(response);

    await expect(getSavedJobs(2, 5)).resolves.toEqual(response);

    expect(mockedApiClient).toHaveBeenCalledTimes(1);
    expect(mockedApiClient).toHaveBeenCalledWith(
      "/api/candidate/saved-jobs?page=2&size=5"
    );
  });

  it("gets active saved job IDs", async () => {
    mockedApiClient.mockResolvedValueOnce([12, 11]);

    await expect(getSavedJobIds()).resolves.toEqual([12, 11]);

    expect(mockedApiClient).toHaveBeenCalledTimes(1);
    expect(mockedApiClient).toHaveBeenCalledWith(
      "/api/candidate/saved-jobs/job-ids"
    );
  });

  it("saves a job with POST", async () => {
    const response = makeSavedJob(3);

    mockedApiClient.mockResolvedValueOnce(response);

    await expect(saveJob(13)).resolves.toEqual(response);

    expect(mockedApiClient).toHaveBeenCalledTimes(1);
    expect(mockedApiClient).toHaveBeenCalledWith(
      "/api/candidate/saved-jobs/13",
      { method: "POST" }
    );
  });

  it("unsaves a job with DELETE", async () => {
    mockedApiClient.mockResolvedValueOnce(undefined);

    await expect(unsaveJob(13)).resolves.toBeUndefined();

    expect(mockedApiClient).toHaveBeenCalledTimes(1);
    expect(mockedApiClient).toHaveBeenCalledWith(
      "/api/candidate/saved-jobs/13",
      { method: "DELETE" }
    );
  });

  it("propagates API errors", async () => {
    const error = new Error("Request failed");

    mockedApiClient.mockRejectedValueOnce(error);

    await expect(getSavedJobs()).rejects.toThrow(error);

    expect(mockedApiClient).toHaveBeenCalledTimes(1);
  });

  it("propagates save errors", async () => {
    const error = new Error("Save failed");

    mockedApiClient.mockRejectedValueOnce(error);

    await expect(saveJob(13)).rejects.toThrow(error);

    expect(mockedApiClient).toHaveBeenCalledTimes(1);
    expect(mockedApiClient).toHaveBeenCalledWith(
      "/api/candidate/saved-jobs/13",
      { method: "POST" }
    );
  });

  it("propagates unsave errors", async () => {
    const error = new Error("Unsave failed");

    mockedApiClient.mockRejectedValueOnce(error);

    await expect(unsaveJob(13)).rejects.toThrow(error);

    expect(mockedApiClient).toHaveBeenCalledTimes(1);
    expect(mockedApiClient).toHaveBeenCalledWith(
      "/api/candidate/saved-jobs/13",
      { method: "DELETE" }
    );
  });
});

describe("candidate applications API", () => {
  beforeEach(() => {
    mockedApiClient.mockReset();
    mockedApiBlobClient.mockReset();
  });

  it("gets applications with default pagination", async () => {
    const response = makeApplicationsPage([makeApplication(1)], 0, 1, 1);

    mockedApiClient.mockResolvedValueOnce(response);

    await expect(getMyApplications()).resolves.toEqual(response);

    expect(mockedApiClient).toHaveBeenCalledTimes(1);
    expect(mockedApiClient).toHaveBeenCalledWith(
      "/api/applications/me?page=0&size=10"
    );
  });

  it("gets applications with explicit pagination", async () => {
    const response = makeApplicationsPage([makeApplication(2)], 2, 22, 3);

    mockedApiClient.mockResolvedValueOnce(response);

    await expect(getMyApplications(2, 5)).resolves.toEqual(response);

    expect(mockedApiClient).toHaveBeenCalledTimes(1);
    expect(mockedApiClient).toHaveBeenCalledWith(
      "/api/applications/me?page=2&size=5"
    );
  });

  it("gets a candidate-owned application detail", async () => {
    const response = makeApplication(12, 55);

    mockedApiClient.mockResolvedValueOnce(response);

    await expect(getMyApplication(12)).resolves.toEqual(response);

    expect(mockedApiClient).toHaveBeenCalledTimes(1);
    expect(mockedApiClient).toHaveBeenCalledWith("/api/applications/me/12");
  });

  it("withdraws a candidate-owned application with PATCH and no body", async () => {
    const response = makeApplication(12, 55);

    mockedApiClient.mockResolvedValueOnce(response);

    await expect(withdrawMyApplication(12)).resolves.toEqual(response);

    expect(mockedApiClient).toHaveBeenCalledTimes(1);
    expect(mockedApiClient).toHaveBeenCalledWith(
      "/api/applications/me/12/withdraw",
      { method: "PATCH" }
    );
  });

  it("downloads a candidate-owned application CV as a blob", async () => {
    const response = new Blob(["%PDF-1.4"]);

    mockedApiBlobClient.mockResolvedValueOnce(response);

    await expect(getMyApplicationCv(12)).resolves.toBe(response);

    expect(mockedApiBlobClient).toHaveBeenCalledTimes(1);
    expect(mockedApiBlobClient).toHaveBeenCalledWith("/api/applications/me/12/cv");
  });

  it("gets a candidate-owned application timeline", async () => {
    const response = [
      makeTimelineEvent("APPLIED", "BASELINE", "2026-10-10T09:00:00"),
      makeTimelineEvent("REVIEWING", "TRANSITION", "2026-10-10T10:00:00")
    ];

    mockedApiClient.mockResolvedValueOnce(response);

    await expect(getMyApplicationTimeline(12)).resolves.toEqual(response);

    expect(mockedApiClient).toHaveBeenCalledTimes(1);
    expect(mockedApiClient).toHaveBeenCalledWith(
      "/api/applications/me/12/timeline"
    );
  });

  it("gets the candidate-owned application for a job in one request", async () => {
    const matchingApplication = makeApplication(15, 900);

    mockedApiClient.mockResolvedValueOnce(matchingApplication);

    await expect(getMyApplicationForJob(900)).resolves.toEqual(matchingApplication);

    expect(mockedApiClient).toHaveBeenCalledTimes(1);
    expect(mockedApiClient).toHaveBeenCalledWith("/api/applications/me/jobs/900");
  });

  it("returns a withdrawn application for the job lookup", async () => {
    const withdrawnApplication = makeApplication(15, 900);
    withdrawnApplication.status = "WITHDRAWN";

    mockedApiClient.mockResolvedValueOnce(withdrawnApplication);

    await expect(getMyApplicationForJob(900)).resolves.toEqual(withdrawnApplication);

    expect(mockedApiClient).toHaveBeenCalledTimes(1);
    expect(mockedApiClient).toHaveBeenCalledWith("/api/applications/me/jobs/900");
  });

  it("returns null when the candidate has no application for a job", async () => {
    mockedApiClient.mockResolvedValueOnce(null);

    await expect(getMyApplicationForJob(999)).resolves.toBeNull();

    expect(mockedApiClient).toHaveBeenCalledTimes(1);
    expect(mockedApiClient).toHaveBeenCalledWith("/api/applications/me/jobs/999");
  });

  it("propagates application pagination errors", async () => {
    const error = new Error("Applications failed");

    mockedApiClient.mockRejectedValueOnce(error);

    await expect(getMyApplications()).rejects.toThrow(error);

    expect(mockedApiClient).toHaveBeenCalledTimes(1);
  });

  it("propagates application detail errors", async () => {
    const error = new Error("Detail failed");

    mockedApiClient.mockRejectedValueOnce(error);

    await expect(getMyApplication(12)).rejects.toThrow(error);

    expect(mockedApiClient).toHaveBeenCalledTimes(1);
  });

  it("propagates application CV download errors", async () => {
    const error = new Error("Download failed");

    mockedApiBlobClient.mockRejectedValueOnce(error);

    await expect(getMyApplicationCv(12)).rejects.toThrow(error);

    expect(mockedApiBlobClient).toHaveBeenCalledTimes(1);
  });

  it("propagates application timeline errors", async () => {
    const error = new Error("Timeline failed");

    mockedApiClient.mockRejectedValueOnce(error);

    await expect(getMyApplicationTimeline(12)).rejects.toThrow(error);

    expect(mockedApiClient).toHaveBeenCalledTimes(1);
  });

  it("propagates candidate job-application lookup errors", async () => {
    const error = new Error("Lookup failed");

    mockedApiClient.mockRejectedValueOnce(error);

    await expect(getMyApplicationForJob(999)).rejects.toThrow(error);

    expect(mockedApiClient).toHaveBeenCalledTimes(1);
  });

  it("submits an application with POST", async () => {
    const response = makeApplication(1, 55);

    mockedApiClient.mockResolvedValueOnce(response);

    await expect(applyToJob(55)).resolves.toEqual(response);

    expect(mockedApiClient).toHaveBeenCalledTimes(1);
    expect(mockedApiClient).toHaveBeenCalledWith(
      "/api/jobs/55/apply",
      expect.objectContaining({
        method: "POST",
        body: expect.any(FormData)
      })
    );
  });
});
