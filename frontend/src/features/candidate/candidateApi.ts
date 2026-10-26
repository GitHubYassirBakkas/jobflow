import { apiBlobClient, apiClient } from "../../lib/apiClient";
import type {
  CandidateApplication,
  CandidateDashboard,
  CandidateApplicationTimelineEvent,
  CandidateInterview,
  CandidateProfileInput,
  CandidateProfile,
  GetJobsParams,
  Job,
  JobDetails,
  PageResponse,
  SavedJobResponse
} from "../../types/candidate";

export function getCandidateDashboard() {
  return apiClient<CandidateDashboard>("/api/candidate/dashboard");
}

export function getJobs(params: GetJobsParams = {}) {
  const searchParams = new URLSearchParams();
  const search = params.search?.trim();
  const location = params.location?.trim();

  if (search) {
    searchParams.set("search", search);
  }

  if (location) {
    searchParams.set("location", location);
  }

  if (params.employmentType) {
    searchParams.set("employmentType", params.employmentType);
  }

  if (params.page !== undefined) {
    searchParams.set("page", String(params.page));
  }

  if (params.size !== undefined) {
    searchParams.set("size", String(params.size));
  }

  const queryString = searchParams.toString();

  return apiClient<PageResponse<Job>>(
    queryString ? `/api/jobs?${queryString}` : "/api/jobs"
  );
}

export function getJob(jobId: string | number) {
  return apiClient<JobDetails>(`/api/jobs/${jobId}`);
}

export function getSavedJobs(page = 0, size = 10) {
  return apiClient<PageResponse<SavedJobResponse>>(
    `/api/candidate/saved-jobs?page=${page}&size=${size}`
  );
}

export function getSavedJobIds() {
  return apiClient<number[]>("/api/candidate/saved-jobs/job-ids");
}

export function saveJob(jobId: string | number) {
  return apiClient<SavedJobResponse>(`/api/candidate/saved-jobs/${jobId}`, {
    method: "POST"
  });
}

export function unsaveJob(jobId: string | number) {
  return apiClient<void>(`/api/candidate/saved-jobs/${jobId}`, {
    method: "DELETE"
  });
}

export function applyToJob(jobId: string | number, file?: File | null) {
  const formData = new FormData();

  if (file) {
    formData.append("file", file);
  }

  return apiClient<CandidateApplication>(`/api/jobs/${jobId}/apply`, {
    method: "POST",
    body: formData
  });
}

export function getMyApplications(page = 0, size = 10) {
  return apiClient<PageResponse<CandidateApplication>>(
    `/api/applications/me?page=${page}&size=${size}`
  );
}

export function getMyApplication(applicationId: string | number) {
  return apiClient<CandidateApplication>(`/api/applications/me/${applicationId}`);
}

export function getMyApplicationForJob(jobId: string | number) {
  return apiClient<CandidateApplication | null>(
    `/api/applications/me/jobs/${jobId}`
  );
}

export function withdrawMyApplication(applicationId: string | number) {
  return apiClient<CandidateApplication>(
    `/api/applications/me/${applicationId}/withdraw`,
    {
      method: "PATCH"
    }
  );
}

export function getMyApplicationTimeline(applicationId: string | number) {
  return apiClient<CandidateApplicationTimelineEvent[]>(
    `/api/applications/me/${applicationId}/timeline`
  );
}

export function getMyApplicationCv(applicationId: string | number) {
  return apiBlobClient(`/api/applications/me/${applicationId}/cv`);
}

export function getMyInterviews() {
  return apiClient<CandidateInterview[]>("/api/candidate/interviews");
}

export function getInterviewByApplication(applicationId: string | number) {
  return apiClient<CandidateInterview>(`/api/candidate/interviews/applications/${applicationId}`);
}

export function getMyCandidateProfile() {
  return apiClient<CandidateProfile>("/api/candidate/profile/me");
}

export function updateMyCandidateProfile(data: CandidateProfileInput) {
  return apiClient<CandidateProfile>("/api/candidate/profile", {
    method: "PUT",
    body: JSON.stringify(data)
  });
}

export function getMyCandidateCv() {
  return apiBlobClient("/api/candidate/profile/cv");
}

export function uploadMyCandidateCv(file: File) {
  const formData = new FormData();
  formData.append("file", file);

  return apiClient<string>("/api/candidate/profile/cv", {
    method: "POST",
    body: formData
  });
}

export function deleteMyCandidateCv() {
  return apiClient<void>("/api/candidate/profile/cv", {
    method: "DELETE"
  });
}
