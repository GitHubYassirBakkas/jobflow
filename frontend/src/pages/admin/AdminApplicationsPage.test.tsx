/** @vitest-environment jsdom */

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import {
  getAdminApplicationCv,
  getAdminApplications,
  updateAdminApplicationStatus
} from "../../features/admin/adminApi";
import type { AdminApplication } from "../../types/admin";
import { AdminApplicationsPage } from "./AdminApplicationsPage";

vi.mock("../../features/admin/adminApi", () => ({
  getAdminApplicationCv: vi.fn(),
  getAdminApplications: vi.fn(),
  updateAdminApplicationStatus: vi.fn()
}));

const mockedGetAdminApplications = vi.mocked(getAdminApplications);
const mockedGetAdminApplicationCv = vi.mocked(getAdminApplicationCv);
const mockedUpdateAdminApplicationStatus = vi.mocked(
  updateAdminApplicationStatus
);

function makeApplication(
  overrides: Partial<AdminApplication> = {}
): AdminApplication {
  return {
    id: 31,
    jobId: 23,
    jobTitle: "Junior Developer",
    companyName: "Acme Ltd",
    candidateId: 7,
    candidateName: "Jane Doe",
    candidateEmail: "jane@example.com",
    status: "APPLIED",
    cvFileName: "jane-resume.pdf",
    appliedAt: "2026-10-15T12:00:00",
    ...overrides
  };
}

function renderPage() {
  return render(
    <MemoryRouter>
      <AdminApplicationsPage />
    </MemoryRouter>
  );
}

beforeEach(() => {
  mockedGetAdminApplications.mockReset();
  mockedGetAdminApplicationCv.mockReset();
  mockedUpdateAdminApplicationStatus.mockReset();
});

afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
});

