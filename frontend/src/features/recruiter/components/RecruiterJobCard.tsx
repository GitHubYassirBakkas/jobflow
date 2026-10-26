import { ArrowRight, BriefcaseBusiness, ClipboardList, MapPin } from "lucide-react";
import { Link } from "react-router-dom";
import { formatEnumLabel } from "../../../lib/formatters";
import type { RecruiterJob } from "../../../types/recruiter";

interface RecruiterJobCardProps {
  job: RecruiterJob;
  owned: boolean;
}

export function RecruiterJobCard({ job, owned }: RecruiterJobCardProps) {
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
          <span>{owned ? "Your company" : "Global listing"}</span>
        </div>
      </div>
      <div className="card-actions-row">
        <Link className="text-button" to={`/recruiter/jobs/${job.id}`}>
          View role <ArrowRight size={16} />
        </Link>
        {owned ? (
          <Link className="secondary-button" to={`/recruiter/jobs/${job.id}/applications`}>
            <ClipboardList size={16} />
            Applications
          </Link>
        ) : null}
      </div>
    </article>
  );
}
