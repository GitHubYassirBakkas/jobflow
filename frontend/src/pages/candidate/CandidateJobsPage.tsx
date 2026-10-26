import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { EmptyState, ErrorState, LoadingState } from "../../components/AsyncState";
import { getJobs, getSavedJobIds, saveJob, unsaveJob } from "../../features/candidate/candidateApi";
import { JobCard } from "../../features/candidate/components/JobCard";
import { createSavedJobIdSet, isJobSaved, updateSavedJobIdSet } from "../../features/candidate/savedJobState";
import { getFriendlyErrorMessage } from "../../lib/errors";
import { formatEnumLabel } from "../../lib/formatters";
import type { EmploymentType, Job, PageResponse } from "../../types/candidate";

const pageSize = 10;
const employmentTypes: EmploymentType[] = ["FULL_TIME", "PART_TIME", "INTERNSHIP", "CONTRACT", "FREELANCE"];

interface DiscoveryState {
  search: string;
  location: string;
  employmentType: EmploymentType | "";
  page: number;
}

function isEmploymentType(value: string | null): value is EmploymentType {
  return Boolean(value && employmentTypes.includes(value as EmploymentType));
}

function parsePage(value: string | null) {
  if (!value) {
    return 0;
  }

  const page = Number(value);

  if (!Number.isInteger(page) || page < 0) {
    return 0;
  }

  return page;
}

function getDiscoveryState(searchParams: URLSearchParams): DiscoveryState {
  const employmentType = searchParams.get("employmentType");

  return {
    search: searchParams.get("search")?.trim() ?? "",
    location: searchParams.get("location")?.trim() ?? "",
    employmentType: isEmploymentType(employmentType) ? employmentType : "",
    page: parsePage(searchParams.get("page"))
  };
}

function buildSearchParams(state: DiscoveryState) {
  const nextParams = new URLSearchParams();
  const search = state.search.trim();
  const location = state.location.trim();

  if (search) {
    nextParams.set("search", search);
  }

  if (location) {
    nextParams.set("location", location);
  }

  if (state.employmentType) {
    nextParams.set("employmentType", state.employmentType);
  }

  if (state.page > 0) {
    nextParams.set("page", String(state.page));
  }

  return nextParams;
}

