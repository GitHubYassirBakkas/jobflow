import { useEffect, useMemo, useRef, useState } from "react";
import type { FormEvent } from "react";
import {
  ArrowRight,
  BriefcaseBusiness,
  CalendarDays,
  Download,
  Eye,
  FileText,
  LoaderCircle,
  Mail,
  UserRound
} from "lucide-react";
import { Link } from "react-router-dom";
import {
  EmptyState,
  ErrorState,
  LoadingState
} from "../../components/AsyncState";
import { ApplicationStatusBadge } from "../../components/StatusBadge";
import {
  assignableApplicationStatuses,
  isApplicationStatusAssignable
} from "../../features/applications/applicationStatusOptions";
import {
  getAdminApplicationCv,
  getAdminApplications,
  updateAdminApplicationStatus
} from "../../features/admin/adminApi";
import {
  focusFirstElement,
  trapTabKey
} from "../../lib/focusManagement";
import { getFriendlyErrorMessage } from "../../lib/errors";
import { formatDate, formatEnumLabel } from "../../lib/formatters";
import type { ApplicationStatus } from "../../types/candidate";
import type { AdminApplication } from "../../types/admin";

type CvAction = "view" | "download";

function getCvErrorMessage(error: unknown) {
  if (error instanceof Error && error.message === "cv-popup-blocked") {
    return "Your browser blocked opening the CV. Allow pop-ups for JobFlow and try again.";
  }

  return getFriendlyErrorMessage(error);
}

