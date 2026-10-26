import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { EmptyState, ErrorState, LoadingState } from "../../components/AsyncState";
import { getMyInterviews } from "../../features/candidate/candidateApi";
import { InterviewCard } from "../../features/candidate/components/InterviewCard";
import { getFriendlyErrorMessage } from "../../lib/errors";
import type { CandidateInterview } from "../../types/candidate";

export function CandidateInterviewsPage() {
  const [interviews, setInterviews] = useState<CandidateInterview[]>([]);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let isMounted = true;

    async function loadInterviews() {
      setIsLoading(true);
      setError("");

      try {
        const response = await getMyInterviews();

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
  }, [reloadKey]);

  function retryLoad() {
    setReloadKey((current) => current + 1);
  }

  if (isLoading) {
    return <LoadingState message="Loading interview details..." />;
  }

  if (error) {
    return (
      <ErrorState
        title="Could not load interviews"
        message={error}
        action={
          <button className="secondary-button" onClick={retryLoad} type="button">
            Retry
          </button>
        }
      />
    );
  }

  if (interviews.length === 0) {
    return (
      <EmptyState
        title="No interviews scheduled"
        message="Interviews scheduled for your applications will appear here, regardless of the current application status."
        action={
          <Link className="secondary-button" to="/candidate/applications">
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
          <h2>Your interviews</h2>
        </div>
        <span>{interviews.length} scheduled</span>
      </section>
      <section className="list-stack">
        {[...interviews]
          .sort((first, second) => new Date(first.scheduledAt).getTime() - new Date(second.scheduledAt).getTime())
          .map((interview) => (
            <InterviewCard interview={interview} key={interview.id} />
          ))}
      </section>
    </div>
  );
}
