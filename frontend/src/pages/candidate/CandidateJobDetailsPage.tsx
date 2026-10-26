import { useEffect, useMemo, useRef, useState } from "react";
import type { ChangeEvent } from "react";
import { ArrowLeft, CheckCircle2, ExternalLink, FileText, LoaderCircle, MapPin, Upload } from "lucide-react";
import { Link, useLocation, useParams } from "react-router-dom";
import { EmptyState, ErrorState, LoadingState } from "../../components/AsyncState";
import { applyToJob, getJob, getMyApplicationForJob, getMyCandidateProfile, getSavedJobIds, saveJob, unsaveJob } from "../../features/candidate/candidateApi";
import { BookmarkJobButton } from "../../features/candidate/components/BookmarkJobButton";
import { ApiError } from "../../lib/apiClient";
import { getFriendlyErrorMessage } from "../../lib/errors";
import { getFeedbackRole } from "../../lib/feedback";
import { focusFirstElement, trapTabKey } from "../../lib/focusManagement";
import { formatDate, formatEnumLabel } from "../../lib/formatters";
import type { CandidateApplication, CandidateProfile, JobDetails } from "../../types/candidate";

type Feedback = { type: "success" | "error"; message: string } | null;
type CvChoice = "saved" | "custom";
interface CandidateJobDetailsLocationState {
  returnToJobs?: unknown;
}

function isPdfFile(file: File) {
  return file.type === "application/pdf" || file.name.toLowerCase().endsWith(".pdf");
}

function hasText(value: string | null | undefined) {
  return Boolean(value?.trim());
}

function getSafeWebsiteUrl(value: string | null | undefined) {
  const trimmedValue = value?.trim();

  if (!trimmedValue) {
    return null;
  }

  const candidateUrl = /^https?:\/\//i.test(trimmedValue)
    ? trimmedValue
    : `https://${trimmedValue}`;

  try {
    const url = new URL(candidateUrl);

    if (url.protocol !== "http:" && url.protocol !== "https:") {
      return null;
    }

    return url.toString();
  } catch {
    return null;
  }
}

export function getSafeJobsReturnPath(value: unknown) {
  if (typeof value !== "string") {
    return "/candidate/jobs";
  }

  try {
    const currentOrigin =
      typeof window === "undefined" ? "http://localhost" : window.location.origin;
    const url = new URL(value, currentOrigin);

    const isAllowedCandidateJobReturnPath =
      url.pathname === "/candidate/jobs"
        || url.pathname === "/candidate/saved-jobs"
        || url.pathname === "/candidate/applications";

    if (url.origin !== currentOrigin || !isAllowedCandidateJobReturnPath) {
      return "/candidate/jobs";
    }

    return `${url.pathname}${url.search}`;
  } catch {
    return "/candidate/jobs";
  }
}

export function isApplicationAlreadySubmitted(
  appliedApplication: CandidateApplication | null,
  hasAppliedAfterSubmit: boolean
) {
  return Boolean(appliedApplication) || hasAppliedAfterSubmit;
}

