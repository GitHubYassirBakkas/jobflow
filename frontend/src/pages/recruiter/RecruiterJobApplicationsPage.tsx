import { ChangeEvent, useEffect, useMemo, useState } from "react";
import { ArrowLeft, CalendarPlus, CheckCircle2, Download, Eye, FileText, LoaderCircle } from "lucide-react";
import { Link, useParams } from "react-router-dom";
import { EmptyState, ErrorState, LoadingState } from "../../components/AsyncState";
import { ApplicationStatusBadge, InterviewStatusBadge } from "../../components/StatusBadge";
import { assignableApplicationStatuses, isApplicationStatusAssignable } from "../../features/applications/applicationStatusOptions";
import { ScheduleInterviewForm } from "../../features/recruiter/components/ScheduleInterviewForm";
import {
  getRecruiterApplicationCv,
  getRecruiterJob,
  getRecruiterJobApplications,
  getRecruiterProfile,
  updateRecruiterApplicationStatus,
  updateRecruiterInterviewStatus
} from "../../features/recruiter/recruiterApi";
import { isRecruiterOwnedJob } from "../../features/recruiter/recruiterUtils";
import { getFriendlyErrorMessage } from "../../lib/errors";
import { formatDate, formatDateTime, formatEnumLabel } from "../../lib/formatters";
import type { ApplicationStatus, InterviewStatus } from "../../types/candidate";
import type { RecruiterApplication, RecruiterInterview, RecruiterJob } from "../../types/recruiter";

const interviewStatuses: InterviewStatus[] = ["SCHEDULED", "COMPLETED", "CANCELLED"];

type CvAction = "view" | "download";

interface ApplicationsState {
  job: RecruiterJob;
  applications: RecruiterApplication[];
}

function getCvErrorMessage(error: unknown) {
  if (error instanceof Error && error.message === "cv-popup-blocked") {
    return "Your browser blocked opening the CV. Allow pop-ups for JobFlow and try again.";
  }

  return getFriendlyErrorMessage(error);
}

