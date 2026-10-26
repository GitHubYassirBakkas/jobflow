import type { RecruiterJob, RecruiterProfile } from "../../types/recruiter";

export function isRecruiterOwnedJob(job: RecruiterJob, profile: RecruiterProfile | null) {
  return Boolean(profile && job.companyId === profile.companyId);
}
