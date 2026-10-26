import { apiClient } from "../../lib/apiClient";
import type { LoginRequest, LoginResponse, RegisterRequest, AuthUser } from "../../types/auth";

export function loginUser(payload: LoginRequest) {
  return apiClient<LoginResponse>("/api/auth/login", {
    method: "POST",
    authenticated: false,
    body: JSON.stringify(payload)
  });
}

export function registerUser(payload: RegisterRequest) {
  return apiClient<AuthUser>("/api/auth/register", {
    method: "POST",
    authenticated: false,
    body: JSON.stringify(payload)
  });
}
