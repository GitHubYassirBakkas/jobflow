import { CalendarDays, ExternalLink, FileText } from "lucide-react";
import { Link } from "react-router-dom";
import { ApplicationStatusBadge } from "../../../components/StatusBadge";
import { formatDate } from "../../../lib/formatters";
import type { CandidateApplication } from "../../../types/candidate";

interface ApplicationCardProps {
  application: CandidateApplication;
  returnToApplications?: string;
}

export function getApplicationDetailsPath(application: CandidateApplication) {
  return `/candidate/applications/${application.id}`;
}

export function ApplicationCard({
  application,
  returnToApplications
}: ApplicationCardProps) {
  return (
    <article className="list-card">
      <div>
        <p className="eyebrow">{application.companyName}</p>
        <h2>{application.jobTitle}</h2>
        <p className="muted-line">
          <CalendarDays size={16} />
          Applied {formatDate(application.appliedAt)}
        </p>
      </div>
      <div className="list-card-actions">
        <ApplicationStatusBadge status={application.status} />
        <Link
          className="text-button"
          state={returnToApplications ? { returnToApplications } : undefined}
          to={getApplicationDetailsPath(application)}
        >
          View details <FileText size={16} />
        </Link>
        {application.status === "INTERVIEW" ? (
          <Link className="secondary-button" to={`/candidate/interviews/${application.id}`}>
            Interview <ExternalLink size={16} />
          </Link>
        ) : null}
      </div>
    </article>
  );
}
