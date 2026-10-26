/** @vitest-environment jsdom */

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import {
  getAdminUsers,
  updateAdminUserEnabled
} from "../../features/admin/adminApi";
import type { AdminUser } from "../../types/admin";
import { AdminUsersPage } from "./AdminUsersPage";

vi.mock("../../features/admin/adminApi", () => ({
  getAdminUsers: vi.fn(),
  updateAdminUserEnabled: vi.fn()
}));

const mockedGetAdminUsers = vi.mocked(getAdminUsers);
const mockedUpdateAdminUserEnabled = vi.mocked(updateAdminUserEnabled);

function makeUser(overrides: Partial<AdminUser> = {}): AdminUser {
  return {
    id: 7,
    firstName: "Jane",
    lastName: "Doe",
    email: "jane@example.com",
    role: "CANDIDATE",
    enabled: true,
    createdAt: "2026-10-10T10:00:00",
    ...overrides
  };
}

function renderPage() {
  return render(
    <MemoryRouter>
      <AdminUsersPage />
    </MemoryRouter>
  );
}

beforeEach(() => {
  mockedGetAdminUsers.mockReset();
  mockedUpdateAdminUserEnabled.mockReset();
});

afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
});

describe("AdminUsersPage", () => {
  it("shows a loading state while users are being fetched", () => {
    mockedGetAdminUsers.mockReturnValue(
      new Promise<AdminUser[]>(() => {})
    );

    renderPage();

    expect(screen.getByRole("status").textContent).toContain(
      "Loading users..."
    );
  });

  it("renders users and the candidate profile link", async () => {
    mockedGetAdminUsers.mockResolvedValue([makeUser()]);

    renderPage();

    expect(await screen.findByText("Jane Doe")).toBeTruthy();
    expect(screen.getByText("jane@example.com")).toBeTruthy();
    expect(screen.getByText("Active")).toBeTruthy();

    expect(
      screen.getByRole("link", { name: /view profile/i }).getAttribute("href")
    ).toBe("/admin/candidates/7");

    expect(screen.getByText("1 total")).toBeTruthy();
  });

  it("shows an accessible error when fetching users fails", async () => {
    mockedGetAdminUsers.mockRejectedValue(
      new Error("Internal test error")
    );

    renderPage();

    const alert = await screen.findByRole("alert");

    expect(alert.textContent).toContain("Could not load users");
    expect(alert.textContent).toContain(
      "Something went wrong. Please try again."
    );
  });

  it("shows an empty state when there are no users", async () => {
    mockedGetAdminUsers.mockResolvedValue([]);

    renderPage();

    expect(await screen.findByText("No users found")).toBeTruthy();
    expect(
      screen.getByText(
        "Registered JobFlow users will appear here when they exist."
      )
    ).toBeTruthy();
  });

  it("disables a user after confirmation and updates the interface", async () => {
    const user = makeUser();

    mockedGetAdminUsers.mockResolvedValue([user]);
    mockedUpdateAdminUserEnabled.mockResolvedValue({
      ...user,
      enabled: false
    });

    const confirmSpy = vi.spyOn(window, "confirm").mockReturnValue(true);

    renderPage();

    const disableButton = await screen.findByRole("button", {
      name: "Disable"
    });

    fireEvent.click(disableButton);

    expect(confirmSpy).toHaveBeenCalledWith("Disable Jane Doe?");
    expect(mockedUpdateAdminUserEnabled).toHaveBeenCalledWith(7, false);

    expect(
      await screen.findByText("User disabled successfully.")
    ).toBeTruthy();

    expect(await screen.findByText("Disabled")).toBeTruthy();
    expect(
      await screen.findByRole("button", { name: "Enable" })
    ).toBeTruthy();
  });

  it("does not disable a user when confirmation is cancelled", async () => {
    mockedGetAdminUsers.mockResolvedValue([makeUser()]);

    const confirmSpy = vi.spyOn(window, "confirm").mockReturnValue(false);

    renderPage();

    fireEvent.click(
      await screen.findByRole("button", { name: "Disable" })
    );

    expect(confirmSpy).toHaveBeenCalledTimes(1);
    expect(mockedUpdateAdminUserEnabled).not.toHaveBeenCalled();
    expect(screen.getByText("Active")).toBeTruthy();
  });

  it("enables a disabled user without asking for confirmation", async () => {
    const user = makeUser({ enabled: false });

    mockedGetAdminUsers.mockResolvedValue([user]);
    mockedUpdateAdminUserEnabled.mockResolvedValue({
      ...user,
      enabled: true
    });

    const confirmSpy = vi.spyOn(window, "confirm");

    renderPage();

    fireEvent.click(
      await screen.findByRole("button", { name: "Enable" })
    );

    expect(mockedUpdateAdminUserEnabled).toHaveBeenCalledWith(7, true);
    expect(confirmSpy).not.toHaveBeenCalled();

    expect(
      await screen.findByText("User enabled successfully.")
    ).toBeTruthy();

    expect(await screen.findByText("Active")).toBeTruthy();
    expect(
      await screen.findByRole("button", { name: "Disable" })
    ).toBeTruthy();
  });
});