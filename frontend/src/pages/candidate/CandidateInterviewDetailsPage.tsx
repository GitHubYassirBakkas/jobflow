import { useEffect, useState } from "react";
import { ArrowLeft, CalendarClock, Link as LinkIcon, MapPin, MessageSquareText } from "lucide-react";
import { Link, useParams } from "react-router-dom";
import { EmptyState, ErrorState, LoadingState } from "../../components/AsyncState";
import { InterviewStatusBadge } from "../../components/StatusBadge";
import { getInterviewByApplication } from "../../features/candidate/candidateApi";
import { getFriendlyErrorMessage } from "../../lib/errors";
import { formatDateTime, formatEnumLabel } from "../../lib/formatters";
import type { CandidateInterview } from "../../types/candidate";

export function CandidateInterviewDetailsPage() {
  const { applicationId } = useParams();
  const [interview, setInterview] = useState<CandidateInterview | null>(null);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let isMounted = true;

    async function loadInterview() {
      if (!applicationId) {
        setError("We could not find that interview.");
        setIsLoading(false);
        return;
      }

      setIsLoading(true);
      setError("");

      try {
        const response = await getInterviewByApplication(applicationId);

        if (isMounted) {
          setInterview(response);
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

    void loadInterview();

    return () => {
      isMounted = false;
    };
  }, [applicationId, reloadKey]);

  function retryLoad() {
    setReloadKey((current) => current + 1);
  }

  if (isLoading) {
    return <LoadingState message="Loading interview..." />;
  }

  if (error) {
    return (
      <ErrorState
        title="Could not load interview"
        message={error}
        action={
          applicationId ? (
            <button className="secondary-button" onClick={retryLoad} type="button">
              Retry
            </button>
          ) : undefined
        }
      />
    );
  }

  if (!interview) {
    return <EmptyState title="Interview not found" message="This interview is not available." />;
  }

  return (
    <article className="detail-panel">
      <Link className="back-link" to="/candidate/interviews">
        <ArrowLeft size={16} />
        Back to interviews
      </Link>
      <div className="detail-header">
        <div>
          <p className="eyebrow">{formatEnumLabel(interview.type)} interview</p>
          <h2>{interview.jobTitle}</h2>
        </div>
        <InterviewStatusBadge status={interview.status} />
      </div>
      <section className="detail-grid">
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
      </section>
      {interview.notes ? (
        <section className="detail-section">
          <h3>
            <MessageSquareText size={18} />
            Notes
          </h3>
          <p>{interview.notes}</p>
        </section>
      ) : null}
    </article>
  );
}