export function AdminApplicationsPage() {
  const [applications, setApplications] = useState<AdminApplication[]>([]);
  const [error, setError] = useState("");
  const [feedback, setFeedback] = useState<{
    type: "success" | "error";
    message: string;
  } | null>(null);
  const [cvAction, setCvAction] = useState<{
    applicationId: number;
    type: CvAction;
  } | null>(null);
  const [statusApplication, setStatusApplication] =
    useState<AdminApplication | null>(null);
  const [selectedStatus, setSelectedStatus] =
    useState<ApplicationStatus>("APPLIED");
  const [statusError, setStatusError] = useState("");
  const [isSavingStatus, setIsSavingStatus] = useState(false);
  const [isLoading, setIsLoading] = useState(true);

  const statusTriggerRef = useRef<HTMLButtonElement | null>(null);
  const statusDialogRef = useRef<HTMLElement | null>(null);
  const cancelStatusButtonRef = useRef<HTMLButtonElement | null>(null);
  const shouldRestoreStatusFocusRef = useRef(false);

  useEffect(() => {
    let isMounted = true;

    async function loadApplications() {
      setIsLoading(true);
      setError("");

      try {
        const response = await getAdminApplications();

        if (isMounted) {
          setApplications(response);
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

  const sortedApplications = useMemo(() => {
    return [...applications].sort(
      (first, second) =>
        new Date(second.appliedAt).getTime() -
        new Date(first.appliedAt).getTime()
    );
  }, [applications]);

  useEffect(() => {
    if (!statusApplication) {
      if (!isSavingStatus && shouldRestoreStatusFocusRef.current) {
        shouldRestoreStatusFocusRef.current = false;
        statusTriggerRef.current?.focus();
      }

      return;
    }

    focusFirstElement(
      statusDialogRef.current,
      cancelStatusButtonRef.current
    );

    function handleKeyDown(event: KeyboardEvent) {
      if (trapTabKey(event, statusDialogRef.current)) {
        return;
      }

      if (event.key === "Escape" && !isSavingStatus) {
        event.preventDefault();
        closeStatusForm();
      }
    }

    document.addEventListener("keydown", handleKeyDown);

    return () => {
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [statusApplication, isSavingStatus]);

  async function handleCvAction(
    application: AdminApplication,
    action: CvAction
  ) {
    const cvFileName = application.cvFileName?.trim() ?? "";

    if (!cvFileName || cvAction) {
      return;
    }

    let previewWindow: Window | null = null;

    if (action === "view") {
      previewWindow = window.open("", "_blank");

      if (!previewWindow) {
        setFeedback({
          type: "error",
          message: getCvErrorMessage(new Error("cv-popup-blocked"))
        });
        return;
      }

      previewWindow.opener = null;
    }

    setCvAction({ applicationId: application.id, type: action });
    setFeedback(null);

    let objectUrl: string | null = null;

    try {
      const blob = await getAdminApplicationCv(application.id);
      objectUrl = URL.createObjectURL(blob);

      if (action === "view") {
        if (!previewWindow) {
          throw new Error("cv-popup-blocked");
        }

        const viewUrl = objectUrl;
        previewWindow.location.href = viewUrl;

        window.setTimeout(() => URL.revokeObjectURL(viewUrl), 60_000);
        objectUrl = null;
        return;
      }

      const downloadLink = document.createElement("a");
      downloadLink.href = objectUrl;
      downloadLink.download = cvFileName;

      document.body.appendChild(downloadLink);
      downloadLink.click();
      downloadLink.remove();
    } catch (caughtError) {
      if (previewWindow && !previewWindow.closed) {
        previewWindow.close();
      }

      setFeedback({
        type: "error",
        message: getCvErrorMessage(caughtError)
      });
    } finally {
      if (objectUrl) {
        URL.revokeObjectURL(objectUrl);
      }

      setCvAction(null);
    }
  }

  function openStatusForm(application: AdminApplication) {
    if (!isApplicationStatusAssignable(application.status)) {
      return;
    }

    shouldRestoreStatusFocusRef.current = false;

    setStatusApplication(application);
    setSelectedStatus(application.status);
    setStatusError("");
    setFeedback(null);
  }

  function closeStatusForm() {
    if (isSavingStatus) {
      return;
    }

    shouldRestoreStatusFocusRef.current = true;
    setStatusApplication(null);
    setStatusError("");
  }

  async function handleStatusSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!statusApplication || isSavingStatus) {
      return;
    }

    setIsSavingStatus(true);
    setStatusError("");

    try {
      const updatedApplication = await updateAdminApplicationStatus(
        statusApplication.id,
        selectedStatus
      );

      setApplications((currentApplications) =>
        currentApplications.map((application) =>
          application.id === updatedApplication.id
            ? updatedApplication
            : application
        )
      );

      shouldRestoreStatusFocusRef.current = true;
      setStatusApplication(null);

      setFeedback({
        type: "success",
        message: "Application status updated."
      });
    } catch (caughtError) {
      setStatusError(getFriendlyErrorMessage(caughtError));
    } finally {
      setIsSavingStatus(false);
    }
  }

  if (isLoading) {
    return <LoadingState message="Loading applications..." />;
  }

  if (error) {
    return (
      <ErrorState
        title="Could not load applications"
        message={error}
      />
    );
  }

  if (applications.length === 0) {
    return (
      <EmptyState
        title="No applications found"
        message="Candidate applications across JobFlow will appear here when they exist."
      />
    );
  }

  return (
    <div className="page-stack">
      <section className="section-heading">
        <div>
          <p className="eyebrow">Admin</p>
          <h2>Applications</h2>
          <p className="section-description">
            Review candidate submissions across jobs and companies.
          </p>
        </div>

        <span>{applications.length} total</span>
      </section>

      {feedback ? (
        <p
          className={
            feedback.type === "success"
              ? "form-success"
              : "form-error"
          }
          role="status"
          aria-live="polite"
        >
          {feedback.message}
        </p>
      ) : null}

      <section className="list-stack">
        {sortedApplications.map((application) => {
          const cvFileName = application.cvFileName?.trim() ?? "";
          const isCvLoading =
            cvAction?.applicationId === application.id;

          return (
            <article
              className="list-card admin-application-card"
              key={application.id}
            >
              <div className="user-card-main">
                <div className="card-icon">
                  <BriefcaseBusiness size={20} />
                </div>

                <div>
                  <p className="eyebrow">
                    Application #{application.id}
                  </p>

                  <h2>
                    <Link
                      className="inline-link"
                      to={`/admin/candidates/${application.candidateId}`}
                    >
                      {application.candidateName || "Unnamed candidate"}
                    </Link>
                  </h2>

                  <div className="meta-row">
                    <span>
                      <Mail size={16} />
                      {application.candidateEmail}
                    </span>

                    <span>
                      <BriefcaseBusiness size={16} />
                      {application.jobTitle}
                    </span>

                    <span>{application.companyName}</span>
                  </div>

                  <section className="application-cv-section">
                    <p className="eyebrow">CV / Resume</p>

                    {cvFileName ? (
                      <div className="cv-panel">
                        <div className="cv-file">
                          <FileText size={20} />
                          <strong>{cvFileName}</strong>
                        </div>

                        <div className="cv-actions">
                          <button
                            className="primary-button"
                            disabled={cvAction !== null}
                            onClick={() =>
                              void handleCvAction(application, "view")
                            }
                            type="button"
                          >
                            {isCvLoading &&
                            cvAction?.type === "view" ? (
                              <LoaderCircle
                                className="spin"
                                size={16}
                              />
                            ) : (
                              <Eye size={16} />
                            )}
                            View CV
                          </button>

                          <button
                            className="secondary-button"
                            disabled={cvAction !== null}
                            onClick={() =>
                              void handleCvAction(application, "download")
                            }
                            type="button"
                          >
                            {isCvLoading &&
                            cvAction?.type === "download" ? (
                              <LoaderCircle
                                className="spin"
                                size={16}
                              />
                            ) : (
                              <Download size={16} />
                            )}
                            Download
                          </button>
                        </div>
                      </div>
                    ) : (
                      <p className="muted-line">
                        <FileText size={16} />
                        No CV attached
                      </p>
                    )}
                  </section>
                </div>
              </div>

              <div className="list-card-actions">
                <ApplicationStatusBadge status={application.status} />

                <button
                  className="secondary-button"
                  disabled={
                    isSavingStatus ||
                    !isApplicationStatusAssignable(application.status)
                  }
                  onClick={(event) => {
                    statusTriggerRef.current = event.currentTarget;
                    openStatusForm(application);
                  }}
                  type="button"
                >
                  Change status
                </button>

                <span className="saving-line">
                  <CalendarDays size={16} />
                  Applied {formatDate(application.appliedAt)}
                </span>

                <Link
                  className="secondary-button"
                  to={`/admin/candidates/${application.candidateId}`}
                >
                  Candidate profile <UserRound size={16} />
                  <ArrowRight size={16} />
                </Link>
              </div>
            </article>
          );
        })}
      </section>

      {statusApplication ? (
        <div className="modal-backdrop">
          <section
            ref={statusDialogRef}
            aria-labelledby="application-status-title"
            aria-modal="true"
            className="application-dialog"
            role="dialog"
            tabIndex={-1}
          >
            <div>
              <p className="eyebrow">
                Application #{statusApplication.id}
              </p>
              <h2 id="application-status-title">Change status</h2>
            </div>

            <div className="application-dialog-section">
              <p className="muted-line">
                {statusApplication.candidateName || "Unnamed candidate"}{" "}
                for {statusApplication.jobTitle}
              </p>

              <ApplicationStatusBadge status={statusApplication.status} />
            </div>

            <form
              className="form-card"
              onSubmit={(event) => void handleStatusSubmit(event)}
            >
              <label>
                <span>Status</span>
                <select
                  disabled={isSavingStatus}
                  onChange={(event) =>
                    setSelectedStatus(
                      event.target.value as ApplicationStatus
                    )
                  }
                  value={selectedStatus}
                >
                  {assignableApplicationStatuses.map((status) => (
                    <option key={status} value={status}>
                      {formatEnumLabel(status)}
                    </option>
                  ))}
                </select>
              </label>

              {statusError ? (
                <p className="form-error" role="alert">
                  {statusError}
                </p>
              ) : null}

              <div className="form-actions">
                <button
                  ref={cancelStatusButtonRef}
                  className="secondary-button"
                  disabled={isSavingStatus}
                  onClick={closeStatusForm}
                  type="button"
                >
                  Cancel
                </button>

                <button
                  className="primary-button"
                  disabled={isSavingStatus}
                  type="submit"
                >
                  {isSavingStatus ? (
                    <LoaderCircle className="spin" size={18} />
                  ) : null}
                  {isSavingStatus ? "Saving..." : "Save"}
                </button>
              </div>
            </form>
          </section>
        </div>
      ) : null}
    </div>
  );
}