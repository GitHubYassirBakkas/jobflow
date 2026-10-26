/** @vitest-environment jsdom */

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import {
  getAdminCandidateCv,
  getAdminCandidateProfile
} from "../../features/admin/adminApi";
import type { AdminCandidateProfile } from "../../types/admin";
import { AdminCandidateDetailsPage } from "./AdminCandidateDetailsPage";

vi.mock("../../features/admin/adminApi", () => ({
  getAdminCandidateCv: vi.fn(),
  getAdminCandidateProfile: vi.fn()
}));

const mockedGetAdminCandidateProfile = vi.mocked(
  getAdminCandidateProfile
);
const mockedGetAdminCandidateCv = vi.mocked(getAdminCandidateCv);

function makeProfile(
  overrides: Partial<AdminCandidateProfile> = {}
): AdminCandidateProfile {
  return {
    id: 15,
    userId: 7,
    firstName: "Jane",
    lastName: "Doe",
    email: "jane@example.com",
    phone: "+212600000000",
    location: "Meknes",
    bio: "Junior developer interested in web applications.",
    skills: "Java, React, , SQL ",
    cvFileName: "jane-resume.pdf",
    createdAt: "2026-10-10T10:00:00",
    profileCompleteness: 85,
    ...overrides
  };
}

function renderPage(path = "/admin/candidates/7") {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route
          path="/admin/candidates/:userId"
          element={<AdminCandidateDetailsPage />}
        />
      </Routes>
    </MemoryRouter>
  );
}

beforeEach(() => {
  mockedGetAdminCandidateProfile.mockReset();
  mockedGetAdminCandidateCv.mockReset();
});

afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
});

describe("AdminCandidateDetailsPage", () => {
  it("shows a loading state while the candidate profile is being fetched", () => {
    mockedGetAdminCandidateProfile.mockReturnValue(
      new Promise<AdminCandidateProfile>(() => {})
    );

    renderPage();

    expect(screen.getByRole("status").textContent).toContain(
      "Loading candidate profile..."
    );

    expect(mockedGetAdminCandidateProfile).toHaveBeenCalledWith("7");
  });

  it("renders profile information and separates skills into individual items", async () => {
    mockedGetAdminCandidateProfile.mockResolvedValue(makeProfile());

    renderPage();

    expect(await screen.findByRole("heading", { name: "Jane Doe" })).toBeTruthy();
   expect(screen.getAllByText("jane@example.com")).toHaveLength(2);
    expect(screen.getByText("+212600000000")).toBeTruthy();
    expect(screen.getByText("Meknes")).toBeTruthy();
    expect(
      screen.getByText("Junior developer interested in web applications.")
    ).toBeTruthy();

    expect(screen.getByText("Java")).toBeTruthy();
    expect(screen.getByText("React")).toBeTruthy();
    expect(screen.getByText("SQL")).toBeTruthy();

    expect(screen.getByText("jane-resume.pdf")).toBeTruthy();

    expect(
      screen.getByRole("link", { name: "Back to Users" }).getAttribute("href")
    ).toBe("/admin/users");

    expect(
      screen.getByRole("link", { name: "jane@example.com" }).getAttribute("href")
    ).toBe("mailto:jane@example.com");
  });

  it("shows an error when loading the profile fails", async () => {
    mockedGetAdminCandidateProfile.mockRejectedValue(
      new Error("Internal test error")
    );

    renderPage();

    const alert = await screen.findByRole("alert");

    expect(alert.textContent).toContain(
      "Could not load candidate profile"
    );
    expect(alert.textContent).toContain(
      "Something went wrong. Please try again."
    );
  });

  it("shows fallback text when optional profile information is missing", async () => {
    mockedGetAdminCandidateProfile.mockResolvedValue(
      makeProfile({
        phone: null,
        location: null,
        bio: null,
        skills: null,
        cvFileName: null
      })
    );

    renderPage();

    expect(await screen.findByRole("heading", { name: "Jane Doe" })).toBeTruthy();

    expect(screen.getAllByText("Not provided")).toHaveLength(2);
    expect(screen.getByText("No bio has been added yet.")).toBeTruthy();
    expect(screen.getByText("No skills have been added yet.")).toBeTruthy();
    expect(screen.getByText("No CV uploaded.")).toBeTruthy();

    expect(
      screen.queryByRole("button", { name: "View CV" })
    ).toBeNull();
    expect(
      screen.queryByRole("button", { name: "Download" })
    ).toBeNull();
  });

  it("shows a fallback name when first and last name are empty", async () => {
  mockedGetAdminCandidateProfile.mockResolvedValue(
    makeProfile({
      firstName: "",
      lastName: ""
    })
  );

  renderPage();

  expect(
    await screen.findByRole("heading", { name: "Unnamed candidate" })
  ).toBeTruthy();
});

  it("shows a helpful message when the browser blocks CV preview", async () => {
    mockedGetAdminCandidateProfile.mockResolvedValue(makeProfile());

    const openSpy = vi.spyOn(window, "open").mockReturnValue(null);

    renderPage();

    fireEvent.click(
      await screen.findByRole("button", { name: "View CV" })
    );

    expect(openSpy).toHaveBeenCalledWith("", "_blank");

    expect(
      await screen.findByText(
        "Your browser blocked opening the CV. Allow pop-ups for JobFlow and try again."
      )
    ).toBeTruthy();

    expect(mockedGetAdminCandidateCv).not.toHaveBeenCalled();
  });

  it("shows an error when downloading the CV fails", async () => {
    mockedGetAdminCandidateProfile.mockResolvedValue(makeProfile());
    mockedGetAdminCandidateCv.mockRejectedValue(
      new Error("Network failure")
    );

    renderPage();

    fireEvent.click(
      await screen.findByRole("button", { name: "Download" })
    );

    expect(
      await screen.findByText("Something went wrong. Please try again.")
    ).toBeTruthy();

    expect(mockedGetAdminCandidateCv).toHaveBeenCalledWith("7");
  });
});