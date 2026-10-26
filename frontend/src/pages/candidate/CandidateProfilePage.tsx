import { useEffect, useMemo, useRef, useState } from "react";
import type { ChangeEvent, FormEvent } from "react";
import {
  CalendarDays,
  Check,
  Edit3,
  Eye,
  FileText,
  LoaderCircle,
  Mail,
  MapPin,
  Phone,
  Trash2,
  Upload,
  UserRound,
  X
} from "lucide-react";
import { ErrorState, LoadingState } from "../../components/AsyncState";
import {
  deleteMyCandidateCv,
  getMyCandidateCv,
  getMyCandidateProfile,
  updateMyCandidateProfile,
  uploadMyCandidateCv
} from "../../features/candidate/candidateApi";
import { ApiError } from "../../lib/apiClient";
import { getFriendlyErrorMessage } from "../../lib/errors";
import { getFeedbackRole } from "../../lib/feedback";
import { formatDate } from "../../lib/formatters";
import type { CandidateProfile, CandidateProfileInput } from "../../types/candidate";

type Feedback = { type: "success" | "error"; message: string } | null;

const emptyProfileForm: CandidateProfileInput = {
  phone: "",
  location: "",
  bio: "",
  skills: ""
};

const maxCvSizeBytes = 5 * 1024 * 1024;

function getFullName(profile: CandidateProfile) {
  return `${profile.firstName} ${profile.lastName}`.trim();
}

function getSkillList(skills: string | null) {
  return (skills ?? "")
    .split(",")
    .map((skill) => skill.trim())
    .filter(Boolean);
}

function profileToForm(profile: CandidateProfile | null): CandidateProfileInput {
  if (!profile) {
    return emptyProfileForm;
  }

  return {
    phone: profile.phone ?? "",
    location: profile.location ?? "",
    bio: profile.bio ?? "",
    skills: profile.skills ?? ""
  };
}

function getVisualCompleteness(value: number) {
  return Math.min(100, Math.max(0, value));
}

function isPdfFile(file: File) {
  return file.type === "application/pdf" || file.name.toLowerCase().endsWith(".pdf");
}

function sanitizeFeedbackMessage(message: string) {
  const lowerMessage = message.toLowerCase();

  if (message.includes("\n") || lowerMessage.includes("exception") || lowerMessage.includes("stacktrace")) {
    return "We could not complete that CV request. Please try again.";
  }

  return message;
}

function getCvFeedbackMessage(error: unknown) {
  if (error instanceof Error && error.message === "cv-popup-blocked") {
    return "Your browser blocked opening the CV. Allow pop-ups for JobFlow and try again.";
  }

  return sanitizeFeedbackMessage(getFriendlyErrorMessage(error));
}