describe("AdminApplicationsPage", () => {
  it("shows a loading state while applications are being fetched", () => {
    mockedGetAdminApplications.mockReturnValue(
      new Promise<AdminApplication[]>(() => {})
    );

    renderPage();

    expect(screen.getByRole("status").textContent).toContain(
      "Loading applications..."
    );
  });

  it("renders applications from newest to oldest", async () => {
    mockedGetAdminApplications.mockResolvedValue([
      makeApplication({
        id: 1,
        candidateName: "Older Candidate",
        candidateEmail: "older@example.com",
        cvFileName: "older-resume.pdf",
        appliedAt: "2026-10-10T12:00:00"
      }),
      makeApplication({
        id: 2,
        candidateName: "Newer Candidate",
        candidateEmail: "jane@example.com",
        cvFileName: "jane-resume.pdf",
        appliedAt: "2026-10-18T12:00:00"
      })
    ]);

    renderPage();

    await screen.findByText("Applications");

    const cards = screen.getAllByRole("article");

    expect(cards).toHaveLength(2);
    expect(cards[0].textContent).toContain("Newer Candidate");
    expect(cards[1].textContent).toContain("Older Candidate");

    expect(screen.getByText("jane@example.com")).toBeTruthy();
    expect(screen.getByText("jane-resume.pdf")).toBeTruthy();
    expect(screen.getByText("2 total")).toBeTruthy();
  });

  it("shows an error when loading applications fails", async () => {
    mockedGetAdminApplications.mockRejectedValue(
      new Error("Internal test error")
    );

    renderPage();

    const alert = await screen.findByRole("alert");

    expect(alert.textContent).toContain("Could not load applications");
    expect(alert.textContent).toContain(
      "Something went wrong. Please try again."
    );
  });

  it("shows an empty state when no applications exist", async () => {
    mockedGetAdminApplications.mockResolvedValue([]);

    renderPage();

    expect(await screen.findByText("No applications found")).toBeTruthy();
    expect(
      screen.getByText(
        "Candidate applications across JobFlow will appear here when they exist."
      )
    ).toBeTruthy();
  });

  it("updates an application status and refreshes the displayed data", async () => {
    const application = makeApplication();

    mockedGetAdminApplications.mockResolvedValue([application]);
    mockedUpdateAdminApplicationStatus.mockResolvedValue({
      ...application,
      status: "REJECTED"
    });

    renderPage();

    await screen.findByText("Jane Doe");

    fireEvent.click(
      screen.getByRole("button", { name: "Change status" })
    );

    expect(screen.getByRole("dialog")).toBeTruthy();

    fireEvent.change(screen.getByLabelText("Status"), {
      target: { value: "REJECTED" }
    });

    fireEvent.click(screen.getByRole("button", { name: "Save" }));

    expect(mockedUpdateAdminApplicationStatus).toHaveBeenCalledWith(
      31,
      "REJECTED"
    );

    expect(
      await screen.findByText("Application status updated.")
    ).toBeTruthy();

    expect(screen.queryByRole("dialog")).toBeNull();
    expect(await screen.findByText("Rejected")).toBeTruthy();
  });

  it("shows an error and keeps the status dialog open if saving fails", async () => {
    mockedGetAdminApplications.mockResolvedValue([makeApplication()]);
    mockedUpdateAdminApplicationStatus.mockRejectedValue(
      new Error("Status update failed")
    );

    renderPage();

    await screen.findByText("Jane Doe");

    fireEvent.click(
      screen.getByRole("button", { name: "Change status" })
    );

    fireEvent.change(screen.getByLabelText("Status"), {
      target: { value: "REVIEWING" }
    });

    fireEvent.click(screen.getByRole("button", { name: "Save" }));

    expect(
      await screen.findByText("Something went wrong. Please try again.")
    ).toBeTruthy();

    expect(screen.getByRole("dialog")).toBeTruthy();
  });

  it("does not allow changing the status of a withdrawn application", async () => {
    mockedGetAdminApplications.mockResolvedValue([
      makeApplication({ status: "WITHDRAWN" })
    ]);

    renderPage();

    await screen.findByText("Jane Doe");

    expect(
      screen
        .getByRole("button", { name: "Change status" })
        .hasAttribute("disabled")
    ).toBe(true);

    expect(screen.queryByRole("dialog")).toBeNull();
  });

  it("shows when an application has no CV attached", async () => {
    mockedGetAdminApplications.mockResolvedValue([
      makeApplication({ cvFileName: null })
    ]);

    renderPage();

    expect(await screen.findByText("No CV attached")).toBeTruthy();

    expect(
      screen.queryByRole("button", { name: "View CV" })
    ).toBeNull();

    expect(
      screen.queryByRole("button", { name: "Download" })
    ).toBeNull();
  });

  it("shows a helpful message when the browser blocks CV preview", async () => {
    mockedGetAdminApplications.mockResolvedValue([makeApplication()]);

    vi.spyOn(window, "open").mockReturnValue(null);

    renderPage();

    fireEvent.click(
      await screen.findByRole("button", { name: "View CV" })
    );

    expect(
      await screen.findByText(
        "Your browser blocked opening the CV. Allow pop-ups for JobFlow and try again."
      )
    ).toBeTruthy();

    expect(mockedGetAdminApplicationCv).not.toHaveBeenCalled();
  });

  it("closes the status dialog when Cancel is clicked", async () => {
    mockedGetAdminApplications.mockResolvedValue([makeApplication()]);

    renderPage();

    await screen.findByText("Jane Doe");

    fireEvent.click(
      screen.getByRole("button", { name: "Change status" })
    );

    expect(screen.getByRole("dialog")).toBeTruthy();

    fireEvent.click(screen.getByRole("button", { name: "Cancel" }));

    expect(screen.queryByRole("dialog")).toBeNull();
    expect(mockedUpdateAdminApplicationStatus).not.toHaveBeenCalled();
  });

  it("closes the status dialog with Escape and restores focus to Change status", async () => {
    mockedGetAdminApplications.mockResolvedValue([makeApplication()]);

    renderPage();

    const changeStatusButton = await screen.findByRole("button", {
      name: "Change status"
    });

    fireEvent.click(changeStatusButton);

    const dialog = screen.getByRole("dialog");
    const cancelButton = screen.getByRole("button", {
      name: "Cancel"
    });

    expect(dialog).toBeTruthy();
    expect(document.activeElement).toBe(cancelButton);

    fireEvent.keyDown(document, { key: "Escape" });

    expect(screen.queryByRole("dialog")).toBeNull();
    expect(document.activeElement).toBe(changeStatusButton);
  });

  it("keeps keyboard focus inside the status dialog when Tab reaches the last control", async () => {
    mockedGetAdminApplications.mockResolvedValue([makeApplication()]);

    renderPage();

    fireEvent.click(
      await screen.findByRole("button", { name: "Change status" })
    );

    const dialog = screen.getByRole("dialog");
    const statusSelect = screen.getByRole("combobox", {
      name: "Status"
    });
    const saveButton = screen.getByRole("button", {
      name: "Save"
    });

    saveButton.focus();

    expect(document.activeElement).toBe(saveButton);

    fireEvent.keyDown(document, { key: "Tab" });

    expect(document.activeElement).toBe(statusSelect);
    expect(dialog.contains(document.activeElement)).toBe(true);
  });
});