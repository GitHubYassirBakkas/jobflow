import type {
  ApplicationStatus,
  CandidateApplication,
  CandidateInterview,
  EmploymentType,
  InterviewStatus,
  InterviewType,
  Job
} from "./candidate";

export interface RecruiterProfile {
  id: number;
  userId: number;
  firstName: string;
  lastName: string;
  email: string;
  jobTitle: string;
  companyId: number;
  companyName: string;
}

export interface CreateJobRequest {
  title: string;
  description: string;
  location: string;
  employmentType: EmploymentType;
}

export interface UpdateApplicationStatusRequest {
  status: ApplicationStatus;
}

export interface ScheduleInterviewRequest {
  scheduledAt: string;
  type: InterviewType;
  meetingLink: string | null;
  location: string | null;
  notes: string | null;
}

export interface UpdateInterviewStatusRequest {
  status: InterviewStatus;
}

export type RecruiterJob = Job;
export type RecruiterApplication = CandidateApplication;
export type RecruiterInterview = CandidateInterview;
