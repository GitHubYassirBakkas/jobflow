import { useEffect, useState } from "react";
import { PlusCircle } from "lucide-react";
import { Link } from "react-router-dom";
import { EmptyState, ErrorState, LoadingState } from "../../components/AsyncState";
import { getAllRecruiterJobs, getRecruiterProfile } from "../../features/recruiter/recruiterApi";
import { RecruiterJobCard } from "../../features/recruiter/components/RecruiterJobCard";
import { isRecruiterOwnedJob } from "../../features/recruiter/recruiterUtils";
import { getFriendlyErrorMessage } from "../../lib/errors";
import type { RecruiterJob, RecruiterProfile } from "../../types/recruiter";

export function RecruiterJobsPage() {
  const [jobs, setJobs] = useState<RecruiterJob[]>([]);
  const [profile, setProfile] = useState<RecruiterProfile | null>(null);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let isMounted = true;

    async function loadJobs() {
      setIsLoading(true);
      setError("");

      try {
        const [profileResponse, jobsResponse] = await Promise.all([getRecruiterProfile(), getAllRecruiterJobs()]);

        if (isMounted) {
          setProfile(profileResponse);
          setJobs(jobsResponse);
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

    void loadJobs();

    return () => {
      isMounted = false;
    };
  }, []);

  if (isLoading) {
    return <LoadingState message="Loading jobs..." />;
  }

  if (error) {
    return <ErrorState title="Could not load jobs" message={error} />;
  }

  if (jobs.length === 0) {
    return (
      <EmptyState
        title="No active jobs"
        message="Create a job for your company to start receiving applications."
        action={
          <Link className="primary-button" to="/recruiter/jobs/new">
            <PlusCircle size={18} />
            Create job
          </Link>
        }
      />
    );
  }

  const ownedCount = jobs.filter((job) => isRecruiterOwnedJob(job, profile)).length;

  return (
    <div className="page-stack">
      <section className="section-heading">
        <div>
          <p className="eyebrow">Jobs</p>
          <h2>Active jobs</h2>
        </div>
        <div className="heading-actions">
          <span>{ownedCount} owned by your company</span>
          <Link className="primary-button" to="/recruiter/jobs/new">
            <PlusCircle size={18} />
            Create job
          </Link>
        </div>
      </section>
      <section className="jobs-grid">
        {jobs.map((job) => (
          <RecruiterJobCard job={job} key={job.id} owned={isRecruiterOwnedJob(job, profile)} />
        ))}
      </section>
    </div>
  );
}
