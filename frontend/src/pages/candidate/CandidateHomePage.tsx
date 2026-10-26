import { useCallback, useEffect, useState } from "react";
import type { ComponentType } from "react";
import {
  BookmarkCheck,
  BriefcaseBusiness,
  CalendarDays,
  ClipboardList,
  FileText,
  RotateCw,
  Search,
  Send,
  TimerOff
} from "lucide-react";
import { Link } from "react-router-dom";
import { EmptyState, ErrorState, LoadingState } from "../../components/AsyncState";
import { getCandidateDashboard } from "../../features/candidate/candidateApi";
import { ApplicationCard } from "../../features/candidate/components/ApplicationCard";
import { InterviewCard } from "../../features/candidate/components/InterviewCard";
import { getFriendlyErrorMessage } from "../../lib/errors";
import type { CandidateApplication, CandidateDashboard, CandidateInterview } from "../../types/candidate";

type DashboardIconKey =
  | "applications"
  | "active"
  | "withdrawn"
  | "interviews"
  | "saved";

interface DashboardSummaryCard {
  label: string;
  value: number;
  description: string;
  to: string;
  icon: DashboardIconKey;
}

const summaryIcons: Record<DashboardIconKey, ComponentType<{ size?: number }>> = {
  applications: ClipboardList,
  active: Send,
  withdrawn: TimerOff,
  interviews: CalendarDays,
  saved: BookmarkCheck
};

export function buildCandidateDashboardSummary(
  dashboard: CandidateDashboard
): DashboardSummaryCard[] {
  return [
    {
      label: "Total Applications",
      value: dashboard.totalApplications,
      description: "All applications you have submitted.",
      to: "/candidate/applications",
      icon: "applications"
    },
    {
      label: "Active Applications",
      value: dashboard.activeApplications,
      description: "Applications still moving through the process.",
      to: "/candidate/applications",
      icon: "active"
    },
    {
      label: "Withdrawn Applications",
      value: dashboard.withdrawnApplications,
      description: "Applications you have withdrawn.",
      to: "/candidate/applications",
      icon: "withdrawn"
    },
    {
      label: "Upcoming Interviews",
      value: dashboard.upcomingInterviews,
      description: "Scheduled interviews coming up.",
      to: "/candidate/interviews",
      icon: "interviews"
    },
    {
      label: "Saved Jobs",
      value: dashboard.savedJobs,
      description: "Active jobs saved for later.",
      to: "/candidate/saved-jobs",
      icon: "saved"
    }
  ];
}

export function getCandidateDashboardPreviews(dashboard: CandidateDashboard) {
  return {
    recentApplications: dashboard.recentApplications,
    nextInterviews: dashboard.nextInterviews,
    hasRecentApplications: dashboard.recentApplications.length > 0,
    hasUpcomingInterviews: dashboard.nextInterviews.length > 0,
    hasSavedJobs: dashboard.savedJobs > 0
  };
}

function SummaryCard({ item }: { item: DashboardSummaryCard }) {
  const Icon = summaryIcons[item.icon];

  return (
    <Link className="overview-card linked-card dashboard-summary-card" to={item.to}>
      <Icon size={22} />
      <strong>{item.value}</strong>
      <h2>{item.label}</h2>
      <p>{item.description}</p>
    </Link>
  );
}

export function CandidateHomePage() {
  const [data, setData] = useState<CandidateDashboard | null>(null);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);

  const loadDashboard = useCallback(async () => {
    setIsLoading(true);
    setError("");

    try {
      const dashboard = await getCandidateDashboard();
      setData(dashboard);
    } catch (caughtError) {
      setError(getFriendlyErrorMessage(caughtError));
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    let isMounted = true;

    async function loadMountedDashboard() {
      setIsLoading(true);
      setError("");

      try {
        const dashboard = await getCandidateDashboard();

        if (isMounted) {
          setData(dashboard);
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

    void loadMountedDashboard();

    return () => {
      isMounted = false;
    };
  }, []);

  if (isLoading) {
    return <LoadingState message="Loading your candidate dashboard..." />;
  }

  if (error) {
    return (
      <ErrorState
        action={
          <button className="secondary-button" onClick={() => void loadDashboard()} type="button">
            Retry <RotateCw size={16} />
          </button>
        }
        title="Could not load dashboard"
        message={error}
      />
    );
  }

  if (!data) {
    return (
      <EmptyState
        title="Dashboard unavailable"
        message="Your candidate dashboard could not be loaded right now."
      />
    );
  }

  const summaryCards = buildCandidateDashboardSummary(data);
  const previews = getCandidateDashboardPreviews(data);

  return (
    <div className="page-stack">
      <section className="dashboard-hero">
        <div>
          <h2>Candidate Overview</h2>
          <p>Track your applications and upcoming interviews.</p>
        </div>
        <div className="heading-actions">
          <Link className="primary-button" to="/candidate/jobs">
            Browse Jobs <Search size={16} />
          </Link>
          <Link className="secondary-button" to="/candidate/applications">
            My Applications <FileText size={16} />
          </Link>
          <Link className="secondary-button" to="/candidate/saved-jobs">
            Saved Jobs <BookmarkCheck size={16} />
          </Link>
        </div>
      </section>

      <section aria-label="Dashboard summary" className="overview-grid dashboard-summary-grid">
        {summaryCards.map((item) => (
          <SummaryCard item={item} key={item.label} />
        ))}
      </section>

      <section aria-labelledby="recent-applications-heading" className="dashboard-section">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Applications</p>
            <h2 id="recent-applications-heading">Recent Applications</h2>
          </div>
          <Link className="text-button" to="/candidate/applications">
            View all <FileText size={16} />
          </Link>
        </div>

        {previews.hasRecentApplications ? (
          <div className="page-stack">
            {previews.recentApplications.map((application: CandidateApplication) => (
              <ApplicationCard
                application={application}
                key={application.id}
                returnToApplications="/candidate/applications"
              />
            ))}
          </div>
        ) : (
          <EmptyState
            action={
              <Link className="secondary-button" to="/candidate/jobs">
                Browse Jobs <BriefcaseBusiness size={16} />
              </Link>
            }
            title="No applications yet"
            message="Applications you submit will appear here with their latest status."
          />
        )}
      </section>

      <section aria-labelledby="upcoming-interviews-heading" className="dashboard-section">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Interviews</p>
            <h2 id="upcoming-interviews-heading">Upcoming Interviews</h2>
          </div>
          <Link className="text-button" to="/candidate/interviews">
            View all <CalendarDays size={16} />
          </Link>
        </div>

        {previews.hasUpcomingInterviews ? (
          <div className="page-stack">
            {previews.nextInterviews.map((interview: CandidateInterview) => (
              <InterviewCard interview={interview} key={interview.id} />
            ))}
          </div>
        ) : (
          <EmptyState
            action={
              <Link className="secondary-button" to="/candidate/applications">
                My Applications <FileText size={16} />
              </Link>
            }
            title="No upcoming interviews"
            message="Scheduled interviews will appear here when recruiters add them."
          />
        )}
      </section>
    </div>
  );
}
