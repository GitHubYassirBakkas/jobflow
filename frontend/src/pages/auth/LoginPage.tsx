import { FormEvent, useState } from "react";
import { Link, Navigate, useLocation, useNavigate } from "react-router-dom";
import { LoaderCircle } from "lucide-react";
import { useAuth } from "../../features/auth/AuthContext";
import type { UserRole } from "../../types/auth";

function homeForRole(role: UserRole) {
  if (role === "ADMIN") {
    return "/admin";
  }

  return role === "RECRUITER" ? "/recruiter" : "/candidate";
}

interface LoginLocationState {
  from?: {
    pathname?: string;
  };
  sessionExpired?: boolean;
}

export function getRequestedPathFromLoginState(state: unknown) {
  const locationState = state as LoginLocationState | null;

  return locationState?.from?.pathname;
}

export function getSessionExpiryMessage(state: unknown) {
  const locationState = state as LoginLocationState | null;

  return locationState?.sessionExpired
    ? "Your session expired. Please sign in again."
    : "";
}

export function LoginPage() {
  const { isAuthenticated, login, user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const sessionExpiryMessage = getSessionExpiryMessage(location.state);

  if (isAuthenticated && user) {
    return <Navigate to={homeForRole(user.role)} replace />;
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setIsSubmitting(true);

    try {
      const loggedInUser = await login({ email, password });
      const requestedPath = getRequestedPathFromLoginState(location.state);
      navigate(requestedPath ?? homeForRole(loggedInUser.role), { replace: true });
    } catch (caughtError) {
      setError(caughtError instanceof Error ? caughtError.message : "Unable to sign in.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="form-stack">
      <div>
        <p className="eyebrow">Welcome back</p>
        <h2>Sign in to JobFlow</h2>
      </div>
      <form onSubmit={handleSubmit} className="form-card">
        <label>
          <span>Email</span>
          <input
            autoComplete="email"
            name="email"
            onChange={(event) => setEmail(event.target.value)}
            placeholder="you@example.com"
            required
            type="email"
            value={email}
          />
        </label>
        <label>
          <span>Password</span>
          <input
            autoComplete="current-password"
            name="password"
            onChange={(event) => setPassword(event.target.value)}
            placeholder="Enter your password"
            required
            type="password"
            value={password}
          />
        </label>
        {sessionExpiryMessage ? (
          <p className="form-error" role="alert">{sessionExpiryMessage}</p>
        ) : null}
        {error ? <p className="form-error">{error}</p> : null}
        <button className="primary-button" disabled={isSubmitting} type="submit">
          {isSubmitting ? <LoaderCircle className="spin" size={18} /> : null}
          <span>{isSubmitting ? "Signing in" : "Sign in"}</span>
        </button>
      </form>
      <p className="switch-link">
        New to JobFlow? <Link to="/register">Create an account</Link>
      </p>
    </div>
  );
}