export function RecruiterJobApplicationsPage() {
  const { jobId } = useParams();
  const [data, setData] = useState<ApplicationsState | null>(null);
  const [scheduledInterviews, setScheduledInterviews] = useState<Record<number, RecruiterInterview>>({});
  const [openScheduleFor, setOpenScheduleFor] = useState<number | null>(null);
  const [savingApplicationId, setSavingApplicationId] = useState<number | null>(null);
  const [savingInterviewId, setSavingInterviewId] = useState<number | null>(null);
  const [cvAction, setCvAction] = useState<{ applicationId: number; type: CvAction } | null>(null);
  const [feedback, setFeedback] = useState<{ type: "success" | "error"; message: string } | null>(null);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let isMounted = true;

    async function loadApplications() {
      if (!jobId) {
        setError("We could not find that job.");
        setIsLoading(false);
        return;
      }

      setIsLoading(true);
      setError("");

      try {
        const [jobResponse, profileResponse] = await Promise.all([getRecruiterJob(jobId), getRecruiterProfile()]);

        if (!isRecruiterOwnedJob(jobResponse, profileResponse)) {
          throw new Error("This job is not owned by your company.");
        }

        const applicationsResponse = await getRecruiterJobApplications(jobId);

        if (isMounted) {
          setData({ job: jobResponse, applications: applicationsResponse });
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
  }, [jobId]);

  const sortedApplications = useMemo(() => {
    return [...(data?.applications ?? [])].sort(
      (first, second) => new Date(second.appliedAt).getTime() - new Date(first.appliedAt).getTime()
    );
  }, [data]);

  async function handleStatusChange(application: RecruiterApplication, event: ChangeEvent<HTMLSelectElement>) {
    const nextStatus = event.target.value as ApplicationStatus;

    if (nextStatus === application.status) {
      return;
    }

    setSavingApplicationId(application.id);
    setFeedback(null);

    try {
      const updatedApplication = await updateRecruiterApplicationStatus(application.id, { status: nextStatus });
      setData((current) =>
        current
          ? {
              ...current,
              applications: current.applications.map((item) =>
                item.id === updatedApplication.id ? updatedApplication : item
              )
            }
          : current
      );
      setFeedback({ type: "success", message: "Application status updated." });
    } catch (caughtError) {
      setFeedback({ type: "error", message: getFriendlyErrorMessage(caughtError) });
    } finally {
      setSavingApplicationId(null);
    }
  }

  async function handleCvAction(application: RecruiterApplication, action: CvAction) {
    const cvFileName = application.cvFileName?.trim() ?? "";

    if (!cvFileName || cvAction) {
      return;
    }

    let previewWindow: Window | null = null;

    if (action === "view") {
      previewWindow = window.open("", "_blank");

      if (!previewWindow) {
        setFeedback({ type: "error", message: getCvErrorMessage(new Error("cv-popup-blocked")) });
        return;
      }

      previewWindow.opener = null;
    }

    setCvAction({ applicationId: application.id, type: action });
    setFeedback(null);

    let objectUrl: string | null = null;

    try {
      const blob = await getRecruiterApplicationCv(application.id);
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

      setFeedback({ type: "error", message: getCvErrorMessage(caughtError) });
    } finally {
      if (objectUrl) {
        URL.revokeObjectURL(objectUrl);
      }

      setCvAction(null);
    }
  }

  async function handleInterviewStatusChange(interview: RecruiterInterview, nextStatus: InterviewStatus) {
    if (nextStatus === interview.status) {
      return;
    }

    setSavingInterviewId(interview.id);
    setFeedback(null);

    try {
      const updatedInterview = await updateRecruiterInterviewStatus(interview.id, { status: nextStatus });
      setScheduledInterviews((current) => ({
        ...current,
        [updatedInterview.applicationId]: updatedInterview
      }));
      setFeedback({ type: "success", message: "Interview status updated." });
    } catch (caughtError) {
      setFeedback({ type: "error", message: getFriendlyErrorMessage(caughtError) });
    } finally {
      setSavingInterviewId(null);
    }
  }

  if (isLoading) {
    return <LoadingState message="Loading job applications..." />;
  }

  if (error) {
    return <ErrorState title="Could not load applications" message={error} />;
  }

  if (!data) {
    return <EmptyState title="Applications unavailable" message="Applications could not be loaded." />;
  }

  return (
    <div className="page-stack">
      <section className="section-heading">
        <div>
          <Link className="back-link" to={`/recruiter/jobs/${data.job.id}`}>
            <ArrowLeft size={16} />
            Back to job
          </Link>
          <p className="eyebrow">{data.job.companyName}</p>
          <h2>{data.job.title} applications</h2>
        </div>
        <span>{data.applications.length} total</span>
      </section>

      {feedback ? (
        <p className={feedback.type === "success" ? "form-success" : "form-error"}>{feedback.message}</p>
      ) : null}

      {data.applications.length === 0 ? (
        <EmptyState
          title="No applications yet"
          message="Candidates who apply to this job will appear here."
          action={
            <Link className="secondary-button" to="/recruiter/jobs">
              View jobs
            </Link>
          }
        />
      ) : (
        <section className="list-stack">
          {sortedApplications.map((application) => {
            const scheduledInterview = scheduledInterviews[application.id];
            const cvFileName = application.cvFileName?.trim() ?? "";
            const isCvLoading = cvAction?.applicationId === application.id;
            const canManageStatus = isApplicationStatusAssignable(application.status);

            return (
              <article className="list-card recruiter-application-card" key={application.id}>
                <div className="application-main">
                  <p className="eyebrow">{application.candidateEmail}</p>
                  <h2>{application.candidateName}</h2>
                  <p className="muted-line">Applied {formatDate(application.appliedAt)}</p>
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
                            onClick={() => void handleCvAction(application, "view")}
                            type="button"
                          >
                            {isCvLoading && cvAction?.type === "view" ? (
                              <LoaderCircle className="spin" size={16} />
                            ) : (
                              <Eye size={16} />
                            )}
                            View CV
                          </button>
                          <button
                            className="secondary-button"
                            disabled={cvAction !== null}
                            onClick={() => void handleCvAction(application, "download")}
                            type="button"
                          >
                            {isCvLoading && cvAction?.type === "download" ? (
                              <LoaderCircle className="spin" size={16} />
                            ) : (
                              <Download size={16} />
                            )}
                            Download
                          </button>
                        </div>
                      </div>
                    ) : (
                      <p className="muted-line">No CV attached</p>
                    )}
                  </section>
                  <ApplicationStatusBadge status={application.status} />
                </div>
                <div className="application-controls">
                  {canManageStatus ? (
                    <label>
                      <span>Status</span>
                      <select
                        disabled={savingApplicationId === application.id}
                        onChange={(event) => void handleStatusChange(application, event)}
                        value={application.status}
                      >
                        {assignableApplicationStatuses.map((status) => (
                          <option key={status} value={status}>
                            {formatEnumLabel(status)}
                          </option>
                        ))}
                      </select>
                    </label>
                  ) : (
                    <span className="saving-line">Status is read-only</span>
                  )}
                  {savingApplicationId === application.id ? (
                    <span className="saving-line">
                      <LoaderCircle className="spin" size={16} />
                      Saving
                    </span>
                  ) : null}
                  <button
                    className="secondary-button"
                    disabled={!canManageStatus}
                    onClick={() => setOpenScheduleFor(openScheduleFor === application.id ? null : application.id)}
                    type="button"
                  >
                    <CalendarPlus size={16} />
                    Schedule interview
                  </button>
                </div>
                {openScheduleFor === application.id ? (
                  <ScheduleInterviewForm
                    applicationId={application.id}
                    onCancel={() => setOpenScheduleFor(null)}
                    onScheduled={(interview) => {
                      setScheduledInterviews((current) => ({ ...current, [interview.applicationId]: interview }));
                      setOpenScheduleFor(null);
                      setFeedback({ type: "success", message: "Interview scheduled successfully." });
                    }}
                  />
                ) : null}
                {scheduledInterview ? (
                  <section className="scheduled-interview-panel">
                    <div>
                      <p className="eyebrow">Scheduled interview</p>
                      <h3>{formatDateTime(scheduledInterview.scheduledAt)}</h3>
                      <p className="muted-line">
                        {formatEnumLabel(scheduledInterview.type)}
                        {scheduledInterview.meetingLink ? ` | ${scheduledInterview.meetingLink}` : ""}
                        {scheduledInterview.location ? ` | ${scheduledInterview.location}` : ""}
                      </p>
                    </div>
                    <div className="interview-status-actions">
                      <InterviewStatusBadge status={scheduledInterview.status} />
                      <label>
                        <span>Interview status</span>
                        <select
                          disabled={savingInterviewId === scheduledInterview.id}
                          onChange={(event) =>
                            void handleInterviewStatusChange(scheduledInterview, event.target.value as InterviewStatus)
                          }
                          value={scheduledInterview.status}
                        >
                          {interviewStatuses.map((status) => (
                            <option key={status} value={status}>
                              {formatEnumLabel(status)}
                            </option>
                          ))}
                        </select>
                      </label>
                      {savingInterviewId === scheduledInterview.id ? (
                        <span className="saving-line">
                          <LoaderCircle className="spin" size={16} />
                          Saving
                        </span>
                      ) : (
                        <span className="saving-line">
                          <CheckCircle2 size={16} />
                          Real interview ID #{scheduledInterview.id}
                        </span>
                      )}
                    </div>
                  </section>
                ) : null}
              </article>
            );
          })}
        </section>
      )}
    </div>
  );
}
