import { apiBlobClient, apiClient } from "../../lib/apiClient";
import type { PageResponse } from "../../types/candidate";
import type {
  CreateJobRequest,
  RecruiterApplication,
  RecruiterInterview,
  RecruiterJob,
  RecruiterProfile,
  ScheduleInterviewRequest,
  UpdateApplicationStatusRequest,
  UpdateInterviewStatusRequest
} from "../../types/recruiter";

export function getRecruiterProfile() {
  return apiClient<RecruiterProfile>("/api/recruiter/profile/me");
}

export function getRecruiterJobs() {
  return apiClient<PageResponse<RecruiterJob>>("/api/jobs");
}

async function getRecruiterJobsPage(page: number) {
  const searchParams = new URLSearchParams();

  if (page > 0) {
    searchParams.set("page", String(page));
  }

  const queryString = searchParams.toString();

  return apiClient<PageResponse<RecruiterJob>>(
    queryString ? `/api/jobs?${queryString}` : "/api/jobs"
  );
}

export async function getAllRecruiterJobs() {
  const firstPage = await getRecruiterJobsPage(0);
  const jobs = [...firstPage.content];

  for (
    let page = firstPage.page + 1;
    page < firstPage.totalPages && !firstPage.last;
    page += 1
  ) {
    const nextPage = await getRecruiterJobsPage(page);
    jobs.push(...nextPage.content);

    if (nextPage.last) {
      break;
    }
  }

  return jobs;
}

export function getRecruiterJob(jobId: string | number) {
  return apiClient<RecruiterJob>(`/api/jobs/${jobId}`);
}

export function createRecruiterJob(payload: CreateJobRequest) {
  return apiClient<RecruiterJob>("/api/jobs", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function getRecruiterJobApplications(jobId: string | number) {
  return apiClient<RecruiterApplication[]>(`/api/recruiter/jobs/${jobId}/applications`);
}

export function getRecruiterApplicationCv(applicationId: string | number) {
  return apiBlobClient(`/api/recruiter/jobs/applications/${applicationId}/cv`);
}

export function updateRecruiterApplicationStatus(
  applicationId: string | number,
  payload: UpdateApplicationStatusRequest
) {
  return apiClient<RecruiterApplication>(`/api/recruiter/jobs/applications/${applicationId}/status`, {
    method: "PATCH",
    body: JSON.stringify(payload)
  });
}

export function scheduleRecruiterInterview(applicationId: string | number, payload: ScheduleInterviewRequest) {
  return apiClient<RecruiterInterview>(`/api/recruiter/interviews/applications/${applicationId}`, {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function getRecruiterInterviews() {
  return apiClient<RecruiterInterview[]>("/api/recruiter/interviews");
}

export function updateRecruiterInterviewStatus(
  interviewId: string | number,
  payload: UpdateInterviewStatusRequest
) {
  return apiClient<RecruiterInterview>(`/api/recruiter/interviews/${interviewId}/status`, {
    method: "PATCH",
    body: JSON.stringify(payload)
  });
}
