import { useEffect, useState } from "react";
import { BriefcaseBusiness, ClipboardList } from "lucide-react";
import { Link } from "react-router-dom";
import { EmptyState, ErrorState, LoadingState } from "../../components/AsyncState";
import { ApplicationStatusBadge } from "../../components/StatusBadge";
import {
  getAllRecruiterJobs,
  getRecruiterJobApplications,
  getRecruiterProfile
} from "../../features/recruiter/recruiterApi";
import { isRecruiterOwnedJob } from "../../features/recruiter/recruiterUtils";
import { getFriendlyErrorMessage } from "../../lib/errors";
import { formatDate } from "../../lib/formatters";
import type { RecruiterApplication, RecruiterJob } from "../../types/recruiter";

interface RecruiterApplicationsState {
  ownedJobs: RecruiterJob[];
  applications: RecruiterApplication[];
  partialError: boolean;
}

export function RecruiterApplicationsPage() {
  const [data, setData] = useState<RecruiterApplicationsState | null>(null);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let isMounted = true;

    async function loadApplications() {
      setIsLoading(true);
      setError("");

      try {
        const [profile, jobs] = await Promise.all([getRecruiterProfile(), getAllRecruiterJobs()]);
        const ownedJobs = jobs.filter((job) => isRecruiterOwnedJob(job, profile));
        const applicationResults = await Promise.allSettled(
          ownedJobs.map((job) => getRecruiterJobApplications(job.id))
        );
        const applications = applicationResults.flatMap((result) =>
          result.status === "fulfilled" ? result.value : []
        );

        if (isMounted) {
          setData({
            ownedJobs,
            applications,
            partialError: applicationResults.some((result) => result.status === "rejected")
          });
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

    void loadApplications();

    return () => {
      isMounted = false;
    };
  }, []);

  if (isLoading) {
    return <LoadingState message="Loading recruiter applications..." />;
  }

  if (error) {
    return <ErrorState title="Could not load applications" message={error} />;
  }

  if (!data || data.ownedJobs.length === 0) {
    return (
      <EmptyState
        title="Choose a job first"
        message="Applications are available through jobs owned by your company."
        action={
          <Link className="primary-button" to="/recruiter/jobs/new">
            Create job
          </Link>
        }
      />
    );
  }

  return (
    <div className="page-stack">
      <section className="section-heading">
        <div>
          <p className="eyebrow">Applications</p>
          <h2>Applications by job</h2>
        </div>
        <span>{data.applications.length} aggregated safely</span>
      </section>

      {data.partialError ? (
        <p className="form-error">Some job applications could not be loaded. Open a job directly to retry.</p>
      ) : null}

      {data.applications.length === 0 ? (
        <EmptyState
          title="No applications yet"
          message="Open one of your jobs to review candidates when applications arrive."
          action={
            <Link className="secondary-button" to="/recruiter/jobs">
              <BriefcaseBusiness size={16} />
              View jobs
            </Link>
          }
        />
      ) : (
        <section className="list-stack">
          {data.applications
            .sort((first, second) => new Date(second.appliedAt).getTime() - new Date(first.appliedAt).getTime())
            .map((application) => (
              <article className="list-card" key={application.id}>
                <div>
                  <p className="eyebrow">{application.jobTitle}</p>
                  <h2>{application.candidateName}</h2>
                  <p className="muted-line">
                    {application.candidateEmail} | Applied {formatDate(application.appliedAt)}
                  </p>
                </div>
                <div className="list-card-actions">
                  <ApplicationStatusBadge status={application.status} />
                  <Link className="secondary-button" to={`/recruiter/jobs/${application.jobId}/applications`}>
                    <ClipboardList size={16} />
                    Open pipeline
                  </Link>
                </div>
              </article>
            ))}
        </section>
      )}
    </div>
  );
}
