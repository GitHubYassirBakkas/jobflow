import { ArrowRight, BriefcaseBusiness, MapPin } from "lucide-react";
import { Link, useLocation } from "react-router-dom";
import { formatEnumLabel } from "../../../lib/formatters";
import type { Job } from "../../../types/candidate";
import { BookmarkJobButton } from "./BookmarkJobButton";

interface JobCardProps {
  job: Job;
  isSaved?: boolean;
  isSavedActionPending?: boolean;
  onSavedToggle?: (job: Job) => void;
}

export function JobCard({
  job,
  isSaved = false,
  isSavedActionPending = false,
  onSavedToggle
}: JobCardProps) {
  const location = useLocation();
  const returnToJobs = `${location.pathname}${location.search}`;

  return (
    <article className="job-card">
      <div className="card-icon">
        <BriefcaseBusiness size={20} />
      </div>
      <div className="job-card-body">
        <p className="eyebrow">{job.companyName}</p>
        <h2>{job.title}</h2>
        <div className="meta-row">
          <span>
            <MapPin size={16} />
            {job.location}
          </span>
          <span>{formatEnumLabel(job.employmentType)}</span>
        </div>
      </div>
      <div className="job-card-actions">
        <Link className="text-button" state={{ returnToJobs }} to={`/candidate/jobs/${job.id}`}>
          View role <ArrowRight size={16} />
        </Link>
        {onSavedToggle ? (
          <BookmarkJobButton
            isPending={isSavedActionPending}
            isSaved={isSaved}
            jobTitle={job.title}
            onToggle={() => onSavedToggle(job)}
          />
        ) : null}
      </div>
    </article>
  );
}
