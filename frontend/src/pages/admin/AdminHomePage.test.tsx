/** @vitest-environment jsdom */

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { cleanup, render, screen } from "@testing-library/react";
import type { AdminDashboardStats } from "../../types/admin";
import { getAdminDashboard } from "../../features/admin/adminApi";
import { AdminHomePage, buildStats } from "./AdminHomePage";

vi.mock("../../features/admin/adminApi", () => ({
  getAdminDashboard: vi.fn()
}));

const mockedGetAdminDashboard = vi.mocked(getAdminDashboard);

function makeStats(
  overrides: Partial<AdminDashboardStats> = {}
): AdminDashboardStats {
  return {
    totalUsers: 100,
    totalCandidates: 70,
    totalRecruiters: 20,
    totalAdmins: 2,
    totalCompanies: 15,
    totalJobs: 30,
    totalApplications: 80,
    ...overrides
  };
}

afterEach(() => {
  cleanup();
});

beforeEach(() => {
  mockedGetAdminDashboard.mockReset();
});

describe("AdminHomePage dashboard stats", () => {
  it("maps all backend counts to the seven dashboard cards", () => {
    const cards = buildStats(makeStats());

    expect(cards.map(({ label, value }) => [label, value])).toEqual([
      ["Total Users", 100],
      ["Candidates", 70],
      ["Recruiters", 20],
      ["Admins", 2],
      ["Companies", 15],
      ["Jobs", 30],
      ["Applications", 80]
    ]);
  });

  it("preserves zero counts instead of hiding them", () => {
    const cards = buildStats(
      makeStats({
        totalUsers: 0,
        totalCandidates: 0,
        totalRecruiters: 0,
        totalAdmins: 0,
        totalCompanies: 0,
        totalJobs: 0,
        totalApplications: 0
      })
    );

    expect(cards).toHaveLength(7);
    expect(cards.every((card) => card.value === 0)).toBe(true);
  });

  it("provides a helper description for every card", () => {
    const cards = buildStats(makeStats());

    expect(cards).toHaveLength(7);
    expect(cards.every((card) => card.helper.trim().length > 0)).toBe(true);
  });
});

describe("AdminHomePage component", () => {
  it("shows a loading state while the dashboard request is pending", () => {
    mockedGetAdminDashboard.mockReturnValue(
      new Promise<AdminDashboardStats>(() => {})
    );

    render(<AdminHomePage />);

    expect(screen.getByRole("status").textContent).toContain(
      "Loading admin dashboard..."
    );
  });

  it("renders all seven dashboard cards after a successful request", async () => {
    mockedGetAdminDashboard.mockResolvedValue(makeStats());

    render(<AdminHomePage />);

    expect(
      await screen.findByText("Platform health at a glance")
    ).toBeTruthy();

    expect(screen.getAllByRole("article")).toHaveLength(7);
    expect(screen.getByText("Total Users")).toBeTruthy();
    expect(screen.getByText("Candidates")).toBeTruthy();
    expect(screen.getByText("Recruiters")).toBeTruthy();
    expect(screen.getByText("Admins")).toBeTruthy();
    expect(screen.getByText("Companies")).toBeTruthy();
    expect(screen.getByText("Jobs")).toBeTruthy();
    expect(screen.getByText("Applications")).toBeTruthy();

    expect(screen.getByText("100")).toBeTruthy();
    expect(screen.getByText("80")).toBeTruthy();
  });

  it("shows an accessible error when the dashboard request fails", async () => {
    mockedGetAdminDashboard.mockRejectedValue(
      new Error("Internal test error")
    );

    render(<AdminHomePage />);

    const alert = await screen.findByRole("alert");

    expect(alert.textContent).toContain(
      "Could not load admin dashboard"
    );
    expect(alert.textContent).toContain(
      "Something went wrong. Please try again."
    );
  });
});