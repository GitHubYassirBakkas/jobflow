import { useEffect, useRef, useState } from "react";
import { ArrowLeft, Download, ExternalLink, FileText, LoaderCircle, XCircle } from "lucide-react";
import { Link, useLocation, useParams } from "react-router-dom";
import { EmptyState, ErrorState, LoadingState } from "../../components/AsyncState";
import { ApplicationStatusBadge } from "../../components/StatusBadge";
import { CandidateApplicationTimeline } from "../../features/candidate/components/CandidateApplicationTimeline";
import { getMyApplication, getMyApplicationCv, withdrawMyApplication } from "../../features/candidate/candidateApi";
import { ApiError } from "../../lib/apiClient";
import { getFriendlyErrorMessage } from "../../lib/errors";
import { focusFirstElement, trapTabKey } from "../../lib/focusManagement";
import { formatDate, formatEnumLabel } from "../../lib/formatters";
import type { ApplicationStatus, CandidateApplication } from "../../types/candidate";

interface CandidateApplicationDetailsLocationState {
  returnToApplications?: unknown;
}

export function getSafeApplicationsReturnPath(value: unknown) {
  if (typeof value !== "string") {
    return "/candidate/applications";
  }

  try {
    const currentOrigin =
      typeof window === "undefined" ? "http://localhost" : window.location.origin;
    const url = new URL(value, currentOrigin);

    if (url.origin !== currentOrigin || url.pathname !== "/candidate/applications") {
      return "/candidate/applications";
    }

    return `${url.pathname}${url.search}`;
  } catch {
    return "/candidate/applications";
  }
}

export function getApplicationCvDownloadName(application: CandidateApplication) {
  return application.cvFileName?.trim() || `application-${application.id}-cv.pdf`;
}

export const withdrawableApplicationStatuses: ApplicationStatus[] = [
  "APPLIED",
  "REVIEWING",
  "INTERVIEW"
];

export function canWithdrawApplication(status: ApplicationStatus) {
  return withdrawableApplicationStatuses.includes(status);
}

export function canSubmitWithdrawal(
  isDialogOpen: boolean,
  isWithdrawing: boolean
) {
  return isDialogOpen && !isWithdrawing;
}

export function getNextTimelineReloadKey(currentKey: number) {
  return currentKey + 1;
}

export function getWithdrawalErrorMessage(error: unknown) {
  if (error instanceof ApiError) {
    if (error.status === 403) {
      return "You do not have access to withdraw this application.";
    }

    if (error.status === 404) {
      return "This application is no longer available.";
    }

    if (error.status === 409) {
      return "This application can no longer be withdrawn because its status changed.";
    }
  }

  return getFriendlyErrorMessage(error);
}

