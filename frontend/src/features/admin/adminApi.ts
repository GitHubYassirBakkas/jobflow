import { apiBlobClient, apiClient } from "../../lib/apiClient";
import type { ApplicationStatus, InterviewStatus } from "../../types/candidate";
import type {
  AdminApplication,
  AdminCandidateProfile,
  AdminCompany,
  AdminDashboardStats,
  AdminInterview,
  AdminJob,
  AdminUser,
  UpdateAdminCompanyRequest,
  UpdateAdminJobRequest
} from "../../types/admin";

export function getAdminDashboard() {
  return apiClient<AdminDashboardStats>("/api/admin/dashboard");
}

export function getAdminUsers() {
  return apiClient<AdminUser[]>("/api/admin/users");
}

export function updateAdminUserEnabled(userId: string | number, enabled: boolean) {
  return apiClient<AdminUser>(`/api/admin/users/${userId}/enabled?enabled=${enabled}`, {
    method: "PATCH"
  });
}

export function getAdminCompanies() {
  return apiClient<AdminCompany[]>("/api/admin/companies");
}

export function updateAdminCompany(companyId: string | number, payload: UpdateAdminCompanyRequest) {
  return apiClient<AdminCompany>(`/api/admin/companies/${companyId}`, {
    method: "PUT",
    body: JSON.stringify(payload)
  });
}

export function getAdminCandidateProfile(userId: string | number) {
  return apiClient<AdminCandidateProfile>(`/api/admin/candidates/${userId}`);
}

export function getAdminCandidateCv(userId: string | number) {
  return apiBlobClient(`/api/admin/candidates/${userId}/cv`);
}

export function getAdminJobs() {
  return apiClient<AdminJob[]>("/api/admin/jobs");
}

export function updateAdminJobActive(jobId: string | number, active: boolean) {
  return apiClient<AdminJob>(`/api/admin/jobs/${jobId}/active?active=${active}`, {
    method: "PATCH"
  });
}

export function updateAdminJob(jobId: string | number, payload: UpdateAdminJobRequest) {
  return apiClient<AdminJob>(`/api/admin/jobs/${jobId}`, {
    method: "PUT",
    body: JSON.stringify(payload)
  });
}

export function getAdminApplications() {
  return apiClient<AdminApplication[]>("/api/admin/applications");
}

export function updateAdminApplicationStatus(applicationId: string | number, status: ApplicationStatus) {
  return apiClient<AdminApplication>(`/api/admin/applications/${applicationId}/status?status=${status}`, {
    method: "PATCH"
  });
}

export function getAdminApplicationCv(applicationId: string | number) {
  return apiBlobClient(`/api/admin/applications/${applicationId}/cv`);
}

export function getAdminInterviews() {
  return apiClient<AdminInterview[]>("/api/admin/interviews");
}

export function updateAdminInterviewStatus(interviewId: string | number, status: InterviewStatus) {
  return apiClient<AdminInterview>(`/api/admin/interviews/${interviewId}/status?status=${status}`, {
    method: "PATCH"
  });
}
