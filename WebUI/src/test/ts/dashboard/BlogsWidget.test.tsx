/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import { BlogsWidget } from "@/dashboard/BlogsWidget";
import * as homeApi from "@/api/home/homeApi";

vi.mock("@/api/home/homeApi", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/api/home/homeApi")>();
  return {
    ...actual,
    fetchAllBlogs: vi.fn(),
    fetchSites: vi.fn().mockResolvedValue([{ name: "Demo" }]),
    fetchBlogListTemplates: vi.fn().mockResolvedValue([
      { id: "list-t", name: "Blog List Tmpl" },
    ]),
    fetchBlogPostTemplates: vi.fn().mockResolvedValue([
      { id: "post-t", name: "Blog Post Tmpl" },
    ]),
    formatApiError: vi.fn((_e: unknown, f: string) => f),
  };
});

vi.mock("@/api/client", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/api/client")>();
  return {
    ...actual,
    post: vi.fn().mockResolvedValue({}),
  };
});

describe("BlogsWidget", () => {
  beforeEach(() => {
    vi.mocked(homeApi.fetchAllBlogs).mockReset();
  });

  it("shows empty state when no blogs", async () => {
    vi.mocked(homeApi.fetchAllBlogs).mockResolvedValue([]);
    render(<BlogsWidget />);
    await waitFor(() => {
      expect(screen.getByTestId("blogs-widget-empty")).toBeDefined();
    });
  });

  it("lists blogs from allBlogs", async () => {
    vi.mocked(homeApi.fetchAllBlogs).mockResolvedValue([
      {
        title: "News",
        folderPath: "/Sites/Demo/News",
        templateId: "t1",
        site: "Demo",
        path: "/Sites/Demo/News",
      },
    ]);
    render(<BlogsWidget />);
    await waitFor(() => {
      expect(screen.getByTestId("blogs-widget-list")).toBeDefined();
    });
    expect(screen.getAllByText(/News/).length).toBeGreaterThan(0);
  });

  it("opens create blog section form with filtered template lists", async () => {
    vi.mocked(homeApi.fetchAllBlogs).mockResolvedValue([]);
    render(<BlogsWidget />);
    await waitFor(() => screen.getByTestId("blogs-widget-empty"));
    fireEvent.click(screen.getByTestId("blogs-widget-create-section"));
    await waitFor(() => {
      expect(screen.getByTestId("blogs-widget-create-form")).toBeDefined();
    });
    await waitFor(() => {
      expect(homeApi.fetchBlogListTemplates).toHaveBeenCalledWith("Demo");
      expect(homeApi.fetchBlogPostTemplates).toHaveBeenCalledWith("Demo");
    });
    const indexSel = screen.getByTestId(
      "blogs-create-index-template",
    ) as HTMLSelectElement;
    const postSel = screen.getByTestId(
      "blogs-create-post-template",
    ) as HTMLSelectElement;
    expect(
      Array.from(indexSel.options).map((o) => o.value),
    ).toContain("list-t");
    expect(Array.from(postSel.options).map((o) => o.value)).toContain("post-t");
  });

  it("shows guidance when no eligible templates", async () => {
    vi.mocked(homeApi.fetchAllBlogs).mockResolvedValue([]);
    vi.mocked(homeApi.fetchBlogListTemplates).mockResolvedValueOnce([]);
    vi.mocked(homeApi.fetchBlogPostTemplates).mockResolvedValueOnce([]);
    render(<BlogsWidget />);
    await waitFor(() => screen.getByTestId("blogs-widget-empty"));
    fireEvent.click(screen.getByTestId("blogs-widget-create-section"));
    await waitFor(() => {
      expect(screen.getByTestId("blogs-create-no-templates")).toBeDefined();
    });
    expect(
      (screen.getByTestId("blogs-create-submit") as HTMLButtonElement).disabled,
    ).toBe(true);
  });
});

