/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { LibrarySection } from "@/home/sections/LibrarySection";
import * as homeApi from "@/api/home/homeApi";

vi.mock("@/api/home/homeApi", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/api/home/homeApi")>();
  return {
    ...actual,
    fetchSites: vi.fn(),
    fetchFolderChildren: vi.fn(),
    fetchMyContent: vi.fn().mockResolvedValue([]),
    addToMyPages: vi.fn(),
    removeFromMyPages: vi.fn(),
  };
});

describe("LibrarySection", () => {
  beforeEach(() => {
    (window as unknown as { I18N: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    vi.mocked(homeApi.fetchSites).mockReset();
    vi.mocked(homeApi.fetchFolderChildren).mockReset();
    vi.mocked(homeApi.fetchMyContent).mockResolvedValue([]);
  });

  it("lists sites then navigates into a site folder", async () => {
    vi.mocked(homeApi.fetchSites).mockResolvedValue([
      { name: "Demo", id: "Demo" },
    ]);
    vi.mocked(homeApi.fetchFolderChildren).mockResolvedValue([
      {
        name: "index.html",
        id: "p1",
        path: "/Sites/Demo/index.html",
        type: "percPage",
      },
      {
        name: "blog",
        path: "/Sites/Demo/blog",
        type: "Folder",
        folder: true,
      },
    ]);

    render(<LibrarySection />);
    await waitFor(() => {
      expect(screen.getByText("Demo")).toBeDefined();
    });
    fireEvent.click(screen.getByTestId("home-library-open-site"));
    await waitFor(() => {
      expect(screen.getByTestId("home-library-path").textContent).toBe(
        "/Sites/Demo",
      );
    });
    expect(homeApi.fetchFolderChildren).toHaveBeenCalledWith("/Sites/Demo");
    expect(screen.getByText("index.html")).toBeDefined();
    expect(screen.getByText("blog")).toBeDefined();
  });

  it("goes up from nested folder to parent then site list", async () => {
    vi.mocked(homeApi.fetchSites).mockResolvedValue([
      { name: "Demo", id: "Demo" },
    ]);
    vi.mocked(homeApi.fetchFolderChildren).mockImplementation(
      async (p: string) => {
        if (p === "/Sites/Demo") {
          return [
            {
              name: "blog",
              path: "/Sites/Demo/blog",
              type: "Folder",
              folder: true,
            },
          ];
        }
        if (p === "/Sites/Demo/blog") {
          return [
            {
              name: "post.html",
              id: "p2",
              path: "/Sites/Demo/blog/post.html",
              type: "percPage",
            },
          ];
        }
        return [];
      },
    );

    render(<LibrarySection />);
    await waitFor(() => expect(screen.getByText("Demo")).toBeDefined());
    fireEvent.click(screen.getByTestId("home-library-open-site"));
    await waitFor(() => expect(screen.getByText("blog")).toBeDefined());
    fireEvent.click(screen.getByTestId("home-library-open-folder"));
    await waitFor(() => {
      expect(screen.getByTestId("home-library-path").textContent).toBe(
        "/Sites/Demo/blog",
      );
    });
    fireEvent.click(screen.getByTestId("home-library-up"));
    await waitFor(() => {
      expect(screen.getByTestId("home-library-path").textContent).toBe(
        "/Sites/Demo",
      );
    });
    fireEvent.click(screen.getByTestId("home-library-up"));
    await waitFor(() => {
      expect(screen.getByTestId("home-library-site-list")).toBeDefined();
    });
  });
});
