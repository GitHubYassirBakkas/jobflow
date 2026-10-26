import { useEffect, useState } from "react";
import { Link, useLocation, useSearchParams } from "react-router-dom";
import { EmptyState, ErrorState, LoadingState } from "../../components/AsyncState";
import { getMyApplications } from "../../features/candidate/candidateApi";
import { ApplicationCard } from "../../features/candidate/components/ApplicationCard";
import { getFriendlyErrorMessage } from "../../lib/errors";
import type { CandidateApplication, PageResponse } from "../../types/candidate";

const pageSize = 10;

export function parseApplicationsPage(value: string | null) {
  if (!value) {
    return 0;
  }

  const page = Number(value);

  if (!Number.isInteger(page) || page < 0) {
    return 0;
  }

  return page;
}

export function buildApplicationsSearchParams(page: number) {
  const searchParams = new URLSearchParams();

  if (page > 0) {
    searchParams.set("page", String(page));
  }

  return searchParams;
}

export function getRecoveredApplicationsPage(
  response: PageResponse<CandidateApplication>
) {
  if (
    response.content.length === 0
      && response.totalElements > 0
      && response.totalPages > 0
      && response.page >= response.totalPages
  ) {
    return response.totalPages - 1;
  }

  return null;
}

export function CandidateApplicationsPage() {
  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();
  const page = parseApplicationsPage(searchParams.get("page"));
  const [applicationPage, setApplicationPage] = useState<PageResponse<CandidateApplication> | null>(null);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [reloadKey, setReloadKey] = useState(0);
  const returnToApplications = `${location.pathname}${location.search}`;

  useEffect(() => {
    let isMounted = true;

    async function loadApplications() {
      let didRedirect = false;

      setIsLoading(true);
      setError("");

      try {
        const response = await getMyApplications(page, pageSize);
        const recoveredPage = getRecoveredApplicationsPage(response);

        if (recoveredPage !== null) {
          didRedirect = true;
          setSearchParams(
            buildApplicationsSearchParams(recoveredPage),
            { replace: true }
          );
          return;
        }

        if (isMounted) {
          setApplicationPage(response);
        }
      } catch (caughtError) {
        if (isMounted) {
          setError(getFriendlyErrorMessage(caughtError));
        }
      } finally {
        if (isMounted && !didRedirect) {
          setIsLoading(false);
        }
      }
    }

    void loadApplications();

    return () => {
      isMounted = false;
    };
  }, [page, reloadKey, setSearchParams]);

  function goToPage(nextPage: number) {
    setSearchParams(buildApplicationsSearchParams(nextPage));
  }

  function retryLoad() {
    setReloadKey((current) => current + 1);
  }

  if (isLoading) {
    return <LoadingState message="Loading your applications..." />;
  }

  if (error) {
    return (
      <ErrorState
        title="Could not load applications"
        message={error}
        action={
          <button className="secondary-button" onClick={retryLoad} type="button">
            Retry
          </button>
        }
      />
    );
  }

  if (!applicationPage) {
    return <EmptyState title="Applications unavailable" message="Your applications could not be loaded right now." />;
  }

  const applications = applicationPage.content;
  const totalPages = applicationPage.totalPages;
  const displayPage = totalPages > 0 ? applicationPage.page + 1 : 0;
  const resultSummary =
    applicationPage.totalElements === 1
      ? "1 application"
      : `${applicationPage.totalElements} applications`;

  if (applications.length === 0) {
    return (
      <EmptyState
        title="No applications yet"
        message="Apply to an open role and it will appear here."
        action={
          <Link className="secondary-button" to="/candidate/jobs">
            Browse jobs
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
          <h2>Your applications</h2>
        </div>
        <span>{resultSummary}</span>
      </section>
      <section className="list-stack">
        {applications.map((application) => (
          <ApplicationCard
            application={application}
            key={application.id}
            returnToApplications={returnToApplications}
          />
        ))}
      </section>

      {totalPages > 1 ? (
        <nav aria-label="Application pages" className="pagination-bar">
          <button
            className="secondary-button"
            disabled={applicationPage.first || applicationPage.page === 0}
            onClick={() => goToPage(applicationPage.page - 1)}
            type="button"
          >
            Previous
          </button>
          <span>
            Page {displayPage} of {totalPages}
          </span>
          <button
            className="secondary-button"
            disabled={applicationPage.last}
            onClick={() => goToPage(applicationPage.page + 1)}
            type="button"
          >
            Next
          </button>
        </nav>
      ) : null}
    </div>
  );
}
