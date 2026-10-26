import type { ApplicationStatus } from "../../types/candidate";

export type AssignableApplicationStatus = Exclude<ApplicationStatus, "WITHDRAWN">;

export const assignableApplicationStatuses: AssignableApplicationStatus[] = [
  "APPLIED",
  "REVIEWING",
  "INTERVIEW",
  "ACCEPTED",
  "REJECTED"
];

export function isApplicationStatusAssignable(
  status: ApplicationStatus
): status is AssignableApplicationStatus {
  return assignableApplicationStatuses.includes(status as AssignableApplicationStatus);
}
