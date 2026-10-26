import { ChangeEvent, useEffect, useMemo, useState } from "react";
import { CalendarClock, Link as LinkIcon, LoaderCircle, MapPin, MessageSquareText } from "lucide-react";
import { Link } from "react-router-dom";
import { EmptyState, ErrorState, LoadingState } from "../../components/AsyncState";
import { InterviewStatusBadge } from "../../components/StatusBadge";
import { getRecruiterInterviews, updateRecruiterInterviewStatus } from "../../features/recruiter/recruiterApi";
import { getFriendlyErrorMessage } from "../../lib/errors";
import { formatDateTime, formatEnumLabel } from "../../lib/formatters";
import type { InterviewStatus } from "../../types/candidate";
import type { RecruiterInterview } from "../../types/recruiter";

const interviewStatuses: InterviewStatus[] = ["SCHEDULED", "COMPLETED", "CANCELLED"];

export function RecruiterInterviewsPage() {
  const [interviews, setInterviews] = useState<RecruiterInterview[]>([]);
  const [error, setError] = useState("");
  const [feedback, setFeedback] = useState<{ type: "success" | "error"; message: string } | null>(null);
  const [savingInterviewId, setSavingInterviewId] = useState<number | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let isMounted = true;

    async function loadInterviews() {
      setIsLoading(true);
      setError("");

      try {
        const response = await getRecruiterInterviews();

        if (isMounted) {
          setInterviews(response);
        }
      } catch (caughtError) {
        if (isMounted) {
          setError(getFriendlyErrorMessage(caughtError));
        }
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    void loadInterviews();

    return () => {
      isMounted = false;
    };
  }, []);

  const sortedInterviews = useMemo(() => {
    return [...interviews].sort(
      (first, second) => new Date(first.scheduledAt).getTime() - new Date(second.scheduledAt).getTime()
    );
  }, [interviews]);

  async function handleStatusChange(interview: RecruiterInterview, event: ChangeEvent<HTMLSelectElement>) {
    const nextStatus = event.target.value as InterviewStatus;

    if (nextStatus === interview.status) {
      return;
    }

    setSavingInterviewId(interview.id);
    setFeedback(null);

    try {
      const updatedInterview = await updateRecruiterInterviewStatus(interview.id, { status: nextStatus });
      setInterviews((current) =>
        current.map((item) => (item.id === updatedInterview.id ? updatedInterview : item))
      );
      setFeedback({ type: "success", message: "Interview status updated." });
    } catch (caughtError) {
      setFeedback({ type: "error", message: getFriendlyErrorMessage(caughtError) });
    } finally {
      setSavingInterviewId(null);
    }
  }

  if (isLoading) {
    return <LoadingState message="Loading recruiter interviews..." />;
  }

  if (error) {
    return <ErrorState title="Could not load interviews" message={error} />;
  }

  if (interviews.length === 0) {
    return (
      <EmptyState
        title="No interviews yet"
        message="Scheduled interviews for your company will appear here."
        action={
          <Link className="secondary-button" to="/recruiter/applications">
            View applications
          </Link>
        }
      />
    );
  }

  return (
    <div className="page-stack">
      <section className="section-heading">
        <div>
          <p className="eyebrow">Interviews</p>
          <h2>Company interviews</h2>
        </div>
        <span>{interviews.length} total</span>
      </section>

      {feedback ? (
        <p className={feedback.type === "success" ? "form-success" : "form-error"}>{feedback.message}</p>
      ) : null}

      <section className="list-stack">
        {sortedInterviews.map((interview) => (
          <article className="list-card recruiter-application-card" key={interview.id}>
            <div className="application-main">
              <p className="eyebrow">{formatEnumLabel(interview.type)} interview</p>
              <h2>{interview.jobTitle}</h2>
              <p className="muted-line">
                {interview.candidateName} | {interview.candidateEmail}
              </p>
              <p className="muted-line">
                <CalendarClock size={16} />
                {formatDateTime(interview.scheduledAt)}
              </p>
            </div>

            <div className="interview-status-actions">
              <InterviewStatusBadge status={interview.status} />
              <label>
                <span>Status</span>
                <select
                  disabled={savingInterviewId === interview.id}
                  onChange={(event) => void handleStatusChange(interview, event)}
                  value={interview.status}
                >
                  {interviewStatuses.map((status) => (
                    <option key={status} value={status}>
                      {formatEnumLabel(status)}
                    </option>
                  ))}
                </select>
              </label>
              {savingInterviewId === interview.id ? (
                <span className="saving-line">
                  <LoaderCircle className="spin" size={16} />
                  Saving
                </span>
              ) : null}
            </div>

            <section className="scheduled-interview-panel">
              <div className="detail-grid">
                <div className="info-tile">
                  <CalendarClock size={18} />
                  <span>Date and time</span>
                  <strong>{formatDateTime(interview.scheduledAt)}</strong>
                </div>
                {interview.meetingLink ? (
                  <div className="info-tile">
                    <LinkIcon size={18} />
                    <span>Meeting link</span>
                    <a href={interview.meetingLink} rel="noreferrer" target="_blank">
                      Open meeting
                    </a>
                  </div>
                ) : null}
                {interview.location ? (
                  <div className="info-tile">
                    <MapPin size={18} />
                    <span>Location</span>
                    <strong>{interview.location}</strong>
                  </div>
                ) : null}
              </div>
              {interview.notes ? (
                <div className="detail-section">
                  <h3>
                    <MessageSquareText size={18} />
                    Notes
                  </h3>
                  <p>{interview.notes}</p>
                </div>
              ) : null}
            </section>
          </article>
        ))}
      </section>
    </div>
  );
}
