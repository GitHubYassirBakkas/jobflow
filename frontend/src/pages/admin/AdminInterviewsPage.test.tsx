/** @vitest-environment jsdom */

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import {
  getAdminInterviews,
  updateAdminInterviewStatus
} from "../../features/admin/adminApi";
import type { AdminInterview } from "../../types/admin";
import { AdminInterviewsPage } from "./AdminInterviewsPage";

vi.mock("../../features/admin/adminApi", () => ({
  getAdminInterviews: vi.fn(),
  updateAdminInterviewStatus: vi.fn()
}));

const mockedGetAdminInterviews = vi.mocked(getAdminInterviews);
const mockedUpdateAdminInterviewStatus = vi.mocked(
  updateAdminInterviewStatus
);

function makeInterview(
  overrides: Partial<AdminInterview> = {}
): AdminInterview {
  return {
    id: 54,
    applicationId: 31,
    jobId: 23,
    jobTitle: "Junior Developer",
    candidateId: 7,
    candidateName: "Jane Doe",
    candidateEmail: "jane@example.com",
    scheduledAt: "2026-10-20T10:00:00",
    type: "ONLINE",
    meetingLink: "https://meet.example.com/room",
    location: null,
    notes: "Bring your portfolio",
    status: "SCHEDULED",
    ...overrides
  };
}

function renderPage() {
  return render(
    <MemoryRouter>
      <AdminInterviewsPage />
    </MemoryRouter>
  );
}

beforeEach(() => {
  mockedGetAdminInterviews.mockReset();
  mockedUpdateAdminInterviewStatus.mockReset();
});

afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
});

