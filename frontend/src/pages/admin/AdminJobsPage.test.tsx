/** @vitest-environment jsdom */

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import {
  getAdminJobs,
  updateAdminJob,
  updateAdminJobActive
} from "../../features/admin/adminApi";
import type { AdminJob } from "../../types/admin";
import { AdminJobsPage } from "./AdminJobsPage";

vi.mock("../../features/admin/adminApi", () => ({
  getAdminJobs: vi.fn(),
  updateAdminJob: vi.fn(),
  updateAdminJobActive: vi.fn()
}));

const mockedGetAdminJobs = vi.mocked(getAdminJobs);
const mockedUpdateAdminJob = vi.mocked(updateAdminJob);
const mockedUpdateAdminJobActive = vi.mocked(updateAdminJobActive);

function makeJob(overrides: Partial<AdminJob> = {}): AdminJob {
  return {
    id: 23,
    title: "Junior Developer",
    description: "Build and maintain web applications.",
    location: "Remote",
    employmentType: "FULL_TIME",
    active: true,
    companyId: 5,
    companyName: "Acme Ltd",
    createdById: 8,
    createdByName: "Recruiter User",
    createdAt: "2026-10-10T10:00:00",
    ...overrides
  };
}

beforeEach(() => {
  mockedGetAdminJobs.mockReset();
  mockedUpdateAdminJob.mockReset();
  mockedUpdateAdminJobActive.mockReset();
});

afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
});

