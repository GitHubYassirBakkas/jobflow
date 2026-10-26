export type EmploymentType = "FULL_TIME" | "PART_TIME" | "INTERNSHIP" | "CONTRACT" | "FREELANCE";

export type ApplicationStatus = "APPLIED" | "REVIEWING" | "INTERVIEW" | "ACCEPTED" | "REJECTED" | "WITHDRAWN";

export type ApplicationTimelineEventType = "BASELINE" | "TRANSITION";

export type InterviewType = "ONLINE" | "ONSITE" | "PHONE";

export type InterviewStatus = "SCHEDULED" | "COMPLETED" | "CANCELLED";

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface Job {
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

export interface JobDetails extends Job {
  updatedAt: string;
  companyDescription: string | null;
  companyWebsite: string | null;
  companyLocation: string | null;
}

export interface SavedJobResponse {
  id: number;
  job: Job;
  savedAt: string;
}

export interface GetJobsParams {
  search?: string;
  location?: string;
  employmentType?: EmploymentType;
  page?: number;
  size?: number;
}

export interface CandidateApplication {
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

export interface CandidateApplicationTimelineEvent {
  status: ApplicationStatus;
  changedAt: string;
  eventType: ApplicationTimelineEventType;
}

export interface CandidateInterview {
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

export interface CandidateDashboard {
  totalApplications: number;
  activeApplications: number;
  withdrawnApplications: number;
  upcomingInterviews: number;
  savedJobs: number;
  recentApplications: CandidateApplication[];
  nextInterviews: CandidateInterview[];
}

export interface CandidateProfile {
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

export interface CandidateProfileInput {
  phone: string;
  location: string;
  bio: string;
  skills: string;
}
