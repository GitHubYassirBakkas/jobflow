import { useEffect, useRef, useState } from "react";
import type { ChangeEvent, FormEvent } from "react";
import {
  Building2,
  ExternalLink,
  LoaderCircle,
  MapPin,
  Pencil
} from "lucide-react";
import { EmptyState, ErrorState, LoadingState } from "../../components/AsyncState";
import {
  getAdminCompanies,
  updateAdminCompany
} from "../../features/admin/adminApi";
import { ApiError } from "../../lib/apiClient";
import { getFriendlyErrorMessage } from "../../lib/errors";
import { formatDate } from "../../lib/formatters";
import type {
  AdminCompany,
  UpdateAdminCompanyRequest
} from "../../types/admin";
import {
  focusFirstElement,
  trapTabKey
} from "../../lib/focusManagement";

function getCompanyErrorMessage(error: unknown) {
  if (
    error instanceof ApiError &&
    error.details &&
    typeof error.details === "object"
  ) {
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

export function AdminCompaniesPage() {
  const [companies, setCompanies] = useState<AdminCompany[]>([]);
  const [error, setError] = useState("");
  const [editCompany, setEditCompany] = useState<AdminCompany | null>(null);
  const [formValues, setFormValues] =
    useState<UpdateAdminCompanyRequest>({
      name: "",
      description: "",
      website: "",
      location: ""
    });
  const [formError, setFormError] = useState("");
  const [isSaving, setIsSaving] = useState(false);
  const [isLoading, setIsLoading] = useState(true);

  const editTriggerRef = useRef<HTMLButtonElement>(null);
  const editDialogRef = useRef<HTMLElement>(null);
  const cancelEditButtonRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    let isMounted = true;

    async function loadCompanies() {
      setIsLoading(true);
      setError("");

      try {
        const response = await getAdminCompanies();

        if (isMounted) {
          setCompanies(response);
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

    void loadCompanies();

    return () => {
      isMounted = false;
    };
  }, []);

  function openEditForm(company: AdminCompany) {
    setEditCompany(company);
    setFormValues({
      name: company.name,
      description: company.description,
      website: company.website,
      location: company.location
    });
    setFormError("");
  }

  function closeEditForm() {
    if (isSaving) {
      return;
    }

    setEditCompany(null);
    setFormError("");
    editTriggerRef.current?.focus();
  }

  useEffect(() => {
    if (!editCompany) {
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

      if (event.key === "Escape" && !isSaving) {
        event.preventDefault();
        closeEditForm();
      }
    }

    document.addEventListener("keydown", handleKeyDown);

    return () => {
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [editCompany, isSaving]);

  function handleFieldChange(
    event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>
  ) {
    const { name, value } = event.target;

    setFormValues((currentValues) => ({
      ...currentValues,
      [name]: value
    }));
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!editCompany || isSaving) {
      return;
    }

    const payload: UpdateAdminCompanyRequest = {
      name: formValues.name.trim(),
      description: formValues.description.trim(),
      website: formValues.website.trim(),
      location: formValues.location.trim()
    };

    if (!payload.name) {
      setFormError("Company name is required.");
      return;
    }

    if (!payload.location) {
      setFormError("Location is required.");
      return;
    }

    setIsSaving(true);
    setFormError("");

    try {
      const updatedCompany = await updateAdminCompany(
        editCompany.id,
        payload
      );

      setCompanies((currentCompanies) =>
        currentCompanies.map((company) =>
          company.id === updatedCompany.id ? updatedCompany : company
        )
      );

      setEditCompany(null);
      editTriggerRef.current?.focus();
    } catch (caughtError) {
      setFormError(getCompanyErrorMessage(caughtError));
    } finally {
      setIsSaving(false);
    }
  }

  if (isLoading) {
    return <LoadingState message="Loading companies..." />;
  }

  if (error) {
    return (
      <ErrorState
        title="Could not load companies"
        message={error}
      />
    );
  }

  if (companies.length === 0) {
    return (
      <EmptyState
        title="No companies found"
        message="Companies registered in JobFlow will appear here when they exist."
      />
    );
  }

  return (
    <div className="page-stack">
      <section className="section-heading">
        <div>
          <p className="eyebrow">Admin</p>
          <h2>Companies</h2>
          <p className="section-description">
            Review company records connected to recruiter hiring activity.
          </p>
        </div>
        <span>{companies.length} total</span>
      </section>

      <section className="jobs-grid">
        {companies.map((company) => (
          <article
            className="job-card company-card"
            key={company.id}
          >
            <div className="card-icon">
              <Building2 size={20} />
            </div>

            <div className="job-card-body">
              <p className="eyebrow">Company #{company.id}</p>
              <h2>{company.name}</h2>

              <div className="meta-row">
                <span>
                  <MapPin size={16} />
                  {company.location}
                </span>
                <span>Created {formatDate(company.createdAt)}</span>
              </div>

              <p className="company-description">
                {company.description}
              </p>
            </div>

            <div className="company-card-actions">
              <button
                className="secondary-button"
                onClick={(event) => {
                  editTriggerRef.current = event.currentTarget;
                  openEditForm(company);
                }}
                type="button"
              >
                <Pencil size={16} />
                Edit
              </button>

              <a
                className="text-button"
                href={company.website}
                rel="noreferrer"
                target="_blank"
              >
                Website <ExternalLink size={16} />
              </a>
            </div>
          </article>
        ))}
      </section>

      {editCompany ? (
        <div className="modal-backdrop">
          <section
            aria-labelledby="company-edit-title"
            aria-modal="true"
            className="application-dialog"
            ref={editDialogRef}
            role="dialog"
            tabIndex={-1}
          >
            <div>
              <p className="eyebrow">
                Company #{editCompany.id}
              </p>
              <h2 id="company-edit-title">Edit company</h2>
            </div>

            <form
              className="form-card"
              onSubmit={(event) => void handleSubmit(event)}
            >
              <label>
                <span>Name</span>
                <input
                  disabled={isSaving}
                  name="name"
                  onChange={handleFieldChange}
                  required
                  type="text"
                  value={formValues.name}
                />
              </label>

              <label>
                <span>Description</span>
                <textarea
                  disabled={isSaving}
                  name="description"
                  onChange={handleFieldChange}
                  value={formValues.description}
                />
              </label>

              <div className="form-grid">
                <label>
                  <span>Website</span>
                  <input
                    disabled={isSaving}
                    name="website"
                    onChange={handleFieldChange}
                    type="url"
                    value={formValues.website}
                  />
                </label>

                <label>
                  <span>Location</span>
                  <input
                    disabled={isSaving}
                    name="location"
                    onChange={handleFieldChange}
                    required
                    type="text"
                    value={formValues.location}
                  />
                </label>
              </div>

              {formError ? (
                <p className="form-error">{formError}</p>
              ) : null}

              <div className="form-actions">
                <button
                  className="secondary-button"
                  disabled={isSaving}
                  onClick={closeEditForm}
                  ref={cancelEditButtonRef}
                  type="button"
                >
                  Cancel
                </button>

                <button
                  className="primary-button"
                  disabled={isSaving}
                  type="submit"
                >
                  {isSaving ? (
                    <LoaderCircle
                      className="spin"
                      size={18}
                    />
                  ) : null}
                  {isSaving ? "Saving..." : "Save"}
                </button>
              </div>
            </form>
          </section>
        </div>
      ) : null}
    </div>
  );
}