export function CandidateProfilePage() {
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const [profile, setProfile] = useState<CandidateProfile | null>(null);
  const [error, setError] = useState("");
  const [feedback, setFeedback] = useState<Feedback>(null);
  const [formError, setFormError] = useState("");
  const [formValues, setFormValues] = useState<CandidateProfileInput>(emptyProfileForm);
  const [isProfileMissing, setIsProfileMissing] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [isUploading, setIsUploading] = useState(false);
  const [isDeletingCv, setIsDeletingCv] = useState(false);
  const [isViewingCv, setIsViewingCv] = useState(false);
  const [selectedFileName, setSelectedFileName] = useState("");
  const [reloadKey, setReloadKey] = useState(0);

  async function refreshProfile() {
    const response = await getMyCandidateProfile();
    setProfile(response);
    setFormValues(profileToForm(response));
    setIsProfileMissing(false);
    return response;
  }

  useEffect(() => {
    let isMounted = true;

    async function loadProfile() {
      setIsLoading(true);
      setError("");
      setFeedback(null);
      setFormError("");

      try {
        const response = await getMyCandidateProfile();

        if (isMounted) {
          setProfile(response);
          setFormValues(profileToForm(response));
          setIsProfileMissing(false);
        }
      } catch (caughtError) {
        if (isMounted) {
          if (caughtError instanceof ApiError && caughtError.status === 404) {
            setProfile(null);
            setFormValues(emptyProfileForm);
            setIsProfileMissing(true);
          } else {
            setError(getFriendlyErrorMessage(caughtError));
          }
        }
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    void loadProfile();

    return () => {
      isMounted = false;
    };
  }, [reloadKey]);

  function retryLoad() {
    setReloadKey((current) => current + 1);
  }

  const skills = useMemo(() => getSkillList(profile?.skills ?? null), [profile]);
  const cvFileName = profile?.cvFileName?.trim() ?? "";
  const isCvBusy = isViewingCv || isUploading || isDeletingCv;
  const completeness = profile ? getVisualCompleteness(profile.profileCompleteness) : 0;

  function updateField(field: keyof CandidateProfileInput, value: string) {
    setFormValues((current) => ({ ...current, [field]: value }));
  }

  function handleStartEdit() {
    setFormValues(profileToForm(profile));
    setFormError("");
    setFeedback(null);
    setIsEditing(true);
  }

  function handleCancelEdit() {
    setFormValues(profileToForm(profile));
    setFormError("");
    setIsEditing(false);
  }

  async function handleSubmitProfile(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (isSaving) {
      return;
    }

    setIsSaving(true);
    setFormError("");
    setFeedback(null);

    try {
      const savedProfile = await updateMyCandidateProfile(formValues);
      setProfile(savedProfile);
      setFormValues(profileToForm(savedProfile));
      setIsProfileMissing(false);
      setIsEditing(false);
      setFeedback({ type: "success", message: isProfileMissing ? "Profile created successfully." : "Profile updated successfully." });
    } catch (caughtError) {
      setFormError(getFriendlyErrorMessage(caughtError));
    } finally {
      setIsSaving(false);
    }
  }

  async function handleViewCv() {
    if (!cvFileName || isCvBusy) {
      return;
    }

    const previewWindow = window.open("", "_blank");

    if (!previewWindow) {
      setFeedback({ type: "error", message: getCvFeedbackMessage(new Error("cv-popup-blocked")) });
      return;
    }

    setIsViewingCv(true);
    setFeedback(null);

    let objectUrl: string | null = null;

    try {
      const blob = await getMyCandidateCv();
      objectUrl = URL.createObjectURL(blob);

      const viewUrl = objectUrl;
      previewWindow.location.href = viewUrl;
      window.setTimeout(() => URL.revokeObjectURL(viewUrl), 60_000);
      objectUrl = null;
    } catch (caughtError) {
      if (!previewWindow.closed) {
        previewWindow.close();
      }

      setFeedback({ type: "error", message: getCvFeedbackMessage(caughtError) });
    } finally {
      if (objectUrl) {
        URL.revokeObjectURL(objectUrl);
      }

      setIsViewingCv(false);
    }
  }

  function handleChooseCv() {
    if (isCvBusy || !profile) {
      return;
    }

    fileInputRef.current?.click();
  }

  async function handleFileChange(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0] ?? null;
    event.target.value = "";

    setFeedback(null);

    if (!file) {
      setFeedback({ type: "error", message: "No file selected." });
      return;
    }

    if (!isPdfFile(file)) {
      setFeedback({ type: "error", message: "Please choose a PDF file." });
      return;
    }

    if (file.size > maxCvSizeBytes) {
      setFeedback({ type: "error", message: "CV must be 5 MB or smaller." });
      return;
    }

    if (isUploading || !profile) {
      return;
    }

    setIsUploading(true);
    setSelectedFileName(file.name);

    try {
      await uploadMyCandidateCv(file);
      const refreshedProfile = await refreshProfile();
      setFeedback({ type: "success", message: refreshedProfile.cvFileName ? "CV uploaded successfully." : "CV updated successfully." });
    } catch (caughtError) {
      setFeedback({ type: "error", message: getCvFeedbackMessage(caughtError) });
    } finally {
      setIsUploading(false);
      setSelectedFileName("");
    }
  }

  async function handleRemoveCv() {
    if (!cvFileName || isCvBusy) {
      return;
    }

    const confirmed = window.confirm("Remove your current CV? Existing job applications will keep their submitted CV copies.");

    if (!confirmed) {
      return;
    }

    setIsDeletingCv(true);
    setFeedback(null);

    try {
      await deleteMyCandidateCv();
      await refreshProfile();
      setFeedback({ type: "success", message: "CV removed successfully." });
    } catch (caughtError) {
      setFeedback({ type: "error", message: getCvFeedbackMessage(caughtError) });
    } finally {
      setIsDeletingCv(false);
    }
  }

  function renderProfileForm(submitLabel: string) {
    return (
      <form className="form-card wide-form profile-form" onSubmit={(event) => void handleSubmitProfile(event)}>
        <div className="form-grid">
          <label>
            <span>Phone</span>
            <input
              disabled={isSaving}
              maxLength={255}
              onChange={(event) => updateField("phone", event.target.value)}
              placeholder="0611111111"
              type="tel"
              value={formValues.phone}
            />
          </label>
          <label>
            <span>Location</span>
            <input
              disabled={isSaving}
              maxLength={255}
              onChange={(event) => updateField("location", event.target.value)}
              placeholder="Meknes, Morocco"
              type="text"
              value={formValues.location}
            />
          </label>
        </div>
        <label>
          <span>Bio</span>
          <textarea
            disabled={isSaving}
            maxLength={2000}
            onChange={(event) => updateField("bio", event.target.value)}
            placeholder="Briefly describe your background, interests, and strengths."
            value={formValues.bio}
          />
        </label>
        <label>
          <span>Skills</span>
          <textarea
            disabled={isSaving}
            maxLength={1000}
            onChange={(event) => updateField("skills", event.target.value)}
            placeholder="Java, Spring Boot, PostgreSQL"
            value={formValues.skills}
          />
        </label>
        {formError ? <p className="form-error" role="alert">{formError}</p> : null}
        <div className="form-actions">
          {profile ? (
            <button className="secondary-button" disabled={isSaving} onClick={handleCancelEdit} type="button">
              <X size={16} />
              Cancel
            </button>
          ) : null}
          <button className="primary-button" disabled={isSaving} type="submit">
            {isSaving ? <LoaderCircle className="spin" size={18} /> : <Check size={18} />}
            {isSaving ? "Saving..." : submitLabel}
          </button>
        </div>
      </form>
    );
  }

  if (isLoading) {
    return <LoadingState message="Loading your profile..." />;
  }

  if (error) {
    return (
      <ErrorState
        title="Could not load profile"
        message={error}
        action={
          <button className="secondary-button" onClick={retryLoad} type="button">
            Retry
          </button>
        }
      />
    );
  }

  if (isProfileMissing) {
    return (
      <article className="detail-panel profile-setup-panel">
        <div className="detail-header">
          <div>
            <p className="eyebrow">Candidate profile</p>
            <h2>Complete your candidate profile</h2>
            <p className="section-description">
              Add your information so recruiters can understand your background and so you can attach a CV to applications.
            </p>
          </div>
        </div>

        {feedback ? (
          <p className={feedback.type === "success" ? "form-success" : "form-error"} role={getFeedbackRole(feedback.type)}>{feedback.message}</p>
        ) : null}

        {renderProfileForm("Create profile")}
      </article>
    );
  }

  if (!profile) {
    return (
      <ErrorState
        title="Could not load profile"
        message="Your candidate profile could not be loaded right now."
        action={
          <button className="secondary-button" onClick={retryLoad} type="button">
            Retry
          </button>
        }
      />
    );
  }

  return (
    <article className="detail-panel">
      <div className="detail-header profile-header">
        <div>
          <p className="eyebrow">Candidate profile</p>
          <h2>{getFullName(profile) || "Unnamed candidate"}</h2>
          <div className="meta-row">
            <span>
              <Mail size={16} />
              {profile.email}
            </span>
            <span>
              <CalendarDays size={16} />
              Created {formatDate(profile.createdAt)}
            </span>
          </div>
        </div>
        <div className="profile-header-actions">
          <div className="profile-completeness">
            <div className="profile-completeness-label">
              <span>Profile completeness</span>
              <strong>{profile.profileCompleteness}%</strong>
            </div>
            <div
              aria-label="Profile completeness"
              aria-valuemax={100}
              aria-valuemin={0}
              aria-valuenow={completeness}
              className="profile-progress-track"
              role="progressbar"
            >
              <span className="profile-progress-fill" style={{ width: `${completeness}%` }} />
            </div>
          </div>
          {!isEditing ? (
            <button className="primary-button" onClick={handleStartEdit} type="button">
              <Edit3 size={16} />
              Edit profile
            </button>
          ) : null}
        </div>
      </div>

      {feedback ? (
        <p className={feedback.type === "success" ? "form-success" : "form-error"} role={getFeedbackRole(feedback.type)}>{feedback.message}</p>
      ) : null}

      {isEditing ? (
        <section className="detail-section">
          <h3>
            <UserRound size={18} />
            Edit profile
          </h3>
          {renderProfileForm("Save profile")}
        </section>
      ) : (
        <>
          <section className="detail-section">
            <h3>
              <Phone size={18} />
              Contact
            </h3>
            <div className="detail-grid">
              <div className="info-tile">
                <Mail size={18} />
                <span>Email</span>
                <a href={`mailto:${profile.email}`}>{profile.email}</a>
              </div>
              <div className="info-tile">
                <Phone size={18} />
                <span>Phone</span>
                <strong>{profile.phone || "Not provided"}</strong>
              </div>
              <div className="info-tile">
                <MapPin size={18} />
                <span>Location</span>
                <strong>{profile.location || "Not provided"}</strong>
              </div>
            </div>
          </section>

          <section className="detail-section">
            <h3>
              <UserRound size={18} />
              About
            </h3>
            <p>{profile.bio || "No bio has been added yet."}</p>
          </section>
        </>
      )}

      <section className="detail-section">
        <h3>
          <FileText size={18} />
          CV / Resume
        </h3>
        <input
          accept="application/pdf,.pdf"
          aria-label="Choose CV PDF"
          className="visually-hidden"
          onChange={(event) => void handleFileChange(event)}
          ref={fileInputRef}
          type="file"
        />
        {cvFileName ? (
          <div className="cv-panel">
            <div className="cv-file">
              <FileText size={22} />
              <strong>{cvFileName}</strong>
            </div>
            <div className="cv-actions">
              <button className="primary-button" disabled={isCvBusy} onClick={() => void handleViewCv()} type="button">
                {isViewingCv ? <LoaderCircle className="spin" size={16} /> : <Eye size={16} />}
                View CV
              </button>
              <button className="secondary-button" disabled={isCvBusy} onClick={handleChooseCv} type="button">
                {isUploading ? <LoaderCircle className="spin" size={16} /> : <Upload size={16} />}
                {isUploading ? "Uploading..." : "Replace CV"}
              </button>
              <button className="secondary-button danger-button" disabled={isCvBusy} onClick={() => void handleRemoveCv()} type="button">
                {isDeletingCv ? <LoaderCircle className="spin" size={16} /> : <Trash2 size={16} />}
                {isDeletingCv ? "Removing..." : "Remove CV"}
              </button>
            </div>
            {selectedFileName ? <span className="saving-line">Selected {selectedFileName}</span> : null}
          </div>
        ) : (
          <div className="cv-panel">
            <p>No CV uploaded yet.</p>
            <div className="cv-actions">
              <button className="primary-button" disabled={isCvBusy} onClick={handleChooseCv} type="button">
                {isUploading ? <LoaderCircle className="spin" size={16} /> : <Upload size={16} />}
                {isUploading ? "Uploading..." : "Upload CV"}
              </button>
            </div>
            {selectedFileName ? <span className="saving-line">Selected {selectedFileName}</span> : null}
          </div>
        )}
      </section>

      {!isEditing ? (
        <section className="detail-section">
          <h3>Skills</h3>
          {skills.length > 0 ? (
            <div className="skill-chip-list">
              {skills.map((skill) => (
                <span className="skill-chip" key={skill}>
                  {skill}
                </span>
              ))}
            </div>
          ) : (
            <p>No skills have been added yet.</p>
          )}
        </section>
      ) : null}
    </article>
  );
}
