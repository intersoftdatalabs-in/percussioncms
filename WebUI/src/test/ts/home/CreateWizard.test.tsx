/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import { CreateWizard } from "@/home/create/CreateWizard";

vi.mock("@/api/home/homeApi", () => ({
  fetchSites: vi.fn().mockResolvedValue([{ name: "Demo" }]),
  fetchBlogsForSite: vi.fn().mockResolvedValue([
    {
      title: "News",
      folderPath: "/Sites/Demo/News",
      templateId: "tmpl-1",
      site: "Demo",
    },
  ]),
  fetchTemplatesForSite: vi.fn().mockResolvedValue([
    { id: "t1", name: "Base" },
  ]),
  fetchFolderChildren: vi.fn().mockResolvedValue([]),
  fetchAssetTypes: vi.fn().mockResolvedValue([{ id: "w1", name: "Image" }]),
  createPageAndPath: vi.fn().mockResolvedValue("/Sites/Demo/page"),
  formatApiError: vi.fn((_e: unknown, fallback: string) => fallback),
}));

describe("CreateWizard", () => {
  beforeEach(() => {
    (window as unknown as { I18N: { message: (k: string) => string } }).I18N = {
      message: (k) => k,
    };
  });

  it("shows type chooser with page asset and blog", async () => {
    render(<CreateWizard />);
    await waitFor(() => {
      expect(screen.getByTestId("create-type-chooser")).toBeDefined();
    });
    expect(screen.getByText("perc.ui.home.modern@Create Page")).toBeDefined();
    expect(screen.getByText("perc.ui.home.modern@Create Asset")).toBeDefined();
    expect(
      screen.getByText("perc.ui.home.modern@Create Blog Post"),
    ).toBeDefined();
  });

  it("opens page wizard from chooser", async () => {
    render(<CreateWizard />);
    await waitFor(() => screen.getByTestId("create-type-chooser"));
    fireEvent.click(screen.getByText("perc.ui.home.modern@Create Page"));
    await waitFor(() => {
      expect(screen.getByTestId("page-wizard")).toBeDefined();
    });
  });
});