describe("AdminInterviewsPage", () => {
  it("shows a loading state while interviews are being fetched", () => {
    mockedGetAdminInterviews.mockReturnValue(
      new Promise<AdminInterview[]>(() => {})
    );

    renderPage();

    expect(screen.getByRole("status").textContent).toContain(
      "Loading interviews..."
    );
  });

  it("renders interviews from newest to oldest", async () => {
    mockedGetAdminInterviews.mockResolvedValue([
      makeInterview({
        id: 1,
        candidateName: "Older Candidate",
        candidateEmail: "older@example.com",
        scheduledAt: "2026-10-10T10:00:00"
      }),
      makeInterview({
        id: 2,
        candidateName: "Newer Candidate",
        candidateEmail: "newer@example.com",
        scheduledAt: "2026-10-25T10:00:00"
      })
    ]);

    renderPage();

    await screen.findByText("Interviews");

    const cards = screen.getAllByRole("article");

    expect(cards).toHaveLength(2);
    expect(cards[0].textContent).toContain("Newer Candidate");
    expect(cards[1].textContent).toContain("Older Candidate");
    expect(screen.getByText("2 total")).toBeTruthy();
  });

  it("renders meeting links and optional interview details", async () => {
    mockedGetAdminInterviews.mockResolvedValue([
      makeInterview({
        location: "Meknes",
        notes: "Bring your portfolio"
      })
    ]);

    renderPage();

    expect(await screen.findByText("Jane Doe")).toBeTruthy();
    expect(screen.getByText("Meknes")).toBeTruthy();
    expect(screen.getByText("Bring your portfolio")).toBeTruthy();

    expect(
      screen.getByRole("link", { name: /open meeting/i }).getAttribute("href")
    ).toBe("https://meet.example.com/room");
  });

  it("does not render optional details when they are missing", async () => {
    mockedGetAdminInterviews.mockResolvedValue([
      makeInterview({
        meetingLink: null,
        location: null,
        notes: null
      })
    ]);

    renderPage();

    expect(await screen.findByText("Jane Doe")).toBeTruthy();
    expect(screen.queryByRole("link", { name: /open meeting/i })).toBeNull();
  });

  it("shows an error when loading interviews fails", async () => {
    mockedGetAdminInterviews.mockRejectedValue(
      new Error("Internal test error")
    );

    renderPage();

    const alert = await screen.findByRole("alert");

    expect(alert.textContent).toContain("Could not load interviews");
    expect(alert.textContent).toContain(
      "Something went wrong. Please try again."
    );
  });

  it("shows an empty state when no interviews exist", async () => {
    mockedGetAdminInterviews.mockResolvedValue([]);

    renderPage();

    expect(await screen.findByText("No interviews found")).toBeTruthy();
    expect(
      screen.getByText(
        "Scheduled interviews across JobFlow will appear here when they exist."
      )
    ).toBeTruthy();
  });

  it("updates an interview status and refreshes the interface", async () => {
    const interview = makeInterview();

    mockedGetAdminInterviews.mockResolvedValue([interview]);
    mockedUpdateAdminInterviewStatus.mockResolvedValue({
      ...interview,
      status: "COMPLETED"
    });

    renderPage();

    await screen.findByText("Jane Doe");

    fireEvent.click(
      screen.getByRole("button", { name: "Change status" })
    );

    expect(screen.getByRole("dialog")).toBeTruthy();

    fireEvent.change(screen.getByLabelText("Status"), {
      target: { value: "COMPLETED" }
    });

    fireEvent.click(screen.getByRole("button", { name: "Save" }));

    expect(mockedUpdateAdminInterviewStatus).toHaveBeenCalledWith(
      54,
      "COMPLETED"
    );

    expect(
      await screen.findByText("Interview status updated.")
    ).toBeTruthy();

    expect(screen.queryByRole("dialog")).toBeNull();
    expect(await screen.findByText("Completed")).toBeTruthy();
  });

  it("shows an error and keeps the status dialog open if saving fails", async () => {
    mockedGetAdminInterviews.mockResolvedValue([makeInterview()]);
    mockedUpdateAdminInterviewStatus.mockRejectedValue(
      new Error("Interview update failed")
    );

    renderPage();

    await screen.findByText("Jane Doe");

    fireEvent.click(
      screen.getByRole("button", { name: "Change status" })
    );

    fireEvent.change(screen.getByLabelText("Status"), {
      target: { value: "CANCELLED" }
    });

    fireEvent.click(screen.getByRole("button", { name: "Save" }));

    expect(
      await screen.findByText("Something went wrong. Please try again.")
    ).toBeTruthy();

    expect(screen.getByRole("dialog")).toBeTruthy();
  });

  it("closes the status dialog when Cancel is clicked", async () => {
    mockedGetAdminInterviews.mockResolvedValue([makeInterview()]);

    renderPage();

    await screen.findByText("Jane Doe");

    fireEvent.click(
      screen.getByRole("button", { name: "Change status" })
    );

    expect(screen.getByRole("dialog")).toBeTruthy();

    fireEvent.click(screen.getByRole("button", { name: "Cancel" }));

    expect(screen.queryByRole("dialog")).toBeNull();
    expect(mockedUpdateAdminInterviewStatus).not.toHaveBeenCalled();
  });

  it("closes the status dialog with Escape and restores focus to Change status", async () => {
    mockedGetAdminInterviews.mockResolvedValue([makeInterview()]);

    renderPage();

    const changeStatusButton = await screen.findByRole("button", {
      name: "Change status"
    });

    fireEvent.click(changeStatusButton);

    const dialog = screen.getByRole("dialog");
    const cancelButton = screen.getByRole("button", { name: "Cancel" });

    expect(dialog).toBeTruthy();
    expect(document.activeElement).toBe(cancelButton);

    fireEvent.keyDown(document, { key: "Escape" });

    expect(screen.queryByRole("dialog")).toBeNull();
    expect(document.activeElement).toBe(changeStatusButton);
  });

  it("keeps keyboard focus inside the status dialog when Tab reaches the last control", async () => {
    mockedGetAdminInterviews.mockResolvedValue([makeInterview()]);

    renderPage();

    fireEvent.click(
      await screen.findByRole("button", { name: "Change status" })
    );

    const dialog = screen.getByRole("dialog");
    const statusSelect = screen.getByRole("combobox", { name: "Status" });
    const saveButton = screen.getByRole("button", { name: "Save" });

    saveButton.focus();

    expect(document.activeElement).toBe(saveButton);

    fireEvent.keyDown(document, { key: "Tab" });

    expect(document.activeElement).toBe(statusSelect);
    expect(dialog.contains(document.activeElement)).toBe(true);
  });
});