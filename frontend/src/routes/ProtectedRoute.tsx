import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "../features/auth/AuthContext";
import type { UserRole } from "../types/auth";

interface ProtectedRouteProps {
  allowedRoles?: UserRole[];
}

export function ProtectedRoute({ allowedRoles }: ProtectedRouteProps) {
  const { hasExpiredSession, isAuthenticated, user } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    return (
      <Navigate
        to="/login"
        replace
        state={{ from: location, sessionExpired: hasExpiredSession }}
      />
    );
  }

  if (allowedRoles && user && !allowedRoles.includes(user.role)) {
    const fallbackPath = user.role === "ADMIN" ? "/admin" : user.role === "RECRUITER" ? "/recruiter" : "/candidate";
    return <Navigate to={fallbackPath} replace />;
  }

  return <Outlet />;
}