export function CandidateJobDetailsPage() {
  const { jobId } = useParams();
  const location = useLocation();
  const locationState = location.state as CandidateJobDetailsLocationState | null;
  const jobsReturnPath = getSafeJobsReturnPath(locationState?.returnToJobs);
  const [job, setJob] = useState<JobDetails | null>(null);
  const [profile, setProfile] = useState<CandidateProfile | null>(null);
  const [isProfileMissing, setIsProfileMissing] = useState(false);
  const [appliedApplication, setAppliedApplication] = useState<CandidateApplication | null>(null);
  const [error, setError] = useState("");
  const [applicationLoadError, setApplicationLoadError] = useState("");
  const [profileLoadError, setProfileLoadError] = useState("");
  const [bookmarkError, setBookmarkError] = useState("");
  const [feedback, setFeedback] = useState<Feedback>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isApplying, setIsApplying] = useState(false);
  const [isBookmarkPending, setIsBookmarkPending] = useState(false);
  const [isJobSaved, setIsJobSaved] = useState(false);
  const [hasAppliedAfterSubmit, setHasAppliedAfterSubmit] = useState(false);
  const [isApplyDialogOpen, setIsApplyDialogOpen] = useState(false);
  const [cvChoice, setCvChoice] = useState<CvChoice>("saved");
  const [selectedCvFile, setSelectedCvFile] = useState<File | null>(null);
  const [applyError, setApplyError] = useState("");
  const [reloadKey, setReloadKey] = useState(0);
  const applyButtonRef = useRef<HTMLButtonElement | null>(null);
  const applyDialogRef = useRef<HTMLElement | null>(null);

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
      setApplicationLoadError("");
      setProfileLoadError("");
      setBookmarkError("");
      setProfile(null);
      setIsProfileMissing(false);
      setAppliedApplication(null);
      setIsJobSaved(false);

      try {
        const jobResponse = await getJob(jobId);

        if (isMounted) {
          setJob(jobResponse);
        }

        const [applicationResult, profileResult, savedJobIdsResult] = await Promise.allSettled([
          getMyApplicationForJob(jobResponse.id),
          getMyCandidateProfile(),
          getSavedJobIds()
        ]);

        if (!isMounted) {
          return;
        }

        if (applicationResult.status === "fulfilled") {
          setAppliedApplication(applicationResult.value);
        } else {
          setApplicationLoadError("We could not confirm whether you have already applied. Refresh before applying.");
        }

        if (profileResult.status === "fulfilled") {
          setProfile(profileResult.value);
          setIsProfileMissing(false);
        } else if (profileResult.reason instanceof ApiError && profileResult.reason.status === 404) {
          setProfile(null);
          setIsProfileMissing(true);
        } else {
          setProfileLoadError("We could not load your candidate profile. Refresh before applying.");
        }

        if (savedJobIdsResult.status === "fulfilled") {
          setIsJobSaved(savedJobIdsResult.value.includes(jobResponse.id));
        } else {
          setBookmarkError("We could not confirm whether this job is saved. Refresh before saving.");
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
  }, [jobId, reloadKey]);

  const alreadyApplied = useMemo(() => {
    return isApplicationAlreadySubmitted(
      appliedApplication,
      hasAppliedAfterSubmit
    );
  }, [appliedApplication, hasAppliedAfterSubmit]);

  const savedCvFileName = profile?.cvFileName?.trim() ?? "";
  const hasSavedCv = savedCvFileName.length > 0;
  const canOpenApplyDialog = Boolean(
    job
      && job.active
      && !alreadyApplied
      && !isApplying
      && profile
      && !applicationLoadError
      && !profileLoadError
  );

  const companyWebsiteUrl = getSafeWebsiteUrl(job?.companyWebsite);
  const showCompanyDetails =
    hasText(job?.companyDescription)
    || hasText(job?.companyLocation)
    || Boolean(companyWebsiteUrl);

  function handleOpenApplyDialog() {
    if (!canOpenApplyDialog) {
      return;
    }

    setCvChoice(hasSavedCv ? "saved" : "custom");
    setSelectedCvFile(null);
    setApplyError("");
    setFeedback(null);
    setIsApplyDialogOpen(true);
  }

  function restoreApplyButtonFocus() {
    window.requestAnimationFrame(() => {
      applyButtonRef.current?.focus();
    });
  }

  function retryLoad() {
    setReloadKey((current) => current + 1);
  }

  async function handleSavedToggle() {
    if (!job || isBookmarkPending) {
      return;
    }

    const nextSavedState = !isJobSaved;

    setIsBookmarkPending(true);
    setBookmarkError("");

    try {
      if (nextSavedState) {
        await saveJob(job.id);
      } else {
        await unsaveJob(job.id);
      }

      setIsJobSaved(nextSavedState);
    } catch (caughtError) {
      setBookmarkError(getFriendlyErrorMessage(caughtError));
    } finally {
      setIsBookmarkPending(false);
    }
  }

  function handleCloseApplyDialog() {
    if (isApplying) {
      return;
    }

    setIsApplyDialogOpen(false);
    setApplyError("");
    setSelectedCvFile(null);
    restoreApplyButtonFocus();
  }

  useEffect(() => {
    if (!isApplyDialogOpen) {
      return;
    }

    focusFirstElement(applyDialogRef.current);

    function handleKeyDown(event: KeyboardEvent) {
      if (trapTabKey(event, applyDialogRef.current)) {
        return;
      }

      if (event.key === "Escape" && !isApplying) {
        event.preventDefault();
        handleCloseApplyDialog();
      }
    }

    document.addEventListener("keydown", handleKeyDown);

    return () => {
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [isApplyDialogOpen, isApplying]);

  function handleCvFileChange(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0] ?? null;
    event.target.value = "";

    setApplyError("");

    if (!file) {
      setSelectedCvFile(null);
      setApplyError("No file selected.");
      return;
    }

    if (!isPdfFile(file)) {
      setSelectedCvFile(null);
      setApplyError("Please choose a PDF file.");
      return;
    }

    setSelectedCvFile(file);
  }

  async function handleSubmitApplication() {
    if (!jobId || alreadyApplied || isApplying) {
      return;
    }

    let cvFile: File | null = null;

    if (cvChoice === "saved") {
      if (!hasSavedCv) {
        setApplyError("No saved CV is available. Upload a different CV to apply.");
        return;
      }
    } else {
      if (!selectedCvFile) {
        setApplyError("Choose a PDF CV before submitting.");
        return;
      }

      if (!isPdfFile(selectedCvFile)) {
        setApplyError("Please choose a PDF file.");
        return;
      }

      cvFile = selectedCvFile;
    }

    setIsApplying(true);
    setApplyError("");

    try {
      const application = await applyToJob(jobId, cvFile);
      setAppliedApplication(application);
      setHasAppliedAfterSubmit(true);
      setIsApplyDialogOpen(false);
      setSelectedCvFile(null);
      setFeedback({ type: "success", message: "Application submitted successfully." });
      restoreApplyButtonFocus();
    } catch (caughtError) {
      const message = getFriendlyErrorMessage(caughtError);

      if (message.toLowerCase().includes("already applied")) {
        setHasAppliedAfterSubmit(true);
      }

      setApplyError(message);
    } finally {
      setIsApplying(false);
    }
  }

  if (isLoading) {
    return <LoadingState message="Loading job details..." />;
  }

  if (error) {
    return (
      <ErrorState
        title="Could not load job"
        message={error}
        action={
          jobId ? (
            <button className="secondary-button" onClick={retryLoad} type="button">
              Retry
            </button>
          ) : undefined
        }
      />
    );
  }

  if (!job) {
    return <EmptyState title="Job not found" message="This job is not available." />;
  }

  return (
    <article className="detail-panel">
      <Link className="back-link" to={jobsReturnPath}>
        <ArrowLeft size={16} />
        {jobsReturnPath.startsWith("/candidate/saved-jobs")
          ? "Back to saved jobs"
          : jobsReturnPath.startsWith("/candidate/applications")
            ? "Back to applications"
            : "Back to jobs"}
      </Link>
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
            <span>Posted {formatDate(job.createdAt)}</span>
          </div>
        </div>
        <div className="detail-header-actions">
          <BookmarkJobButton
            isPending={isBookmarkPending}
            isSaved={isJobSaved}
            jobTitle={job.title}
            onToggle={() => void handleSavedToggle()}
          />
          <button className="primary-button" disabled={!canOpenApplyDialog} onClick={handleOpenApplyDialog} ref={applyButtonRef} type="button">
            {isApplying ? <LoaderCircle className="spin" size={18} /> : alreadyApplied ? <CheckCircle2 size={18} /> : null}
            <span>
              {alreadyApplied
                ? "Already applied"
                : isApplying
                  ? "Submitting"
                  : isProfileMissing
                    ? "Complete profile"
                    : "Apply now"}
            </span>
          </button>
        </div>
      </div>
      {feedback ? (
        <p className={feedback.type === "success" ? "form-success" : "form-error"} role={getFeedbackRole(feedback.type)}>{feedback.message}</p>
      ) : null}
      {applicationLoadError ? <p className="form-error" role="alert">{applicationLoadError}</p> : null}
      {profileLoadError ? <p className="form-error" role="alert">{profileLoadError}</p> : null}
      {bookmarkError ? <p className="form-error" role="alert">{bookmarkError}</p> : null}
      {isProfileMissing ? (
        <section className="detail-section">
          <h3>Profile required to apply</h3>
          <p>Create your candidate profile before applying. You can still read this job and review the company details here.</p>
          <Link className="secondary-button" to="/candidate/profile">
            Go to profile
          </Link>
        </section>
      ) : null}
      {alreadyApplied ? (
        <section className="detail-section">
          <h3>Application</h3>
          {appliedApplication ? (
            <p>
              Applied. Status: {formatEnumLabel(appliedApplication.status)}. Applied: {formatDate(appliedApplication.appliedAt)}.
            </p>
          ) : (
            <p>Application submitted successfully.</p>
          )}
        </section>
      ) : null}
      <section className="detail-section">
        <h3>Job description</h3>
        <p>{job.description}</p>
      </section>
      {showCompanyDetails ? (
        <section className="detail-section">
          <h3>Company</h3>
          <p>
            <strong>{job.companyName}</strong>
          </p>
          {hasText(job.companyDescription) ? <p>{job.companyDescription}</p> : null}
          {hasText(job.companyLocation) ? (
            <p>
              <MapPin size={16} />
              {job.companyLocation}
            </p>
          ) : null}
          {companyWebsiteUrl ? (
            <a className="text-button" href={companyWebsiteUrl} rel="noreferrer" target="_blank">
              Visit website <ExternalLink size={16} />
            </a>
          ) : null}
        </section>
      ) : null}
      {isApplyDialogOpen ? (
        <div className="modal-backdrop">
          <section
            aria-labelledby="apply-dialog-title"
            aria-modal="true"
            className="application-dialog"
            ref={applyDialogRef}
            role="dialog"
            tabIndex={-1}
          >
            <div>
              <p className="eyebrow">Application</p>
              <h2 id="apply-dialog-title">Apply to {job.title}</h2>
            </div>

            <div className="application-dialog-section">
              <h3>
                <FileText size={18} />
                CV / Resume
              </h3>

              <label className={`cv-option ${cvChoice === "saved" ? "cv-option-selected" : ""} ${!hasSavedCv ? "cv-option-disabled" : ""}`}>
                <input
                  checked={cvChoice === "saved"}
                  disabled={!hasSavedCv || isApplying}
                  name="application-cv-choice"
                  onChange={() => setCvChoice("saved")}
                  type="radio"
                  value="saved"
                />
                <span>
                  <strong>Use my saved CV</strong>
                  <small>{hasSavedCv ? savedCvFileName : "No saved CV is available."}</small>
                </span>
              </label>

              <label className={`cv-option ${cvChoice === "custom" ? "cv-option-selected" : ""}`}>
                <input
                  checked={cvChoice === "custom"}
                  disabled={isApplying}
                  name="application-cv-choice"
                  onChange={() => setCvChoice("custom")}
                  type="radio"
                  value="custom"
                />
                <span>
                  <strong>Upload a different CV</strong>
                  <small>This PDF will only be used for this application.</small>
                </span>
              </label>

              {cvChoice === "custom" ? (
                <div className="cv-upload-control">
                  <label className="secondary-button">
                    <Upload size={16} />
                    Choose PDF
                    <input
                      accept=".pdf"
                      disabled={isApplying}
                      onChange={handleCvFileChange}
                      type="file"
                    />
                  </label>
                  {selectedCvFile ? <span className="saving-line">Selected {selectedCvFile.name}</span> : null}
                </div>
              ) : null}
            </div>

            {applyError ? <p className="form-error" role="alert">{applyError}</p> : null}

            <div className="form-actions">
              <button className="secondary-button" disabled={isApplying} onClick={handleCloseApplyDialog} type="button">
                Cancel
              </button>
              <button className="primary-button" disabled={isApplying || alreadyApplied} onClick={() => void handleSubmitApplication()} type="button">
                {isApplying ? <LoaderCircle className="spin" size={18} /> : null}
                {isApplying ? "Submitting..." : "Submit application"}
              </button>
            </div>
          </section>
        </div>
      ) : null}
    </article>
  );
}