export function CandidateJobsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const committedState = getDiscoveryState(searchParams);
  const [draftSearch, setDraftSearch] = useState(committedState.search);
  const [draftLocation, setDraftLocation] = useState(committedState.location);
  const [draftEmploymentType, setDraftEmploymentType] = useState<EmploymentType | "">(committedState.employmentType);
  const [jobPage, setJobPage] = useState<PageResponse<Job> | null>(null);
  const [savedJobIds, setSavedJobIds] = useState<Set<number>>(() => new Set());
  const [error, setError] = useState("");
  const [saveError, setSaveError] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [pendingSavedJobId, setPendingSavedJobId] = useState<number | null>(null);
  const [reloadKey, setReloadKey] = useState(0);
  const hasActiveFilters = Boolean(
    committedState.search
      || committedState.location
      || committedState.employmentType
  );

  useEffect(() => {
    let isMounted = true;

    async function loadJobs() {
      setIsLoading(true);
      setError("");
      setSaveError("");

      try {
        const [response, savedIds] = await Promise.all([
          getJobs({
            search: committedState.search,
            location: committedState.location,
            employmentType: committedState.employmentType || undefined,
            page: committedState.page,
            size: pageSize
          }),
          getSavedJobIds()
        ]);

        if (
          response.content.length === 0
            && response.totalElements > 0
            && response.totalPages > 0
            && response.page >= response.totalPages
        ) {
          setSearchParams(
            buildSearchParams({
              ...committedState,
              page: response.totalPages - 1
            }),
            { replace: true }
          );
          return;
        }

        if (isMounted) {
          setJobPage(response);
          setSavedJobIds(createSavedJobIdSet(savedIds));
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
  }, [committedState.search, committedState.location, committedState.employmentType, committedState.page, reloadKey, setSearchParams]);

  useEffect(() => {
    setDraftSearch(committedState.search);
    setDraftLocation(committedState.location);
    setDraftEmploymentType(committedState.employmentType);
  }, [committedState.search, committedState.location, committedState.employmentType]);

  function commitDiscoveryState(nextState: DiscoveryState) {
    setSearchParams(buildSearchParams(nextState));
  }

  function handleSearchSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    commitDiscoveryState({
      search: draftSearch.trim(),
      location: draftLocation.trim(),
      employmentType: draftEmploymentType,
      page: 0
    });
  }

  function handleClearFilters() {
    setDraftSearch("");
    setDraftLocation("");
    setDraftEmploymentType("");
    setSearchParams(new URLSearchParams());
  }

  function goToPage(page: number) {
    commitDiscoveryState({
      ...committedState,
      page
    });
  }

  function retryLoad() {
    setSaveError("");
    setReloadKey((current) => current + 1);
  }

  async function handleSavedToggle(job: Job) {
    if (pendingSavedJobId !== null) {
      return;
    }

    const currentlySaved = isJobSaved(savedJobIds, job.id);

    setPendingSavedJobId(job.id);
    setSaveError("");

    try {
      if (currentlySaved) {
        await unsaveJob(job.id);
      } else {
        await saveJob(job.id);
      }

      setSavedJobIds((current) =>
        updateSavedJobIdSet(current, job.id, !currentlySaved)
      );
    } catch (caughtError) {
      setSaveError(getFriendlyErrorMessage(caughtError));
    } finally {
      setPendingSavedJobId(null);
    }
  }

  if (isLoading) {
    return <LoadingState message="Loading open jobs..." />;
  }

  if (error) {
    return (
      <ErrorState
        title="Could not load jobs"
        message={error}
        action={
          <button className="secondary-button" onClick={retryLoad} type="button">
            Retry
          </button>
        }
      />
    );
  }

  if (!jobPage) {
    return <EmptyState title="Jobs unavailable" message="Open jobs could not be loaded right now." />;
  }

  const jobs = jobPage.content;
  const hasJobs = jobs.length > 0;
  const totalPages = jobPage.totalPages;
  const displayPage = totalPages > 0 ? jobPage.page + 1 : 0;
  const resultSummary =
    jobPage.totalElements === 1
      ? "1 job found"
      : `${jobPage.totalElements} jobs found`;

  return (
    <div className="page-stack">
      <section className="section-heading">
        <div>
          <p className="eyebrow">Jobs</p>
          <h2>Open roles</h2>
        </div>
        <span>{resultSummary}</span>
      </section>

      <form className="discovery-filter-panel" onSubmit={handleSearchSubmit}>
        <label>
          <span>Search jobs</span>
          <input
            onChange={(event) => setDraftSearch(event.target.value)}
            placeholder="Title, company, or keyword"
            type="search"
            value={draftSearch}
          />
        </label>

        <label>
          <span>Location</span>
          <input
            onChange={(event) => setDraftLocation(event.target.value)}
            placeholder="e.g. Meknes"
            type="text"
            value={draftLocation}
          />
        </label>

        <label>
          <span>Employment type</span>
          <select
            onChange={(event) => setDraftEmploymentType(event.target.value as EmploymentType | "")}
            value={draftEmploymentType}
          >
            <option value="">All employment types</option>
            {employmentTypes.map((type) => (
              <option key={type} value={type}>
                {formatEnumLabel(type)}
              </option>
            ))}
          </select>
        </label>

        <div className="discovery-filter-actions">
          <button className="primary-button" type="submit">
            Search
          </button>
          <button className="secondary-button" disabled={!hasActiveFilters && committedState.page === 0} onClick={handleClearFilters} type="button">
            Clear filters
          </button>
        </div>
      </form>

      {saveError ? <p className="form-error" role="alert">{saveError}</p> : null}

      {!hasJobs ? (
        <EmptyState
          title={hasActiveFilters ? "No jobs match your search" : "No active jobs yet"}
          message={
            hasActiveFilters
              ? "Try a different keyword, location, or employment type."
              : "There are no open roles available right now. Check back soon."
          }
          action={
            hasActiveFilters ? (
              <button className="secondary-button" onClick={handleClearFilters} type="button">
                Clear filters
              </button>
            ) : (
              <Link className="secondary-button" to="/candidate">
                Back to overview
              </Link>
            )
          }
        />
      ) : (
        <>
          <section className="jobs-grid">
            {jobs.map((job) => (
              <JobCard
                isSaved={isJobSaved(savedJobIds, job.id)}
                isSavedActionPending={pendingSavedJobId === job.id}
                job={job}
                key={job.id}
                onSavedToggle={(selectedJob) => void handleSavedToggle(selectedJob)}
              />
            ))}
          </section>

          {totalPages > 1 ? (
            <nav aria-label="Job result pages" className="pagination-bar">
              <button
                className="secondary-button"
                disabled={jobPage.first || jobPage.page === 0}
                onClick={() => goToPage(jobPage.page - 1)}
                type="button"
              >
                Previous
              </button>
              <span>
                Page {displayPage} of {totalPages}
              </span>
              <button
                className="secondary-button"
                disabled={jobPage.last}
                onClick={() => goToPage(jobPage.page + 1)}
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
