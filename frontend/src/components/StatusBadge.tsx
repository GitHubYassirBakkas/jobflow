import { formatEnumLabel } from "../lib/formatters";
import type { ApplicationStatus, InterviewStatus } from "../types/candidate";

type BadgeTone = "neutral" | "info" | "success" | "danger" | "warning";

const applicationTone: Record<ApplicationStatus, BadgeTone> = {
  APPLIED: "neutral",
  REVIEWING: "info",
  INTERVIEW: "warning",
  ACCEPTED: "success",
  REJECTED: "danger",
  WITHDRAWN: "neutral"
};

const interviewTone: Record<InterviewStatus, BadgeTone> = {
  SCHEDULED: "warning",
  COMPLETED: "success",
  CANCELLED: "danger"
};

interface StatusBadgeProps {
  status: ApplicationStatus | InterviewStatus;
}

export function ApplicationStatusBadge({ status }: StatusBadgeProps) {
  const label = formatEnumLabel(status);

  return (
    <span
      aria-label={label}
      className={`status-badge status-${applicationTone[status as ApplicationStatus]}`}
    >
      {label}
    </span>
  );
}

export function InterviewStatusBadge({ status }: { status: InterviewStatus }) {
  return <span className={`status-badge status-${interviewTone[status]}`}>{formatEnumLabel(status)}</span>;
}
