import { CalendarClock, ExternalLink, Link as LinkIcon, MapPin, MessageSquareText } from "lucide-react";
import { Link } from "react-router-dom";
import { InterviewStatusBadge } from "../../../components/StatusBadge";
import { formatDateTime, formatEnumLabel } from "../../../lib/formatters";
import type { CandidateInterview } from "../../../types/candidate";

export function InterviewCard({ interview }: { interview: CandidateInterview }) {
  return (
    <article className="list-card">
      <div>
        <p className="eyebrow">{formatEnumLabel(interview.type)}</p>
        <h2>{interview.jobTitle}</h2>
        <p className="muted-line">
          <CalendarClock size={16} />
          {formatDateTime(interview.scheduledAt)}
        </p>
        {interview.meetingLink ? (
          <p className="muted-line">
            <LinkIcon size={16} />
            <a href={interview.meetingLink} rel="noreferrer" target="_blank">
              Open meeting
            </a>
          </p>
        ) : null}
        {interview.location ? (
          <p className="muted-line">
            <MapPin size={16} />
            {interview.location}
          </p>
        ) : null}
        {interview.notes ? (
          <p className="muted-line">
            <MessageSquareText size={16} />
            {interview.notes}
          </p>
        ) : null}
      </div>
      <div className="list-card-actions">
        <InterviewStatusBadge status={interview.status} />
        <Link className="secondary-button" to={`/candidate/interviews/${interview.applicationId}`}>
          Details <ExternalLink size={16} />
        </Link>
      </div>
    </article>
  );
}
