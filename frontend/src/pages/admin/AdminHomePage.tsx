import { useEffect, useState } from "react";
import { BriefcaseBusiness, Building2, ClipboardList, ShieldCheck, UserRound, UsersRound } from "lucide-react";
import { ErrorState, LoadingState } from "../../components/AsyncState";
import { getAdminDashboard } from "../../features/admin/adminApi";
import { getFriendlyErrorMessage } from "../../lib/errors";
import type { AdminDashboardStats } from "../../types/admin";

interface StatCard {
  label: string;
  value: number;
  helper: string;
  icon: React.ComponentType<{ size?: number }>;
}

export function buildStats(stats: AdminDashboardStats): StatCard[] {
  return [
    { label: "Total Users", value: stats.totalUsers, helper: "All registered accounts", icon: UsersRound },
    { label: "Candidates", value: stats.totalCandidates, helper: "Candidate accounts", icon: UserRound },
    { label: "Recruiters", value: stats.totalRecruiters, helper: "Recruiter accounts", icon: BriefcaseBusiness },
    { label: "Admins", value: stats.totalAdmins, helper: "Administrative users", icon: ShieldCheck },
    { label: "Companies", value: stats.totalCompanies, helper: "Companies in JobFlow", icon: Building2 },
    { label: "Jobs", value: stats.totalJobs, helper: "Jobs across the platform", icon: BriefcaseBusiness },
    { label: "Applications", value: stats.totalApplications, helper: "Submitted applications", icon: ClipboardList }
  ];
}

export function AdminHomePage() {
  const [stats, setStats] = useState<AdminDashboardStats | null>(null);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let isMounted = true;

    async function loadDashboard() {
      setIsLoading(true);
      setError("");

      try {
        const response = await getAdminDashboard();

        if (isMounted) {
          setStats(response);
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
    return <LoadingState message="Loading admin dashboard..." />;
  }

  if (error) {
    return <ErrorState title="Could not load admin dashboard" message={error} />;
  }

  if (!stats) {
    return <ErrorState title="Dashboard unavailable" message="Admin statistics could not be loaded." />;
  }

  return (
    <div className="page-stack">
      <section className="dashboard-hero">
        <div>
          <p className="eyebrow">Admin overview</p>
          <h2>Platform health at a glance</h2>
          <p>Monitor JobFlow account, company, job, and application totals from the admin API.</p>
        </div>
      </section>

      <section className="overview-grid admin-stats-grid">
        {buildStats(stats).map((item) => {
          const Icon = item.icon;

          return (
            <article className="overview-card" key={item.label}>
              <Icon size={22} />
              <strong>{item.value}</strong>
              <h2>{item.label}</h2>
              <p>{item.helper}</p>
            </article>
          );
        })}
      </section>
    </div>
  );
}
