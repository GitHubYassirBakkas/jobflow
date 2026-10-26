import { useEffect, useMemo, useState } from "react";
import { ArrowLeft, CalendarDays, Download, Eye, FileText, LoaderCircle, Mail, MapPin, Phone, UserRound } from "lucide-react";
import { Link, useParams } from "react-router-dom";
import { EmptyState, ErrorState, LoadingState } from "../../components/AsyncState";
import { getAdminCandidateCv, getAdminCandidateProfile } from "../../features/admin/adminApi";
import { ApiError } from "../../lib/apiClient";
import { getFriendlyErrorMessage } from "../../lib/errors";
import { formatDate } from "../../lib/formatters";
import type { AdminCandidateProfile } from "../../types/admin";

type CvAction = "view" | "download";

function getFullName(profile: AdminCandidateProfile) {
  return `${profile.firstName} ${profile.lastName}`.trim();
}

function getSkillList(skills: string | null) {
  return (skills ?? "")
    .split(",")
    .map((skill) => skill.trim())
    .filter(Boolean);
}

function getCvErrorMessage(error: unknown) {
  if (error instanceof Error && error.message === "cv-popup-blocked") {
    return "Your browser blocked opening the CV. Allow pop-ups for JobFlow and try again.";
  }

  return getFriendlyErrorMessage(error);
}

export function AdminCandidateDetailsPage() {
  const { userId } = useParams();
  const [profile, setProfile] = useState<AdminCandidateProfile | null>(null);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);
  const [cvError, setCvError] = useState("");
  const [cvAction, setCvAction] = useState<CvAction | null>(null);

  useEffect(() => {
    let isMounted = true;

    async function loadProfile() {
      if (!userId) {
        setNotFound(true);
        setIsLoading(false);
        return;
      }

      setIsLoading(true);
      setError("");
      setNotFound(false);

      try {
        const response = await getAdminCandidateProfile(userId);

        if (isMounted) {
          setProfile(response);
        }
      } catch (caughtError) {
        if (isMounted) {
          if (caughtError instanceof ApiError && caughtError.status === 404) {
            setNotFound(true);
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
  }, [userId]);

  const skills = useMemo(() => getSkillList(profile?.skills ?? null), [profile]);
  const cvFileName = profile?.cvFileName?.trim() ?? "";

  async function handleCvAction(action: CvAction) {
    if (!userId || !cvFileName || cvAction) {
      return;
    }

    let previewWindow: Window | null = null;

    if (action === "view") {
      previewWindow = window.open("", "_blank");

      if (!previewWindow) {
        setCvError(getCvErrorMessage(new Error("cv-popup-blocked")));
        return;
      }
    }

    setCvAction(action);
    setCvError("");

    let objectUrl: string | null = null;

    try {
      const blob = await getAdminCandidateCv(userId);
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

      setCvError(getCvErrorMessage(caughtError));
    } finally {
      if (objectUrl) {
        URL.revokeObjectURL(objectUrl);
      }

      setCvAction(null);
    }
  }

  if (isLoading) {
    return <LoadingState message="Loading candidate profile..." />;
  }

  if (error) {
    return <ErrorState title="Could not load candidate profile" message={error} />;
  }

  if (notFound || !profile) {
    return (
      <EmptyState
        title="Candidate profile not found"
        message="This candidate profile is not available."
        action={
          <Link className="secondary-button" to="/admin/users">
            <ArrowLeft size={16} />
            Back to Users
          </Link>
        }
      />
    );
  }

  return (
    <article className="detail-panel">
      <Link className="back-link" to="/admin/users">
        <ArrowLeft size={16} />
        Back to Users
      </Link>

      <div className="detail-header">
        <div>
          <p className="eyebrow">Candidate #{profile.userId}</p>
          <h2>{getFullName(profile) || "Unnamed candidate"}</h2>
          <div className="meta-row">
            <span className="status-badge status-neutral">Candidate</span>
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
      </div>

      <section className="detail-grid">
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
      </section>

      <section className="detail-section">
        <h3>
          <UserRound size={18} />
          Bio
        </h3>
        <p>{profile.bio || "No bio has been added yet."}</p>
      </section>

      <section className="detail-section">
        <h3>
          <FileText size={18} />
          CV / Resume
        </h3>
        {cvFileName ? (
          <div className="cv-panel">
            <div className="cv-file">
              <FileText size={22} />
              <strong>{cvFileName}</strong>
            </div>
            <div className="cv-actions">
              <button className="primary-button" disabled={cvAction !== null} onClick={() => void handleCvAction("view")} type="button">
                {cvAction === "view" ? <LoaderCircle className="spin" size={16} /> : <Eye size={16} />}
                View CV
              </button>
              <button className="secondary-button" disabled={cvAction !== null} onClick={() => void handleCvAction("download")} type="button">
                {cvAction === "download" ? <LoaderCircle className="spin" size={16} /> : <Download size={16} />}
                Download
              </button>
            </div>
            {cvError ? <p className="form-error">{cvError}</p> : null}
          </div>
        ) : (
          <p>No CV uploaded.</p>
        )}
      </section>

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
    </article>
  );
}
