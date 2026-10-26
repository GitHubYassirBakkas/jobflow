import { FormEvent, useState } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import { LoaderCircle } from "lucide-react";
import { useAuth } from "../../features/auth/AuthContext";
import type { UserRole } from "../../types/auth";

function homeForRole(role: UserRole) {
  if (role === "ADMIN") {
    return "/admin";
  }

  return role === "RECRUITER" ? "/recruiter" : "/candidate";
}

export function RegisterPage() {
  const { isAuthenticated, register, user } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({
    firstName: "",
    lastName: "",
    email: "",
    password: ""
  });
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  if (isAuthenticated && user) {
    return <Navigate to={homeForRole(user.role)} replace />;
  }

  function updateField(field: keyof typeof form, value: string) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setSuccess("");
    setIsSubmitting(true);

    try {
      await register(form);
      setSuccess("Account created. You can sign in now.");
      navigate("/login", { replace: true });
    } catch (caughtError) {
      setError(caughtError instanceof Error ? caughtError.message : "Unable to create account.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="form-stack">
      <div>
        <p className="eyebrow">Get started</p>
        <h2>Create a candidate account</h2>
      </div>
      <form onSubmit={handleSubmit} className="form-card">
        <div className="form-grid">
          <label>
            <span>First name</span>
            <input
              autoComplete="given-name"
              name="firstName"
              onChange={(event) => updateField("firstName", event.target.value)}
              required
              type="text"
              value={form.firstName}
            />
          </label>
          <label>
            <span>Last name</span>
            <input
              autoComplete="family-name"
              name="lastName"
              onChange={(event) => updateField("lastName", event.target.value)}
              required
              type="text"
              value={form.lastName}
            />
          </label>
        </div>
        <label>
          <span>Email</span>
          <input
            autoComplete="email"
            name="email"
            onChange={(event) => updateField("email", event.target.value)}
            placeholder="you@example.com"
            required
            type="email"
            value={form.email}
          />
        </label>
        <label>
          <span>Password</span>
          <input
            autoComplete="new-password"
            minLength={8}
            name="password"
            onChange={(event) => updateField("password", event.target.value)}
            placeholder="At least 8 characters"
            required
            type="password"
            value={form.password}
          />
        </label>
        {error ? <p className="form-error">{error}</p> : null}
        {success ? <p className="form-success">{success}</p> : null}
        <button className="primary-button" disabled={isSubmitting} type="submit">
          {isSubmitting ? <LoaderCircle className="spin" size={18} /> : null}
          <span>{isSubmitting ? "Creating account" : "Create account"}</span>
        </button>
      </form>
      <p className="switch-link">
        Already have an account? <Link to="/login">Sign in</Link>
      </p>
    </div>
  );
}
