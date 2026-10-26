import { useEffect, useState } from "react";
import { BriefcaseBusiness, CalendarPlus, ClipboardCheck, PlusCircle } from "lucide-react";
import { Link } from "react-router-dom";
import { EmptyState, ErrorState, LoadingState } from "../../components/AsyncState";
import {
  getAllRecruiterJobs,
  getRecruiterJobApplications,
  getRecruiterProfile
} from "../../features/recruiter/recruiterApi";
import { isRecruiterOwnedJob } from "../../features/recruiter/recruiterUtils";
import { getFriendlyErrorMessage } from "../../lib/errors";
import type { RecruiterApplication, RecruiterJob, RecruiterProfile } from "../../types/recruiter";

interface DashboardState {
  profile: RecruiterProfile;
  ownedJobs: RecruiterJob[];
  applications: RecruiterApplication[];
  partialApplicationError: boolean;
}

export function RecruiterHomePage() {
  const [data, setData] = useState<DashboardState | null>(null);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let isMounted = true;

    async function loadDashboard() {
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
            profile,
            ownedJobs,
            applications,
            partialApplicationError: applicationResults.some((result) => result.status === "rejected")
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

    void loadDashboard();

    return () => {
      isMounted = false;
    };
  }, []);

  if (isLoading) {
    return <LoadingState message="Loading recruiter workspace..." />;
  }

  if (error) {
    return <ErrorState title="Could not load recruiter dashboard" message={error} />;
  }

  if (!data) {
    return <EmptyState title="Workspace unavailable" message="Your recruiter workspace could not be loaded." />;
  }

  const interviewReadyCount = data.applications.filter((application) => application.status === "INTERVIEW").length;

  return (
    <div className="page-stack">
      <section className="dashboard-hero">
        <div>
          <p className="eyebrow">{data.profile.companyName}</p>
          <h2>Recruiter workspace</h2>
          <p>
            Manage roles for your company, review candidates, and schedule interviews from job
            application pipelines.
          </p>
        </div>
        <Link className="primary-button" to="/recruiter/jobs/new">
          <PlusCircle size={18} />
          Create job
        </Link>
      </section>

      {data.partialApplicationError ? (
        <p className="form-error">Some application counts could not be loaded right now.</p>
      ) : null}

      <section className="overview-grid">
        <Link className="overview-card linked-card" to="/recruiter/jobs">
          <BriefcaseBusiness size={22} />
          <strong>{data.ownedJobs.length}</strong>
          <h2>Your jobs</h2>
          <p>View active jobs connected to {data.profile.companyName}.</p>
        </Link>
        <Link className="overview-card linked-card" to="/recruiter/applications">
          <ClipboardCheck size={22} />
          <strong>{data.applications.length}</strong>
          <h2>Applications</h2>
          <p>Review candidates from job-specific application lists.</p>
        </Link>
        <Link className="overview-card linked-card" to="/recruiter/interviews">
          <CalendarPlus size={22} />
          <strong>{interviewReadyCount}</strong>
          <h2>Interview stage</h2>
          <p>Schedule interviews from applications that are ready for interviews.</p>
        </Link>
      </section>
    </div>
  );
}
