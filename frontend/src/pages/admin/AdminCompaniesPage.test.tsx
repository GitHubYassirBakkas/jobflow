/** @vitest-environment jsdom */

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import {
  getAdminCompanies,
  updateAdminCompany
} from "../../features/admin/adminApi";
import type { AdminCompany } from "../../types/admin";
import { AdminCompaniesPage } from "./AdminCompaniesPage";

vi.mock("../../features/admin/adminApi", () => ({
  getAdminCompanies: vi.fn(),
  updateAdminCompany: vi.fn()
}));

const mockedGetAdminCompanies = vi.mocked(getAdminCompanies);
const mockedUpdateAdminCompany = vi.mocked(updateAdminCompany);

function makeCompany(
  overrides: Partial<AdminCompany> = {}
): AdminCompany {
  return {
    id: 12,
    name: "Acme Ltd",
    description: "A software company",
    website: "https://example.com",
    location: "Meknes",
    createdAt: "2026-10-10T10:00:00",
    ...overrides
  };
}

beforeEach(() => {
  mockedGetAdminCompanies.mockReset();
  mockedUpdateAdminCompany.mockReset();
});

afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
});

describe("AdminCompaniesPage", () => {
  it("shows a loading state while companies are being fetched", () => {
    mockedGetAdminCompanies.mockReturnValue(
      new Promise<AdminCompany[]>(() => {})
    );

    render(<AdminCompaniesPage />);

    expect(screen.getByRole("status").textContent).toContain(
      "Loading companies..."
    );
  });

  it("renders company information and opens the edit dialog", async () => {
    mockedGetAdminCompanies.mockResolvedValue([makeCompany()]);

    render(<AdminCompaniesPage />);

    expect(await screen.findByText("Acme Ltd")).toBeTruthy();
    expect(screen.getByText("Meknes")).toBeTruthy();
    expect(screen.getByText("A software company")).toBeTruthy();

    fireEvent.click(screen.getByRole("button", { name: /edit/i }));

    expect(screen.getByRole("dialog")).toBeTruthy();
    expect(
      screen.getByRole("heading", { name: "Edit company" })
    ).toBeTruthy();
    expect(screen.getByLabelText("Name")).toHaveProperty(
      "value",
      "Acme Ltd"
    );
  });

  it("shows an error when loading companies fails", async () => {
    mockedGetAdminCompanies.mockRejectedValue(
      new Error("Internal test error")
    );

    render(<AdminCompaniesPage />);

    const alert = await screen.findByRole("alert");

    expect(alert.textContent).toContain("Could not load companies");
    expect(alert.textContent).toContain(
      "Something went wrong. Please try again."
    );
  });

  it("shows an empty state when no companies exist", async () => {
    mockedGetAdminCompanies.mockResolvedValue([]);

    render(<AdminCompaniesPage />);

    expect(await screen.findByText("No companies found")).toBeTruthy();
    expect(
      screen.getByText(
        "Companies registered in JobFlow will appear here when they exist."
      )
    ).toBeTruthy();
  });

  it("saves trimmed company fields and updates the displayed company", async () => {
    const company = makeCompany();

    mockedGetAdminCompanies.mockResolvedValue([company]);

    const updatedCompany = makeCompany({
      name: "Acme Updated",
      description: "Updated description",
      website: "https://updated.example.com",
      location: "Rabat"
    });

    mockedUpdateAdminCompany.mockResolvedValue(updatedCompany);

    render(<AdminCompaniesPage />);

    await screen.findByText("Acme Ltd");

    fireEvent.click(screen.getByRole("button", { name: /edit/i }));

    fireEvent.change(screen.getByLabelText("Name"), {
      target: { value: "  Acme Updated  " }
    });

    fireEvent.change(screen.getByLabelText("Description"), {
      target: { value: "  Updated description  " }
    });

    fireEvent.change(screen.getByLabelText("Website"), {
      target: { value: "https://updated.example.com" }
    });

    fireEvent.change(screen.getByLabelText("Location"), {
      target: { value: "  Rabat  " }
    });

    fireEvent.click(screen.getByRole("button", { name: "Save" }));

    expect(mockedUpdateAdminCompany).toHaveBeenCalledWith(12, {
      name: "Acme Updated",
      description: "Updated description",
      website: "https://updated.example.com",
      location: "Rabat"
    });

    expect(await screen.findByText("Acme Updated")).toBeTruthy();
    expect(screen.getByText("Updated description")).toBeTruthy();
    expect(screen.queryByRole("dialog")).toBeNull();
  });

  it("displays a helpful message when saving the company fails", async () => {
    mockedGetAdminCompanies.mockResolvedValue([makeCompany()]);
    mockedUpdateAdminCompany.mockRejectedValue(
      new Error("Network failure")
    );

    render(<AdminCompaniesPage />);

    await screen.findByText("Acme Ltd");

    fireEvent.click(screen.getByRole("button", { name: /edit/i }));
    fireEvent.click(screen.getByRole("button", { name: "Save" }));

    expect(
      await screen.findByText("Something went wrong. Please try again.")
    ).toBeTruthy();

    expect(screen.getByRole("dialog")).toBeTruthy();
  });

  it("closes the edit dialog when Cancel is clicked", async () => {
    mockedGetAdminCompanies.mockResolvedValue([makeCompany()]);

    render(<AdminCompaniesPage />);

    await screen.findByText("Acme Ltd");

    fireEvent.click(screen.getByRole("button", { name: /edit/i }));

    expect(screen.getByRole("dialog")).toBeTruthy();

    fireEvent.click(screen.getByRole("button", { name: "Cancel" }));

    expect(screen.queryByRole("dialog")).toBeNull();
  });

  it("closes the edit dialog with Escape and restores focus to the Edit button", async () => {
    mockedGetAdminCompanies.mockResolvedValue([makeCompany()]);

    render(<AdminCompaniesPage />);

    const editButton = await screen.findByRole("button", {
      name: /edit/i
    });

    fireEvent.click(editButton);

    expect(screen.getByRole("dialog")).toBeTruthy();

    const cancelButton = screen.getByRole("button", {
      name: "Cancel"
    });

    expect(document.activeElement).toBe(cancelButton);

    fireEvent.keyDown(document, { key: "Escape" });

    expect(screen.queryByRole("dialog")).toBeNull();
    expect(document.activeElement).toBe(editButton);
  });

  it("keeps keyboard focus inside the edit dialog when Tab reaches the last control", async () => {
    mockedGetAdminCompanies.mockResolvedValue([makeCompany()]);

    render(<AdminCompaniesPage />);

    fireEvent.click(
      await screen.findByRole("button", { name: /edit/i })
    );

    const dialog = screen.getByRole("dialog");
    const nameInput = screen.getByLabelText("Name");
    const saveButton = screen.getByRole("button", { name: "Save" });

    saveButton.focus();

    fireEvent.keyDown(document, { key: "Tab" });

    expect(document.activeElement).toBe(nameInput);
    expect(dialog.contains(document.activeElement)).toBe(true);
  });
});