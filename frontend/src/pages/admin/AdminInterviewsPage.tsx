import { useEffect, useMemo, useRef, useState } from "react";
import type { FormEvent } from "react";
import {
  ArrowRight,
  BriefcaseBusiness,
  CalendarClock,
  ExternalLink,
  LoaderCircle,
  Mail,
  MapPin,
  MessageSquareText,
  UserRound
} from "lucide-react";
import { Link } from "react-router-dom";
import {
  EmptyState,
  ErrorState,
  LoadingState
} from "../../components/AsyncState";
import { InterviewStatusBadge } from "../../components/StatusBadge";
import {
  getAdminInterviews,
  updateAdminInterviewStatus
} from "../../features/admin/adminApi";
import {
  focusFirstElement,
  trapTabKey
} from "../../lib/focusManagement";
import { getFriendlyErrorMessage } from "../../lib/errors";
import { formatDateTime, formatEnumLabel } from "../../lib/formatters";
import type { InterviewStatus } from "../../types/candidate";
import type { AdminInterview } from "../../types/admin";

const interviewStatuses: InterviewStatus[] = [
  "SCHEDULED",
  "COMPLETED",
  "CANCELLED"
];

export function AdminInterviewsPage() {
  const [interviews, setInterviews] = useState<AdminInterview[]>([]);
  const [error, setError] = useState("");
  const [feedback, setFeedback] = useState<{
    type: "success" | "error";
    message: string;
  } | null>(null);
  const [statusInterview, setStatusInterview] =
    useState<AdminInterview | null>(null);
  const [selectedStatus, setSelectedStatus] =
    useState<InterviewStatus>("SCHEDULED");
  const [statusError, setStatusError] = useState("");
  const [isSavingStatus, setIsSavingStatus] = useState(false);
  const [isLoading, setIsLoading] = useState(true);

  const statusTriggerRef = useRef<HTMLButtonElement | null>(null);
  const statusDialogRef = useRef<HTMLElement | null>(null);
  const cancelStatusButtonRef = useRef<HTMLButtonElement | null>(null);
  const shouldRestoreStatusFocusRef = useRef(false);

  useEffect(() => {
    let isMounted = true;

    async function loadInterviews() {
      setIsLoading(true);
      setError("");

      try {
        const response = await getAdminInterviews();

        if (isMounted) {
          setInterviews(response);
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

    void loadInterviews();

    return () => {
      isMounted = false;
    };
  }, []);

  const sortedInterviews = useMemo(() => {
    return [...interviews].sort(
      (first, second) =>
        new Date(second.scheduledAt).getTime() -
        new Date(first.scheduledAt).getTime()
    );
  }, [interviews]);

  useEffect(() => {
    if (!statusInterview) {
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
  }, [statusInterview, isSavingStatus]);

  function openStatusForm(interview: AdminInterview) {
    shouldRestoreStatusFocusRef.current = false;

    setStatusInterview(interview);
    setSelectedStatus(interview.status);
    setStatusError("");
    setFeedback(null);
  }

  function closeStatusForm() {
    if (isSavingStatus) {
      return;
    }

    shouldRestoreStatusFocusRef.current = true;
    setStatusInterview(null);
    setStatusError("");
  }

  async function handleStatusSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!statusInterview || isSavingStatus) {
      return;
    }

    setIsSavingStatus(true);
    setStatusError("");

    try {
      const updatedInterview = await updateAdminInterviewStatus(
        statusInterview.id,
        selectedStatus
      );

      setInterviews((currentInterviews) =>
        currentInterviews.map((interview) =>
          interview.id === updatedInterview.id
            ? updatedInterview
            : interview
        )
      );

      shouldRestoreStatusFocusRef.current = true;
      setStatusInterview(null);

      setFeedback({
        type: "success",
        message: "Interview status updated."
      });
    } catch (caughtError) {
      setStatusError(getFriendlyErrorMessage(caughtError));
    } finally {
      setIsSavingStatus(false);
    }
  }

  if (isLoading) {
    return <LoadingState message="Loading interviews..." />;
  }

  if (error) {
    return (
      <ErrorState
        title="Could not load interviews"
        message={error}
      />
    );
  }

  if (interviews.length === 0) {
    return (
      <EmptyState
        title="No interviews found"
        message="Scheduled interviews across JobFlow will appear here when they exist."
      />
    );
  }

  return (
    <div className="page-stack">
      <section className="section-heading">
        <div>
          <p className="eyebrow">Admin</p>
          <h2>Interviews</h2>
          <p className="section-description">
            Review scheduled interview activity across candidates and jobs.
          </p>
        </div>
        <span>{interviews.length} total</span>
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
        {sortedInterviews.map((interview) => {
          const location = interview.location?.trim() ?? "";
          const notes = interview.notes?.trim() ?? "";
          const meetingLink = interview.meetingLink?.trim() ?? "";

          return (
            <article
              className="list-card admin-interview-card"
              key={interview.id}
            >
              <div className="user-card-main">
                <div className="card-icon">
                  <CalendarClock size={20} />
                </div>

                <div>
                  <p className="eyebrow">
                    Interview #{interview.id}
                  </p>

                  <h2>
                    <Link
                      className="inline-link"
                      to={`/admin/candidates/${interview.candidateId}`}
                    >
                      {interview.candidateName || "Unnamed candidate"}
                    </Link>
                  </h2>

                  <div className="meta-row">
                    <span>
                      <Mail size={16} />
                      {interview.candidateEmail}
                    </span>

                    <span>
                      <BriefcaseBusiness size={16} />
                      {interview.jobTitle}
                    </span>

                    <span>{formatEnumLabel(interview.type)}</span>
                  </div>

                  <p className="muted-line">
                    <CalendarClock size={16} />
                    {formatDateTime(interview.scheduledAt)}
                  </p>

                  {location ? (
                    <p className="muted-line">
                      <MapPin size={16} />
                      {location}
                    </p>
                  ) : null}

                  {notes ? (
                    <p className="muted-line">
                      <MessageSquareText size={16} />
                      {notes}
                    </p>
                  ) : null}
                </div>
              </div>

              <div className="list-card-actions">
                <InterviewStatusBadge status={interview.status} />

                <button
                  className="secondary-button"
                  disabled={isSavingStatus}
                  onClick={(event) => {
                    statusTriggerRef.current = event.currentTarget;
                    openStatusForm(interview);
                  }}
                  type="button"
                >
                  Change status
                </button>

                {meetingLink ? (
                  <a
                    className="secondary-button"
                    href={meetingLink}
                    rel="noreferrer"
                    target="_blank"
                  >
                    Open meeting <ExternalLink size={16} />
                  </a>
                ) : null}

                <Link
                  className="secondary-button"
                  to={`/admin/candidates/${interview.candidateId}`}
                >
                  Candidate profile <UserRound size={16} />
                  <ArrowRight size={16} />
                </Link>
              </div>
            </article>
          );
        })}
      </section>

      {statusInterview ? (
        <div className="modal-backdrop">
          <section
            ref={statusDialogRef}
            aria-labelledby="interview-status-title"
            aria-modal="true"
            className="application-dialog"
            role="dialog"
            tabIndex={-1}
          >
            <div>
              <p className="eyebrow">
                Interview #{statusInterview.id}
              </p>
              <h2 id="interview-status-title">Change status</h2>
            </div>

            <div className="application-dialog-section">
              <p className="muted-line">
                {statusInterview.candidateName || "Unnamed candidate"}{" "}
                for {statusInterview.jobTitle}
              </p>

              <InterviewStatusBadge status={statusInterview.status} />
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
                      event.target.value as InterviewStatus
                    )
                  }
                  value={selectedStatus}
                >
                  {interviewStatuses.map((status) => (
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