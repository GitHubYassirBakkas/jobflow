import type { AuthSession } from "../types/auth";

const STORAGE_KEY = "jobflow.auth.session";
export const SESSION_EXPIRED_EVENT = "jobflow:session-expired";

export function loadStoredSession(): AuthSession | null {
  try {
    const value = window.localStorage.getItem(STORAGE_KEY);

    if (!value) {
      return null;
    }

    const parsed = JSON.parse(value) as Partial<AuthSession>;

    if (!parsed.token || !parsed.user?.email || !parsed.user.role) {
      clearStoredSession();
      return null;
    }

    return parsed as AuthSession;
  } catch {
    clearStoredSession();
    return null;
  }
}

export function storeSession(session: AuthSession) {
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
}

export function clearStoredSession() {
  window.localStorage.removeItem(STORAGE_KEY);
}

export function expireStoredSession() {
  clearStoredSession();
  window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT));
}
