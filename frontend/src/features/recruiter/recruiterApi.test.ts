import { beforeEach, describe, expect, it, vi } from "vitest";
import { apiClient } from "../../lib/apiClient";
import type { PageResponse } from "../../types/candidate";
import type { RecruiterJob } from "../../types/recruiter";
import { getAllRecruiterJobs } from "./recruiterApi";

vi.mock("../../lib/apiClient", () => ({
  apiBlobClient: vi.fn(),
  apiClient: vi.fn()
}));

const mockedApiClient = vi.mocked(apiClient);

function makeJob(id: number): RecruiterJob {
  return {
    id,
    title: `Job ${id}`,
    description: `Description ${id}`,
    location: "Remote",
    employmentType: "FULL_TIME",
    active: true,
    companyId: 1,
    companyName: "Acme",
    createdById: 1,
    createdByName: "Recruiter User",
    createdAt: "2026-10-08T10:00:00"
  };
}

function makeJobs(count: number, startId = 1) {
  return Array.from({ length: count }, (_, index) => makeJob(startId + index));
}

function makePage(
  content: RecruiterJob[],
  page: number,
  totalElements: number,
  totalPages: number
): PageResponse<RecruiterJob> {
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

describe("getAllRecruiterJobs", () => {
  beforeEach(() => {
    mockedApiClient.mockReset();
  });

  it("returns all jobs from a single page", async () => {
    const jobs = makeJobs(5);

    mockedApiClient.mockResolvedValueOnce(makePage(jobs, 0, 5, 1));

    await expect(getAllRecruiterJobs()).resolves.toEqual(jobs);

    expect(mockedApiClient).toHaveBeenCalledTimes(1);
    expect(mockedApiClient).toHaveBeenNthCalledWith(1, "/api/jobs");
  });

  it("requests two pages and returns all jobs in order", async () => {
    const firstPageJobs = makeJobs(10);
    const secondPageJobs = makeJobs(5, 11);

    mockedApiClient
      .mockResolvedValueOnce(makePage(firstPageJobs, 0, 15, 2))
      .mockResolvedValueOnce(makePage(secondPageJobs, 1, 15, 2));

    await expect(getAllRecruiterJobs()).resolves.toEqual([
      ...firstPageJobs,
      ...secondPageJobs
    ]);

    expect(mockedApiClient).toHaveBeenCalledTimes(2);
    expect(mockedApiClient).toHaveBeenNthCalledWith(1, "/api/jobs");
    expect(mockedApiClient).toHaveBeenNthCalledWith(2, "/api/jobs?page=1");
    expect(mockedApiClient).not.toHaveBeenCalledWith("/api/jobs?page=2");
  });

  it("returns all jobs from a larger paginated dataset", async () => {
    const allJobs = makeJobs(100);

    for (let page = 0; page < 10; page += 1) {
      const start = page * 10;

      mockedApiClient.mockResolvedValueOnce(
        makePage(allJobs.slice(start, start + 10), page, 100, 10)
      );
    }

    await expect(getAllRecruiterJobs()).resolves.toEqual(allJobs);

    expect(mockedApiClient).toHaveBeenCalledTimes(10);
    expect(mockedApiClient).toHaveBeenNthCalledWith(1, "/api/jobs");
    expect(mockedApiClient).toHaveBeenNthCalledWith(10, "/api/jobs?page=9");
    expect(mockedApiClient).not.toHaveBeenCalledWith("/api/jobs?page=10");
  });

  it("rejects when a later page fails", async () => {
    const pageError = new Error("Page 1 failed");

    mockedApiClient
      .mockResolvedValueOnce(makePage(makeJobs(10), 0, 15, 2))
      .mockRejectedValueOnce(pageError);

    await expect(getAllRecruiterJobs()).rejects.toThrow(pageError);

    expect(mockedApiClient).toHaveBeenCalledTimes(2);
    expect(mockedApiClient).toHaveBeenNthCalledWith(2, "/api/jobs?page=1");
  });

  it("returns an empty array for an empty first page", async () => {
    mockedApiClient.mockResolvedValueOnce(makePage([], 0, 0, 0));

    await expect(getAllRecruiterJobs()).resolves.toEqual([]);

    expect(mockedApiClient).toHaveBeenCalledTimes(1);
    expect(mockedApiClient).toHaveBeenNthCalledWith(1, "/api/jobs");
  });
});
