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
    fetchTemplatesForSite: vi
      .fn()
      .mockResolvedValue([{ id: "t1", name: "Base" }]),
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

  it("opens create blog section form", async () => {
    vi.mocked(homeApi.fetchAllBlogs).mockResolvedValue([]);
    render(<BlogsWidget />);
    await waitFor(() => screen.getByTestId("blogs-widget-empty"));
    fireEvent.click(screen.getByTestId("blogs-widget-create-section"));
    await waitFor(() => {
      expect(screen.getByTestId("blogs-widget-create-form")).toBeDefined();
    });
  });
});

