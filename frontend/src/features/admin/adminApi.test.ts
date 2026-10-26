import { beforeEach, describe, expect, it, vi } from "vitest";
import { apiBlobClient, apiClient } from "../../lib/apiClient";
import {
  getAdminDashboard,
  getAdminUsers,
  updateAdminUserEnabled,
  getAdminCompanies,
  updateAdminCompany,
  getAdminCandidateProfile,
  getAdminCandidateCv,
  getAdminJobs,
  updateAdminJobActive,
  updateAdminJob,
  getAdminApplications,
  updateAdminApplicationStatus,
  getAdminApplicationCv,
  getAdminInterviews,
  updateAdminInterviewStatus
} from "./adminApi";

import type {
  UpdateAdminCompanyRequest,
  UpdateAdminJobRequest
} from "../../types/admin";

vi.mock("../../lib/apiClient", () => ({
  apiBlobClient: vi.fn(),
  apiClient: vi.fn()
}));

const mockedApiClient = vi.mocked(apiClient);
const mockedApiBlobClient = vi.mocked(apiBlobClient);

beforeEach(() => {
  mockedApiClient.mockReset();
  mockedApiBlobClient.mockReset();
});

describe("Admin API", () => {
  it("requests dashboard, users, companies, jobs, applications and interviews", () => {
    getAdminDashboard();
    getAdminUsers();
    getAdminCompanies();
    getAdminJobs();
    getAdminApplications();
    getAdminInterviews();

    expect(mockedApiClient).toHaveBeenNthCalledWith(
      1,
      "/api/admin/dashboard"
    );
    expect(mockedApiClient).toHaveBeenNthCalledWith(
      2,
      "/api/admin/users"
    );
    expect(mockedApiClient).toHaveBeenNthCalledWith(
      3,
      "/api/admin/companies"
    );
    expect(mockedApiClient).toHaveBeenNthCalledWith(
      4,
      "/api/admin/jobs"
    );
    expect(mockedApiClient).toHaveBeenNthCalledWith(
      5,
      "/api/admin/applications"
    );
    expect(mockedApiClient).toHaveBeenNthCalledWith(
      6,
      "/api/admin/interviews"
    );

    expect(mockedApiClient).toHaveBeenCalledTimes(6);
  });

  it("enables and disables a user using PATCH", () => {
    updateAdminUserEnabled(42, false);
    updateAdminUserEnabled(42, true);

    expect(mockedApiClient).toHaveBeenNthCalledWith(
      1,
      "/api/admin/users/42/enabled?enabled=false",
      { method: "PATCH" }
    );

    expect(mockedApiClient).toHaveBeenNthCalledWith(
      2,
      "/api/admin/users/42/enabled?enabled=true",
      { method: "PATCH" }
    );
  });

  it("updates a company using PUT with the correct payload", () => {
    const payload: UpdateAdminCompanyRequest = {
      name: "Acme",
      description: "Company description",
      website: "https://example.com",
      location: "Meknes"
    };

    updateAdminCompany(7, payload);

    expect(mockedApiClient).toHaveBeenCalledWith(
      "/api/admin/companies/7",
      {
        method: "PUT",
        body: JSON.stringify(payload)
      }
    );
  });

  it("requests a candidate profile by user ID", () => {
    getAdminCandidateProfile(15);

    expect(mockedApiClient).toHaveBeenCalledWith(
      "/api/admin/candidates/15"
    );
  });

  it("requests a candidate CV as a blob", () => {
    getAdminCandidateCv(15);

    expect(mockedApiBlobClient).toHaveBeenCalledWith(
      "/api/admin/candidates/15/cv"
    );
  });

  it("activates and deactivates a job using PATCH", () => {
    updateAdminJobActive(23, false);
    updateAdminJobActive(23, true);

    expect(mockedApiClient).toHaveBeenNthCalledWith(
      1,
      "/api/admin/jobs/23/active?active=false",
      { method: "PATCH" }
    );

    expect(mockedApiClient).toHaveBeenNthCalledWith(
      2,
      "/api/admin/jobs/23/active?active=true",
      { method: "PATCH" }
    );
  });

  it("updates a job using PUT with the correct payload", () => {
    const payload: UpdateAdminJobRequest = {
      title: "Junior Developer",
      description: "A junior developer position",
      location: "Remote",
      employmentType: "FULL_TIME"
    };

    updateAdminJob(23, payload);

    expect(mockedApiClient).toHaveBeenCalledWith(
      "/api/admin/jobs/23",
      {
        method: "PUT",
        body: JSON.stringify(payload)
      }
    );
  });

  it("updates an application status using PATCH", () => {
    const status =
      "PENDING" as Parameters<typeof updateAdminApplicationStatus>[1];

    updateAdminApplicationStatus(31, status);

    expect(mockedApiClient).toHaveBeenCalledWith(
      `/api/admin/applications/31/status?status=${status}`,
      { method: "PATCH" }
    );
  });

  it("requests an application CV as a blob", () => {
    getAdminApplicationCv(31);

    expect(mockedApiBlobClient).toHaveBeenCalledWith(
      "/api/admin/applications/31/cv"
    );
  });

  it("updates an interview status using PATCH", () => {
    const status =
      "SCHEDULED" as Parameters<typeof updateAdminInterviewStatus>[1];

    updateAdminInterviewStatus(54, status);

    expect(mockedApiClient).toHaveBeenCalledWith(
      `/api/admin/interviews/54/status?status=${status}`,
      { method: "PATCH" }
    );
  });
});