export function CandidateApplicationDetailsPage() {
  const { applicationId } = useParams();
  const location = useLocation();
  const locationState = location.state as CandidateApplicationDetailsLocationState | null;
  const applicationsReturnPath = getSafeApplicationsReturnPath(
    locationState?.returnToApplications
  );
  const [application, setApplication] = useState<CandidateApplication | null>(null);
  const [error, setError] = useState("");
  const [isNotFound, setIsNotFound] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [downloadError, setDownloadError] = useState("");
  const [isDownloading, setIsDownloading] = useState(false);
  const [isWithdrawDialogOpen, setIsWithdrawDialogOpen] = useState(false);
  const [isWithdrawing, setIsWithdrawing] = useState(false);
  const [withdrawalError, setWithdrawalError] = useState("");
  const [withdrawalSuccess, setWithdrawalSuccess] = useState("");
  const [timelineReloadKey, setTimelineReloadKey] = useState(0);
  const [applicationReloadKey, setApplicationReloadKey] = useState(0);
  const isWithdrawingRef = useRef(false);
  const withdrawButtonRef = useRef<HTMLButtonElement>(null);
  const cancelWithdrawalButtonRef = useRef<HTMLButtonElement>(null);
  const withdrawDialogRef = useRef<HTMLElement>(null);

  useEffect(() => {
    let isMounted = true;

    async function loadApplication() {
      if (!applicationId) {
        setIsNotFound(true);
        setIsLoading(false);
        return;
      }

      setIsLoading(true);
      setError("");
      setIsNotFound(false);
      setDownloadError("");
      setWithdrawalError("");
      setWithdrawalSuccess("");
      setApplication(null);

      try {
        const response = await getMyApplication(applicationId);

        if (isMounted) {
          setApplication(response);
        }
      } catch (caughtError) {
        if (!isMounted) {
          return;
        }

        if (caughtError instanceof ApiError && caughtError.status === 404) {
          setIsNotFound(true);
        } else {
          setError(getFriendlyErrorMessage(caughtError));
        }
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    void loadApplication();

    return () => {
      isMounted = false;
    };
  }, [applicationId, applicationReloadKey]);

  function retryLoad() {
    setApplicationReloadKey((current) => current + 1);
  }

  useEffect(() => {
    if (!isWithdrawDialogOpen) {
      return;
    }

    focusFirstElement(
      withdrawDialogRef.current,
      cancelWithdrawalButtonRef.current
    );

    function handleKeyDown(event: KeyboardEvent) {
      if (trapTabKey(event, withdrawDialogRef.current)) {
        return;
      }

      if (event.key === "Escape" && !isWithdrawing) {
        event.preventDefault();
        closeWithdrawDialog();
      }
    }

    document.addEventListener("keydown", handleKeyDown);

    return () => {
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [isWithdrawDialogOpen, isWithdrawing]);

  async function refreshApplicationAfterWithdrawalAttempt() {
    if (!applicationId) {
      return;
    }

    try {
      const response = await getMyApplication(applicationId);

      setApplication(response);
    } catch (caughtError) {
      if (caughtError instanceof ApiError && caughtError.status === 404) {
        setIsNotFound(true);
        setApplication(null);
      } else {
        setWithdrawalError(getFriendlyErrorMessage(caughtError));
      }
    } finally {
      setTimelineReloadKey(getNextTimelineReloadKey);
    }
  }

  async function handleDownloadCv() {
    if (!application || isDownloading) {
      return;
    }

    setIsDownloading(true);
    setDownloadError("");

    try {
      const blob = await getMyApplicationCv(application.id);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");

      link.href = url;
      link.download = getApplicationCvDownloadName(application);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (caughtError) {
      setDownloadError(getFriendlyErrorMessage(caughtError));
    } finally {
      setIsDownloading(false);
    }
  }

  function openWithdrawDialog() {
    if (!application || !canWithdrawApplication(application.status)) {
      return;
    }

    setWithdrawalError("");
    setWithdrawalSuccess("");
    setIsWithdrawDialogOpen(true);
  }

  function closeWithdrawDialog() {
    if (isWithdrawing) {
      return;
    }

    setIsWithdrawDialogOpen(false);
    withdrawButtonRef.current?.focus();
  }

  async function handleConfirmWithdrawal() {
    if (
      !application
      || !canSubmitWithdrawal(isWithdrawDialogOpen, isWithdrawing)
      || isWithdrawingRef.current
    ) {
      return;
    }

    isWithdrawingRef.current = true;
    setIsWithdrawing(true);
    setWithdrawalError("");
    setWithdrawalSuccess("");

    try {
      const response = await withdrawMyApplication(application.id);

      setApplication(response);
      setIsWithdrawDialogOpen(false);
      setWithdrawalSuccess("Application withdrawn successfully.");
      setTimelineReloadKey(getNextTimelineReloadKey);
    } catch (caughtError) {
      setWithdrawalError(getWithdrawalErrorMessage(caughtError));

      if (
        caughtError instanceof ApiError
        && (caughtError.status === 409 || caughtError.status === 404)
      ) {
        await refreshApplicationAfterWithdrawalAttempt();
      }
    } finally {
      isWithdrawingRef.current = false;
      setIsWithdrawing(false);
    }
  }

  if (isLoading) {
    return <LoadingState message="Loading application details..." />;
  }

  if (isNotFound) {
    return (
      <EmptyState
        title="Application not found"
        message="This application is not available."
        action={
          <Link className="secondary-button" to={applicationsReturnPath}>
            Back to applications
          </Link>
        }
      />
    );
  }

  if (error) {
    return (
      <ErrorState
        title="Could not load application"
        message={error}
        action={
          applicationId ? (
            <button className="secondary-button" onClick={retryLoad} type="button">
              Retry
            </button>
          ) : undefined
        }
      />
    );
  }

  if (!application) {
    return (
      <EmptyState
        title="Application unavailable"
        message="Your application could not be loaded right now."
      />
    );
  }

  const cvFileName = getApplicationCvDownloadName(application);
  const showWithdrawAction = canWithdrawApplication(application.status);

  return (
    <article className="detail-panel">
      <Link className="back-link" to={applicationsReturnPath}>
        <ArrowLeft size={16} />
        Back to applications
      </Link>

      <div className="detail-header">
        <div>
          <p className="eyebrow">{application.companyName}</p>
          <h2>{application.jobTitle}</h2>
          <div className="meta-row">
            <span>Applied {formatDate(application.appliedAt)}</span>
            <ApplicationStatusBadge status={application.status} />
          </div>
        </div>
        <div className="detail-header-actions">
          {showWithdrawAction ? (
            <button
              className="secondary-button danger-button"
              onClick={openWithdrawDialog}
              ref={withdrawButtonRef}
              type="button"
            >
              <XCircle size={16} />
              Withdraw Application
            </button>
          ) : null}
          <Link
            className="secondary-button"
            state={{ returnToJobs: applicationsReturnPath }}
            to={`/candidate/jobs/${application.jobId}`}
          >
            View job <ExternalLink size={16} />
          </Link>
        </div>
      </div>

      {withdrawalSuccess ? (
        <p className="form-success" role="status">{withdrawalSuccess}</p>
      ) : null}

      {withdrawalError ? (
        <p className="form-error" role="alert">{withdrawalError}</p>
      ) : null}

      <section className="detail-section">
        <h3>Application</h3>
        <p>
          Your application for {application.jobTitle} at {application.companyName} is currently {formatEnumLabel(application.status)}.
        </p>
      </section>

      <CandidateApplicationTimeline
        applicationId={application.id}
        reloadKey={timelineReloadKey}
      />

      <section className="detail-section">
        <h3>Submitted CV</h3>
        <p>
          <FileText size={16} />
          {cvFileName}
        </p>
        <button
          className="secondary-button"
          disabled={isDownloading}
          onClick={() => void handleDownloadCv()}
          type="button"
        >
          {isDownloading ? <LoaderCircle className="spin" size={16} /> : <Download size={16} />}
          {isDownloading ? "Downloading" : "Download CV"}
        </button>
        {downloadError ? <p className="form-error" role="alert">{downloadError}</p> : null}
      </section>

      {isWithdrawDialogOpen ? (
        <div className="modal-backdrop">
          <section
            aria-describedby="withdraw-dialog-description"
            aria-labelledby="withdraw-dialog-title"
            aria-modal="true"
            className="application-dialog"
            ref={withdrawDialogRef}
            role="dialog"
            tabIndex={-1}
          >
            <div>
              <p className="eyebrow">Application withdrawal</p>
              <h2 id="withdraw-dialog-title">Withdraw application?</h2>
            </div>

            <div className="application-dialog-section" id="withdraw-dialog-description">
              <p>
                Withdrawal stops your participation in this job process. Your application and status history remain available, and reapplication to the same job is not currently allowed.
              </p>
              <p>
                Scheduled interviews are not automatically cancelled by this action.
              </p>
            </div>

            <div className="form-actions">
              <button
                className="secondary-button"
                disabled={isWithdrawing}
                onClick={closeWithdrawDialog}
                ref={cancelWithdrawalButtonRef}
                type="button"
              >
                Cancel
              </button>
              <button
                className="secondary-button danger-button"
                disabled={isWithdrawing}
                onClick={() => void handleConfirmWithdrawal()}
                type="button"
              >
                {isWithdrawing ? <LoaderCircle className="spin" size={18} /> : <XCircle size={18} />}
                {isWithdrawing ? "Withdrawing..." : "Confirm Withdrawal"}
              </button>
            </div>
          </section>
        </div>
      ) : null}
    </article>
  );
}
