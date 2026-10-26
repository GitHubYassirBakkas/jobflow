import { useEffect, useRef, useState } from "react";
import type { ChangeEvent, FormEvent } from "react";
import {
  BriefcaseBusiness,
  Building2,
  CalendarDays,
  LoaderCircle,
  MapPin,
  Pencil,
  UserRound
} from "lucide-react";
import { EmptyState, ErrorState, LoadingState } from "../../components/AsyncState";
import {
  getAdminJobs,
  updateAdminJob,
  updateAdminJobActive
} from "../../features/admin/adminApi";
import { ApiError } from "../../lib/apiClient";
import {
  focusFirstElement,
  trapTabKey
} from "../../lib/focusManagement";
import { getFriendlyErrorMessage } from "../../lib/errors";
import { formatDate, formatEnumLabel } from "../../lib/formatters";
import type { EmploymentType } from "../../types/candidate";
import type { AdminJob, UpdateAdminJobRequest } from "../../types/admin";

const employmentTypes: EmploymentType[] = [
  "FULL_TIME",
  "PART_TIME",
  "INTERNSHIP",
  "CONTRACT",
  "FREELANCE"
];

function formatEmploymentType(value: AdminJob["employmentType"]) {
  const label = formatEnumLabel(value);

  return label.charAt(0) + label.slice(1).toLowerCase();
}

function getJobErrorMessage(error: unknown) {
  if (error instanceof ApiError && error.details && typeof error.details === "object") {
    const fieldMessages = Object.values(error.details)
      .filter(
        (value): value is string =>
          typeof value === "string" && value.trim().length > 0
      )
      .join(" ");

    if (fieldMessages) {
      return fieldMessages;
    }
  }

  return getFriendlyErrorMessage(error);
}

