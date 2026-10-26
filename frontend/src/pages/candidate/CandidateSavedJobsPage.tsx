import { useEffect, useState } from "react";
import { ArrowRight, BookmarkCheck, LoaderCircle, MapPin, Trash2 } from "lucide-react";
import { Link, useLocation, useSearchParams } from "react-router-dom";
import { EmptyState, ErrorState, LoadingState } from "../../components/AsyncState";
import { getSavedJobs, unsaveJob } from "../../features/candidate/candidateApi";
import { getFriendlyErrorMessage } from "../../lib/errors";
import { formatDate, formatEnumLabel } from "../../lib/formatters";
import type { PageResponse, SavedJobResponse } from "../../types/candidate";

const pageSize = 10;

export function parseSavedJobsPage(value: string | null) {
  if (!value) {
    return 0;
  }

  const page = Number(value);

  if (!Number.isInteger(page) || page < 0) {
    return 0;
  }

  return page;
}

export function buildSavedJobsSearchParams(page: number) {
  const searchParams = new URLSearchParams();

  if (page > 0) {
    searchParams.set("page", String(page));
  }

  return searchParams;
}

export function getPageAfterSavedJobRemoval(
  currentPage: number,
  currentPageItemCount: number
) {
  if (currentPage > 0 && currentPageItemCount === 1) {
    return currentPage - 1;
  }

  return currentPage;
}

export function CandidateSavedJobsPage() {
  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();
  const page = parseSavedJobsPage(searchParams.get("page"));
  const [savedJobPage, setSavedJobPage] = useState<PageResponse<SavedJobResponse> | null>(null);
  const [error, setError] = useState("");
  const [removeError, setRemoveError] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [removingJobId, setRemovingJobId] = useState<number | null>(null);
  const [reloadKey, setReloadKey] = useState(0);
  const returnToJobs = `${location.pathname}${location.search}`;

  useEffect(() => {
    let isMounted = true;

    async function loadSavedJobs() {
      setIsLoading(true);
      setError("");

      try {
        const response = await getSavedJobs(page, pageSize);

        if (
          response.content.length === 0
            && response.totalElements > 0
            && response.totalPages > 0
            && response.page >= response.totalPages
        ) {
          setSearchParams(
            buildSavedJobsSearchParams(response.totalPages - 1),
            { replace: true }
          );
          return;
        }

        if (isMounted) {
          setSavedJobPage(response);
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

    void loadSavedJobs();

    return () => {
      isMounted = false;
    };
  }, [page, reloadKey, setSearchParams]);

  function goToPage(nextPage: number) {
    setRemoveError("");
    setSearchParams(buildSavedJobsSearchParams(nextPage));
  }

  function retryLoad() {
    setRemoveError("");
    setReloadKey((current) => current + 1);
  }

  async function handleRemove(savedJob: SavedJobResponse) {
    if (removingJobId !== null) {
      return;
    }

    setRemovingJobId(savedJob.job.id);
    setRemoveError("");

    try {
      await unsaveJob(savedJob.job.id);

      const nextPage = getPageAfterSavedJobRemoval(
        savedJobPage?.page ?? page,
        savedJobPage?.content.length ?? 0
      );

      if (nextPage !== page) {
        setSearchParams(buildSavedJobsSearchParams(nextPage), { replace: true });
      } else {
        setReloadKey((current) => current + 1);
      }
    } catch (caughtError) {
      setRemoveError(getFriendlyErrorMessage(caughtError));
    } finally {
      setRemovingJobId(null);
    }
  }

  if (isLoading) {
    return <LoadingState message="Loading saved jobs..." />;
  }

  if (error) {
    return (
      <ErrorState
        title="Could not load saved jobs"
        message={error}
        action={
          <button className="secondary-button" onClick={retryLoad} type="button">
            Retry
          </button>
        }
      />
    );
  }

  if (!savedJobPage) {
    return <EmptyState title="Saved jobs unavailable" message="Your saved jobs could not be loaded right now." />;
  }

  const savedJobs = savedJobPage.content;
  const hasSavedJobs = savedJobs.length > 0;
  const totalPages = savedJobPage.totalPages;
  const displayPage = totalPages > 0 ? savedJobPage.page + 1 : 0;
  const resultSummary =
    savedJobPage.totalElements === 1
      ? "1 saved job"
      : `${savedJobPage.totalElements} saved jobs`;

  return (
    <div className="page-stack">
      <section className="section-heading">
        <div>
          <p className="eyebrow">Saved jobs</p>
          <h2>Your shortlist</h2>
          <p className="section-description">
            Keep track of roles you want to revisit before applying.
          </p>
        </div>
        <span>{resultSummary}</span>
      </section>

      {removeError ? <p className="form-error" role="alert">{removeError}</p> : null}

      {!hasSavedJobs ? (
        <EmptyState
          title="No saved jobs yet"
          message="Save roles from job discovery to build a focused shortlist."
          action={
            <Link className="secondary-button" to="/candidate/jobs">
              Browse jobs
            </Link>
          }
        />
      ) : (
        <>
          <section className="jobs-grid">
            {savedJobs.map((savedJob) => {
              const job = savedJob.job;
              const isRemoving = removingJobId === job.id;
              const isRemoveDisabled = removingJobId !== null;

              return (
                <article className="job-card saved-job-card" key={savedJob.id}>
                  <div className="card-icon">
                    <BookmarkCheck size={20} />
                  </div>
                  <div className="job-card-body">
                    <p className="eyebrow">{job.companyName}</p>
                    <h2>{job.title}</h2>
                    <div className="meta-row">
                      <span>
                        <MapPin size={16} />
                        {job.location}
                      </span>
                      <span>{formatEnumLabel(job.employmentType)}</span>
                      <span>Saved {formatDate(savedJob.savedAt)}</span>
                    </div>
                  </div>
                  <div className="saved-job-card-actions">
                    <Link
                      className="text-button"
                      state={{ returnToJobs }}
                      to={`/candidate/jobs/${job.id}`}
                    >
                      View role <ArrowRight size={16} />
                    </Link>
                    <button
                      className="secondary-button danger-button"
                      disabled={isRemoveDisabled}
                      onClick={() => void handleRemove(savedJob)}
                      type="button"
                    >
                      {isRemoving ? <LoaderCircle className="spin" size={16} /> : <Trash2 size={16} />}
                      {isRemoving ? "Removing" : "Remove"}
                    </button>
                  </div>
                </article>
              );
            })}
          </section>

          {totalPages > 1 ? (
            <nav aria-label="Saved job pages" className="pagination-bar">
              <button
                className="secondary-button"
                disabled={savedJobPage.first || savedJobPage.page === 0 || removingJobId !== null}
                onClick={() => goToPage(savedJobPage.page - 1)}
                type="button"
              >
                Previous
              </button>
              <span>
                Page {displayPage} of {totalPages}
              </span>
              <button
                className="secondary-button"
                disabled={savedJobPage.last || removingJobId !== null}
                onClick={() => goToPage(savedJobPage.page + 1)}
                type="button"
              >
                Next
              </button>
            </nav>
          ) : null}
        </>
      )}
    </div>
  );
}
