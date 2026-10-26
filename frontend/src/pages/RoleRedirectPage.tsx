import { Navigate } from "react-router-dom";

import { useAuth } from "../features/auth/AuthContext";

export function RoleRedirectPage() {
  const { isAuthenticated, user } = useAuth();

  if (!isAuthenticated || !user) {
    return <Navigate to="/login" replace />;
  }

  if (user.role === "ADMIN") {
    return <Navigate to="/admin" replace />;
  }

  if (user.role === "RECRUITER") {
    return <Navigate to="/recruiter" replace />;
  }

  return <Navigate to="/candidate" replace />;
}