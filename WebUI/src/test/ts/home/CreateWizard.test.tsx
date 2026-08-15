/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import { CreateWizard } from "@/home/create/CreateWizard";
import { PageWizard } from "@/home/create/PageWizard";
import * as homeApi from "@/api/home/homeApi";

vi.mock("@/api/home/homeApi", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/api/home/homeApi")>();
  return {
    ...actual,
    fetchSites: vi.fn().mockResolvedValue([{ name: "Demo" }]),
    fetchAllBlogs: vi.fn().mockResolvedValue([
      {
        title: "News",
        folderPath: "/Sites/Demo/News",
        templateId: "tmpl-1",
        site: "Demo",
      },
    ]),
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
    fetchAssetTypes: vi.fn().mockResolvedValue([
      { id: "percImage", name: "Image", label: "Image" },
    ]),
    createPageAndPath: vi.fn().mockResolvedValue("/Sites/Demo/page.html"),
    createPageAndItem: vi.fn().mockResolvedValue({
      path: "/Sites/Demo/page.html",
      itemId: "55",
    }),
    formatApiError: vi.fn((_e: unknown, fallback: string) => fallback),
  };
});

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
    expect(screen.getByTestId("create-choose-page")).toBeDefined();
    expect(screen.getByTestId("create-choose-asset")).toBeDefined();
    expect(screen.getByTestId("create-choose-blog")).toBeDefined();
  });

  it("opens page wizard from chooser", async () => {
    render(<CreateWizard />);
    await waitFor(() => screen.getByTestId("create-type-chooser"));
    fireEvent.click(screen.getByTestId("create-choose-page"));
    await waitFor(() => {
      expect(screen.getByTestId("page-wizard")).toBeDefined();
    });
  });

  it("opens asset wizard and lists widget ids as options", async () => {
    render(<CreateWizard />);
    await waitFor(() => screen.getByTestId("create-type-chooser"));
    fireEvent.click(screen.getByTestId("create-choose-asset"));
    await waitFor(() => {
      expect(screen.getByTestId("asset-wizard")).toBeDefined();
    });
    const select = screen.getByTestId("asset-wizard-type") as HTMLSelectElement;
    const values = Array.from(select.options).map((o) => o.value);
    expect(values).toContain("percImage");
    expect(homeApi.fetchAssetTypes).toHaveBeenCalled();
  });

  it("opens blog wizard", async () => {
    render(<CreateWizard />);
    await waitFor(() => screen.getByTestId("create-type-chooser"));
    fireEvent.click(screen.getByTestId("create-choose-blog"));
    await waitFor(() => {
      expect(screen.getByTestId("blog-wizard")).toBeDefined();
    });
    expect(homeApi.fetchAllBlogs).toHaveBeenCalled();
  });

  it("page create opens the React editor host instead of leftover view=editor", async () => {
    const openCreated = vi.fn().mockResolvedValue(true);
    render(<PageWizard onBack={() => undefined} openCreated={openCreated} />);
    await waitFor(() => screen.getByTestId("page-wizard"));
    await waitFor(() => {
      const sel = document.getElementById("pw-template") as HTMLSelectElement;
      expect(Array.from(sel.options).some((o) => o.value === "t1")).toBe(true);
    });
    fireEvent.change(document.getElementById("pw-template") as HTMLSelectElement, {
      target: { value: "t1" },
    });
    fireEvent.change(document.getElementById("pw-title") as HTMLInputElement, {
      target: { value: "About" },
    });
    fireEvent.click(screen.getByTestId("page-wizard-submit"));
    await waitFor(() => {
      expect(homeApi.createPageAndItem).toHaveBeenCalled();
      expect(openCreated).toHaveBeenCalled();
    });
    expect(openCreated.mock.calls[0]?.[0]).toMatchObject({
      id: "55",
      path: "/Sites/Demo/page.html",
    });
  });

  it("shows no-blogs empty state when none configured", async () => {
    vi.mocked(homeApi.fetchAllBlogs).mockResolvedValueOnce([]);
    render(<CreateWizard />);
    await waitFor(() => screen.getByTestId("create-type-chooser"));
    fireEvent.click(screen.getByTestId("create-choose-blog"));
    await waitFor(() => {
      expect(screen.getByTestId("blog-wizard-empty")).toBeDefined();
    });
  });
});