export function AdminJobsPage() {
  const [jobs, setJobs] = useState<AdminJob[]>([]);
  const [error, setError] = useState("");
  const [feedback, setFeedback] = useState<{
    type: "success" | "error";
    message: string;
  } | null>(null);
  const [savingJobId, setSavingJobId] = useState<number | null>(null);
  const [editJob, setEditJob] = useState<AdminJob | null>(null);
  const [formValues, setFormValues] = useState<UpdateAdminJobRequest>({
    title: "",
    description: "",
    location: "",
    employmentType: "FULL_TIME"
  });
  const [formError, setFormError] = useState("");
  const [isSavingEdit, setIsSavingEdit] = useState(false);
  const [isLoading, setIsLoading] = useState(true);

  const editTriggerRef = useRef<HTMLButtonElement | null>(null);
  const editDialogRef = useRef<HTMLElement | null>(null);
  const cancelEditButtonRef = useRef<HTMLButtonElement | null>(null);
  const shouldRestoreEditFocusRef = useRef(false);

  useEffect(() => {
    let isMounted = true;

    async function loadJobs() {
      setIsLoading(true);
      setError("");

      try {
        const response = await getAdminJobs();

        if (isMounted) {
          setJobs(response);
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
  }, []);

  useEffect(() => {
    if (!editJob) {
      if (!isSavingEdit && shouldRestoreEditFocusRef.current) {
        shouldRestoreEditFocusRef.current = false;
        editTriggerRef.current?.focus();
      }

      return;
    }

    focusFirstElement(
      editDialogRef.current,
      cancelEditButtonRef.current
    );

    function handleKeyDown(event: KeyboardEvent) {
      if (trapTabKey(event, editDialogRef.current)) {
        return;
      }

      if (event.key === "Escape" && !isSavingEdit) {
        event.preventDefault();
        closeEditForm();
      }
    }

    document.addEventListener("keydown", handleKeyDown);

    return () => {
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [editJob, isSavingEdit]);

  async function handleToggleActive(job: AdminJob) {
    const nextActive = !job.active;

    if (!nextActive) {
      const confirmed = window.confirm(`Deactivate ${job.title}?`);

      if (!confirmed) {
        return;
      }
    }

    setSavingJobId(job.id);
    setFeedback(null);

    try {
      const updatedJob = await updateAdminJobActive(job.id, nextActive);

      setJobs((currentJobs) =>
        currentJobs.map((currentJob) =>
          currentJob.id === updatedJob.id ? updatedJob : currentJob
        )
      );

      setFeedback({
        type: "success",
        message: `Job ${updatedJob.active ? "activated" : "deactivated"} successfully.`
      });
    } catch (caughtError) {
      setFeedback({
        type: "error",
        message: getFriendlyErrorMessage(caughtError)
      });
    } finally {
      setSavingJobId(null);
    }
  }

  function openEditForm(job: AdminJob) {
    shouldRestoreEditFocusRef.current = false;

    setEditJob(job);
    setFormValues({
      title: job.title,
      description: job.description,
      location: job.location,
      employmentType: job.employmentType
    });
    setFormError("");
  }

  function closeEditForm() {
    if (isSavingEdit) {
      return;
    }

    shouldRestoreEditFocusRef.current = true;
    setEditJob(null);
    setFormError("");
  }

  function handleFieldChange(
    event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>
  ) {
    const { name, value } = event.target;

    setFormValues((currentValues) => ({
      ...currentValues,
      [name]: name === "employmentType" ? (value as EmploymentType) : value
    }));
  }

  async function handleSubmitEdit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!editJob || isSavingEdit) {
      return;
    }

    const payload: UpdateAdminJobRequest = {
      title: formValues.title.trim(),
      description: formValues.description.trim(),
      location: formValues.location.trim(),
      employmentType: formValues.employmentType
    };

    if (!payload.title) {
      setFormError("Job title is required.");
      return;
    }

    if (!payload.description) {
      setFormError("Job description is required.");
      return;
    }

    if (!payload.location) {
      setFormError("Location is required.");
      return;
    }

    if (!payload.employmentType) {
      setFormError("Employment type is required.");
      return;
    }

    setIsSavingEdit(true);
    setFormError("");

    try {
      const updatedJob = await updateAdminJob(editJob.id, payload);

      setJobs((currentJobs) =>
        currentJobs.map((job) =>
          job.id === updatedJob.id ? updatedJob : job
        )
      );

      shouldRestoreEditFocusRef.current = true;
      setEditJob(null);

      setFeedback({
        type: "success",
        message: "Job updated successfully."
      });
    } catch (caughtError) {
      setFormError(getJobErrorMessage(caughtError));
    } finally {
      setIsSavingEdit(false);
    }
  }

  if (isLoading) {
    return <LoadingState message="Loading jobs..." />;
  }

  if (error) {
    return <ErrorState title="Could not load jobs" message={error} />;
  }

  if (jobs.length === 0) {
    return (
      <EmptyState
        title="No jobs found"
        message="Jobs posted across JobFlow will appear here when they exist."
      />
    );
  }

  return (
    <div className="page-stack">
      <section className="section-heading">
        <div>
          <p className="eyebrow">Admin</p>
          <h2>Jobs</h2>
          <p className="section-description">
            Review all active and inactive job listings across the platform.
          </p>
        </div>
        <span>{jobs.length} total</span>
      </section>

      {feedback ? (
        <p
          className={
            feedback.type === "success" ? "form-success" : "form-error"
          }
          role="status"
          aria-live="polite"
        >
          {feedback.message}
        </p>
      ) : null}

      <section className="jobs-grid">
        {jobs.map((job) => {
          const isSaving = savingJobId === job.id;

          return (
            <article className="job-card admin-job-card" key={job.id}>
              <div className="admin-job-card-header">
                <div className="card-icon">
                  <BriefcaseBusiness size={20} />
                </div>

                <span
                  className={`status-badge ${
                    job.active ? "status-success" : "status-danger"
                  }`}
                >
                  {job.active ? "Active" : "Inactive"}
                </span>
              </div>

              <div className="job-card-body">
                <p className="eyebrow">Job #{job.id}</p>
                <h2>{job.title}</h2>

                <div className="meta-row">
                  <span>
                    <Building2 size={16} />
                    {job.companyName}
                  </span>

                  <span>
                    <MapPin size={16} />
                    {job.location}
                  </span>

                  <span>{formatEmploymentType(job.employmentType)}</span>
                </div>

                <p className="job-description">{job.description}</p>
              </div>

              <div className="admin-job-card-footer">
                <span>
                  <UserRound size={16} />
                  {job.createdByName}
                </span>

                <span>
                  <CalendarDays size={16} />
                  Created {formatDate(job.createdAt)}
                </span>

                <button
                  className="secondary-button"
                  disabled={savingJobId !== null || isSavingEdit}
                  onClick={(event) => {
                    editTriggerRef.current = event.currentTarget;
                    openEditForm(job);
                  }}
                  type="button"
                >
                  <Pencil size={16} />
                  Edit
                </button>

                <button
                  className="secondary-button"
                  disabled={savingJobId !== null || isSavingEdit}
                  onClick={() => void handleToggleActive(job)}
                  type="button"
                >
                  {isSaving
                    ? job.active
                      ? "Deactivating..."
                      : "Activating..."
                    : job.active
                      ? "Deactivate"
                      : "Activate"}
                </button>
              </div>
            </article>
          );
        })}
      </section>

      {editJob ? (
        <div className="modal-backdrop">
          <section
            ref={editDialogRef}
            aria-labelledby="job-edit-title"
            aria-modal="true"
            className="application-dialog"
            role="dialog"
            tabIndex={-1}
          >
            <div>
              <p className="eyebrow">Job #{editJob.id}</p>
              <h2 id="job-edit-title">Edit job</h2>
            </div>

            <form
              className="form-card"
              onSubmit={(event) => void handleSubmitEdit(event)}
            >
              <label>
                <span>Title</span>
                <input
                  disabled={isSavingEdit}
                  name="title"
                  onChange={handleFieldChange}
                  required
                  type="text"
                  value={formValues.title}
                />
              </label>

              <label>
                <span>Description</span>
                <textarea
                  disabled={isSavingEdit}
                  name="description"
                  onChange={handleFieldChange}
                  required
                  value={formValues.description}
                />
              </label>

              <div className="form-grid">
                <label>
                  <span>Location</span>
                  <input
                    disabled={isSavingEdit}
                    name="location"
                    onChange={handleFieldChange}
                    required
                    type="text"
                    value={formValues.location}
                  />
                </label>

                <label>
                  <span>Employment type</span>
                  <select
                    disabled={isSavingEdit}
                    name="employmentType"
                    onChange={handleFieldChange}
                    required
                    value={formValues.employmentType}
                  >
                    {employmentTypes.map((employmentType) => (
                      <option key={employmentType} value={employmentType}>
                        {formatEnumLabel(employmentType)}
                      </option>
                    ))}
                  </select>
                </label>
              </div>

              {formError ? (
                <p className="form-error" role="alert">
                  {formError}
                </p>
              ) : null}

              <div className="form-actions">
                <button
                  ref={cancelEditButtonRef}
                  className="secondary-button"
                  disabled={isSavingEdit}
                  onClick={closeEditForm}
                  type="button"
                >
                  Cancel
                </button>

                <button
                  className="primary-button"
                  disabled={isSavingEdit}
                  type="submit"
                >
                  {isSavingEdit ? (
                    <LoaderCircle className="spin" size={18} />
                  ) : null}
                  {isSavingEdit ? "Saving..." : "Save"}
                </button>
              </div>
            </form>
          </section>
        </div>
      ) : null}
    </div>
  );
}