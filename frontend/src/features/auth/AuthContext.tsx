import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import {
  clearStoredSession,
  loadStoredSession,
  SESSION_EXPIRED_EVENT,
  storeSession
} from "../../lib/authStorage";
import type { AuthSession, AuthUser, LoginRequest, RegisterRequest } from "../../types/auth";
import { loginUser, registerUser } from "./authApi";

interface AuthContextValue {
  user: AuthUser | null;
  token: string | null;
  isAuthenticated: boolean;
  hasExpiredSession: boolean;
  login: (payload: LoginRequest) => Promise<AuthUser>;
  register: (payload: RegisterRequest) => Promise<AuthUser>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [session, setSession] = useState<AuthSession | null>(() => loadStoredSession());
  const [hasExpiredSession, setHasExpiredSession] = useState(false);

  useEffect(() => {
    function handleSessionExpired() {
      setSession(null);
      setHasExpiredSession(true);
    }

    window.addEventListener(SESSION_EXPIRED_EVENT, handleSessionExpired);

    return () => {
      window.removeEventListener(SESSION_EXPIRED_EVENT, handleSessionExpired);
    };
  }, []);

  const login = useCallback(async (payload: LoginRequest) => {
    const response = await loginUser(payload);
    const nextSession = { token: response.token, user: response.user };

    storeSession(nextSession);
    setSession(nextSession);
    setHasExpiredSession(false);

    return response.user;
  }, []);

  const register = useCallback(async (payload: RegisterRequest) => {
    return registerUser(payload);
  }, []);

  const logout = useCallback(() => {
    clearStoredSession();
    setSession(null);
    setHasExpiredSession(false);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user: session?.user ?? null,
      token: session?.token ?? null,
      isAuthenticated: Boolean(session?.token),
      hasExpiredSession,
      login,
      register,
      logout
    }),
    [hasExpiredSession, login, logout, register, session]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }

  return context;
}
