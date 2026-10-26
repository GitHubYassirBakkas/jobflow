import type { ApplicationStatus, EmploymentType, InterviewStatus, InterviewType } from "./candidate";

export interface AdminDashboardStats {
  totalUsers: number;
  totalCandidates: number;
  totalRecruiters: number;
  totalAdmins: number;
  totalCompanies: number;
  totalJobs: number;
  totalApplications: number;
}

export interface AdminUser {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  role: "CANDIDATE" | "RECRUITER" | "ADMIN";
  enabled: boolean;
  createdAt: string;
}

export interface AdminCompany {
  id: number;
  name: string;
  description: string;
  website: string;
  location: string;
  createdAt: string;
}

export interface UpdateAdminCompanyRequest {
  name: string;
  description: string;
  website: string;
  location: string;
}

export interface AdminCandidateProfile {
  id: number;
  userId: number;
  firstName: string;
  lastName: string;
  email: string;
  phone: string | null;
  location: string | null;
  bio: string | null;
  skills: string | null;
  cvFileName: string | null;
  createdAt: string;
  profileCompleteness: number;
}

export interface AdminJob {
  id: number;
  title: string;
  description: string;
  location: string;
  employmentType: EmploymentType;
  active: boolean;
  companyId: number;
  companyName: string;
  createdById: number;
  createdByName: string;
  createdAt: string;
}

export interface UpdateAdminJobRequest {
  title: string;
  description: string;
  location: string;
  employmentType: EmploymentType;
}

export interface AdminApplication {
  id: number;
  jobId: number;
  jobTitle: string;
  companyName: string;
  candidateId: number;
  candidateName: string;
  candidateEmail: string;
  status: ApplicationStatus;
  cvFileName: string | null;
  appliedAt: string;
}

export interface AdminInterview {
  id: number;
  applicationId: number;
  jobId: number;
  jobTitle: string;
  candidateId: number;
  candidateName: string;
  candidateEmail: string;
  scheduledAt: string;
  type: InterviewType;
  meetingLink: string | null;
  location: string | null;
  notes: string | null;
  status: InterviewStatus;
}
