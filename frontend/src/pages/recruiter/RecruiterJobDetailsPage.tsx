import { useEffect, useState } from "react";
import { ArrowLeft, ClipboardList, MapPin } from "lucide-react";
import { Link, useLocation, useParams } from "react-router-dom";
import { EmptyState, ErrorState, LoadingState } from "../../components/AsyncState";
import { getRecruiterJob, getRecruiterProfile } from "../../features/recruiter/recruiterApi";
import { isRecruiterOwnedJob } from "../../features/recruiter/recruiterUtils";
import { getFriendlyErrorMessage } from "../../lib/errors";
import { formatDate, formatEnumLabel } from "../../lib/formatters";
import type { RecruiterJob, RecruiterProfile } from "../../types/recruiter";

interface LocationState {
  success?: string;
}

export function RecruiterJobDetailsPage() {
  const { jobId } = useParams();
  const location = useLocation();
  const [job, setJob] = useState<RecruiterJob | null>(null);
  const [profile, setProfile] = useState<RecruiterProfile | null>(null);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const successMessage = (location.state as LocationState | null)?.success;

  useEffect(() => {
    let isMounted = true;

    async function loadJob() {
      if (!jobId) {
        setError("We could not find that job.");
        setIsLoading(false);
        return;
      }

      setIsLoading(true);
      setError("");

      try {
        const [jobResponse, profileResponse] = await Promise.all([getRecruiterJob(jobId), getRecruiterProfile()]);

        if (isMounted) {
          setJob(jobResponse);
          setProfile(profileResponse);
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

    void loadJob();

    return () => {
      isMounted = false;
    };
  }, [jobId]);

  if (isLoading) {
    return <LoadingState message="Loading job details..." />;
  }

  if (error) {
    return <ErrorState title="Could not load job" message={error} />;
  }

  if (!job) {
    return <EmptyState title="Job not found" message="This job is not available." />;
  }

  const owned = isRecruiterOwnedJob(job, profile);

  return (
    <article className="detail-panel">
      <Link className="back-link" to="/recruiter/jobs">
        <ArrowLeft size={16} />
        Back to jobs
      </Link>
      {successMessage ? <p className="form-success">{successMessage}</p> : null}
      <div className="detail-header">
        <div>
          <p className="eyebrow">{job.companyName}</p>
          <h2>{job.title}</h2>
          <div className="meta-row">
            <span>
              <MapPin size={16} />
              {job.location}
            </span>
            <span>{formatEnumLabel(job.employmentType)}</span>
            <span>{job.active ? "Active" : "Inactive"}</span>
            <span>Posted {formatDate(job.createdAt)}</span>
          </div>
        </div>
        {owned ? (
          <Link className="primary-button" to={`/recruiter/jobs/${job.id}/applications`}>
            <ClipboardList size={18} />
            View applications
          </Link>
        ) : null}
      </div>
      {!owned ? (
        <p className="form-error">This job is visible globally, but it is not owned by your company.</p>
      ) : null}
      <section className="detail-section">
        <h3>Job description</h3>
        <p>{job.description}</p>
      </section>
    </article>
  );
}
