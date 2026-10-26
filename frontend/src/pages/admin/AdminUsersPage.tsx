import { useEffect, useState } from "react";
import { ArrowRight, Mail, UserRound } from "lucide-react";
import { Link } from "react-router-dom";
import { EmptyState, ErrorState, LoadingState } from "../../components/AsyncState";
import { getAdminUsers, updateAdminUserEnabled } from "../../features/admin/adminApi";
import { ApiError } from "../../lib/apiClient";
import { getFriendlyErrorMessage } from "../../lib/errors";
import { formatDate, formatEnumLabel } from "../../lib/formatters";
import type { AdminUser } from "../../types/admin";

const roleTone: Record<AdminUser["role"], string> = {
  CANDIDATE: "status-neutral",
  RECRUITER: "status-info",
  ADMIN: "status-success"
};

function getFullName(user: AdminUser) {
  return `${user.firstName} ${user.lastName}`.trim();
}

export function AdminUsersPage() {
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [error, setError] = useState("");
  const [feedback, setFeedback] = useState<{ type: "success" | "error"; message: string } | null>(null);
  const [savingUserId, setSavingUserId] = useState<number | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let isMounted = true;

    async function loadUsers() {
      setIsLoading(true);
      setError("");

      try {
        const response = await getAdminUsers();

        if (isMounted) {
          setUsers(response);
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

    void loadUsers();

    return () => {
      isMounted = false;
    };
  }, []);

  async function handleToggleEnabled(user: AdminUser) {
    const nextEnabled = !user.enabled;

    if (!nextEnabled) {
      const confirmed = window.confirm(`Disable ${getFullName(user) || user.email}?`);

      if (!confirmed) {
        return;
      }
    }

    setSavingUserId(user.id);
    setFeedback(null);

    try {
      const updatedUser = await updateAdminUserEnabled(user.id, nextEnabled);
      setUsers((currentUsers) =>
        currentUsers.map((currentUser) => (currentUser.id === updatedUser.id ? updatedUser : currentUser))
      );
      setFeedback({ type: "success", message: `User ${updatedUser.enabled ? "enabled" : "disabled"} successfully.` });
    } catch (caughtError) {
      const message = caughtError instanceof ApiError ? caughtError.message : getFriendlyErrorMessage(caughtError);
      setFeedback({ type: "error", message });
    } finally {
      setSavingUserId(null);
    }
  }

  if (isLoading) {
    return <LoadingState message="Loading users..." />;
  }

  if (error) {
    return <ErrorState title="Could not load users" message={error} />;
  }

  if (users.length === 0) {
    return (
      <EmptyState
        title="No users found"
        message="Registered JobFlow users will appear here when they exist."
      />
    );
  }

  return (
    <div className="page-stack">
      <section className="section-heading">
        <div>
          <p className="eyebrow">Admin</p>
          <h2>Users</h2>
          <p className="section-description">Review registered accounts and role distribution across JobFlow.</p>
        </div>
        <span>{users.length} total</span>
      </section>

      {feedback ? (
        <p className={feedback.type === "success" ? "form-success" : "form-error"}>{feedback.message}</p>
      ) : null}

      <section className="list-stack">
        {users.map((user) => {
          const isSaving = savingUserId === user.id;

          return (
            <article className="list-card admin-user-card" key={user.id}>
              <div className="user-card-main">
                <div className="card-icon">
                  <UserRound size={20} />
                </div>
                <div>
                  <p className="eyebrow">User #{user.id}</p>
                  <h2>{getFullName(user) || "Unnamed user"}</h2>
                  <p className="muted-line">
                    <Mail size={16} />
                    {user.email}
                  </p>
                </div>
              </div>
              <div className="list-card-actions">
                <span className={`status-badge ${roleTone[user.role]}`}>{formatEnumLabel(user.role)}</span>
                <span className={`status-badge ${user.enabled ? "status-success" : "status-danger"}`}>
                  {user.enabled ? "Active" : "Disabled"}
                </span>
                <button
                  className="secondary-button"
                  disabled={savingUserId !== null}
                  onClick={() => void handleToggleEnabled(user)}
                  type="button"
                >
                  {isSaving ? (user.enabled ? "Disabling..." : "Enabling...") : user.enabled ? "Disable" : "Enable"}
                </button>
                {user.role === "CANDIDATE" ? (
                  <Link className="secondary-button" to={`/admin/candidates/${user.id}`}>
                    View profile <ArrowRight size={16} />
                  </Link>
                ) : null}
                <span className="saving-line">Created {formatDate(user.createdAt)}</span>
              </div>
            </article>
          );
        })}
      </section>
    </div>
  );
}