describe("AdminJobsPage", () => {
  it("shows a loading state while jobs are being fetched", () => {
    mockedGetAdminJobs.mockReturnValue(new Promise<AdminJob[]>(() => {}));

    render(<AdminJobsPage />);

    expect(screen.getByRole("status").textContent).toContain(
      "Loading jobs..."
    );
  });

  it("renders job details and its active status", async () => {
    mockedGetAdminJobs.mockResolvedValue([makeJob()]);

    render(<AdminJobsPage />);

    expect(await screen.findByText("Junior Developer")).toBeTruthy();
    expect(screen.getByText("Acme Ltd")).toBeTruthy();
    expect(screen.getByText("Remote")).toBeTruthy();
    expect(screen.getByText("Active")).toBeTruthy();
    expect(screen.getByText("1 total")).toBeTruthy();
  });

  it("shows an error when loading jobs fails", async () => {
    mockedGetAdminJobs.mockRejectedValue(new Error("Internal test error"));

    render(<AdminJobsPage />);

    const alert = await screen.findByRole("alert");

    expect(alert.textContent).toContain("Could not load jobs");
    expect(alert.textContent).toContain(
      "Something went wrong. Please try again."
    );
  });

  it("shows an empty state when there are no jobs", async () => {
    mockedGetAdminJobs.mockResolvedValue([]);

    render(<AdminJobsPage />);

    expect(await screen.findByText("No jobs found")).toBeTruthy();
    expect(
      screen.getByText(
        "Jobs posted across JobFlow will appear here when they exist."
      )
    ).toBeTruthy();
  });

  it("deactivates a job after confirmation and updates the interface", async () => {
    const job = makeJob();

    mockedGetAdminJobs.mockResolvedValue([job]);
    mockedUpdateAdminJobActive.mockResolvedValue({
      ...job,
      active: false
    });

    const confirmSpy = vi.spyOn(window, "confirm").mockReturnValue(true);

    render(<AdminJobsPage />);

    fireEvent.click(
      await screen.findByRole("button", { name: "Deactivate" })
    );

    expect(confirmSpy).toHaveBeenCalledWith(
      "Deactivate Junior Developer?"
    );
    expect(mockedUpdateAdminJobActive).toHaveBeenCalledWith(23, false);

    expect(
      await screen.findByText("Job deactivated successfully.")
    ).toBeTruthy();

    expect(await screen.findByText("Inactive")).toBeTruthy();
    expect(
      await screen.findByRole("button", { name: "Activate" })
    ).toBeTruthy();
  });

  it("does not deactivate a job if confirmation is cancelled", async () => {
    mockedGetAdminJobs.mockResolvedValue([makeJob()]);

    const confirmSpy = vi.spyOn(window, "confirm").mockReturnValue(false);

    render(<AdminJobsPage />);

    fireEvent.click(
      await screen.findByRole("button", { name: "Deactivate" })
    );

    expect(confirmSpy).toHaveBeenCalledTimes(1);
    expect(mockedUpdateAdminJobActive).not.toHaveBeenCalled();
    expect(screen.getByText("Active")).toBeTruthy();
  });

  it("activates an inactive job without asking for confirmation", async () => {
    const job = makeJob({ active: false });

    mockedGetAdminJobs.mockResolvedValue([job]);
    mockedUpdateAdminJobActive.mockResolvedValue({
      ...job,
      active: true
    });

    const confirmSpy = vi.spyOn(window, "confirm");

    render(<AdminJobsPage />);

    fireEvent.click(
      await screen.findByRole("button", { name: "Activate" })
    );

    expect(mockedUpdateAdminJobActive).toHaveBeenCalledWith(23, true);
    expect(confirmSpy).not.toHaveBeenCalled();

    expect(
      await screen.findByText("Job activated successfully.")
    ).toBeTruthy();

    expect(await screen.findByText("Active")).toBeTruthy();
  });

  it("saves trimmed job fields and updates the displayed job", async () => {
    const job = makeJob();

    mockedGetAdminJobs.mockResolvedValue([job]);

    const updatedJob = makeJob({
      title: "Backend Developer",
      description: "Develop backend services.",
      location: "Rabat",
      employmentType: "PART_TIME"
    });

    mockedUpdateAdminJob.mockResolvedValue(updatedJob);

    render(<AdminJobsPage />);

    await screen.findByText("Junior Developer");

    fireEvent.click(screen.getByRole("button", { name: "Edit" }));

    fireEvent.change(screen.getByLabelText("Title"), {
      target: { value: "  Backend Developer  " }
    });

    fireEvent.change(screen.getByLabelText("Description"), {
      target: { value: "  Develop backend services.  " }
    });

    fireEvent.change(screen.getByLabelText("Location"), {
      target: { value: "  Rabat  " }
    });

    fireEvent.change(screen.getByLabelText("Employment type"), {
      target: { value: "PART_TIME" }
    });

    fireEvent.click(screen.getByRole("button", { name: "Save" }));

    expect(mockedUpdateAdminJob).toHaveBeenCalledWith(23, {
      title: "Backend Developer",
      description: "Develop backend services.",
      location: "Rabat",
      employmentType: "PART_TIME"
    });

    expect(
      await screen.findByText("Job updated successfully.")
    ).toBeTruthy();

    expect(await screen.findByText("Backend Developer")).toBeTruthy();
    expect(
      await screen.findByText("Develop backend services.")
    ).toBeTruthy();
    expect(screen.queryByRole("dialog")).toBeNull();
  });

  it("shows a helpful message when saving a job fails", async () => {
    mockedGetAdminJobs.mockResolvedValue([makeJob()]);
    mockedUpdateAdminJob.mockRejectedValue(new Error("Network failure"));

    render(<AdminJobsPage />);

    await screen.findByText("Junior Developer");

    fireEvent.click(screen.getByRole("button", { name: "Edit" }));
    fireEvent.click(screen.getByRole("button", { name: "Save" }));

    expect(
      await screen.findByText("Something went wrong. Please try again.")
    ).toBeTruthy();

    expect(screen.getByRole("dialog")).toBeTruthy();
  });

  it("closes the edit dialog when Cancel is clicked", async () => {
    mockedGetAdminJobs.mockResolvedValue([makeJob()]);

    render(<AdminJobsPage />);

    await screen.findByText("Junior Developer");

    fireEvent.click(screen.getByRole("button", { name: "Edit" }));

    expect(screen.getByRole("dialog")).toBeTruthy();

    fireEvent.click(screen.getByRole("button", { name: "Cancel" }));

    expect(screen.queryByRole("dialog")).toBeNull();
  });

  it("closes the edit dialog with Escape and restores focus to the Edit button", async () => {
    mockedGetAdminJobs.mockResolvedValue([makeJob()]);

    render(<AdminJobsPage />);

    const editButton = await screen.findByRole("button", { name: "Edit" });

    fireEvent.click(editButton);

    const dialog = screen.getByRole("dialog");
    const cancelButton = screen.getByRole("button", { name: "Cancel" });

    expect(dialog).toBeTruthy();
    expect(document.activeElement).toBe(cancelButton);

    fireEvent.keyDown(document, { key: "Escape" });

    expect(screen.queryByRole("dialog")).toBeNull();
    expect(document.activeElement).toBe(editButton);
  });

  it("keeps keyboard focus inside the edit dialog when Tab reaches the last control", async () => {
    mockedGetAdminJobs.mockResolvedValue([makeJob()]);

    render(<AdminJobsPage />);

    fireEvent.click(
      await screen.findByRole("button", { name: "Edit" })
    );

    const dialog = screen.getByRole("dialog");
    const titleInput = screen.getByRole("textbox", { name: "Title" });
    const saveButton = screen.getByRole("button", { name: "Save" });

    saveButton.focus();

    expect(document.activeElement).toBe(saveButton);

    fireEvent.keyDown(document, { key: "Tab" });

    expect(document.activeElement).toBe(titleInput);
    expect(dialog.contains(document.activeElement)).toBe(true);
  });
});