/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import {
  formatSearchResultCount,
  SearchSection,
} from "@/home/sections/SearchSection";
import * as homeApi from "@/api/home/homeApi";

vi.mock("@/api/home/homeApi", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/api/home/homeApi")>();
  return {
    ...actual,
    searchContent: vi.fn(),
    fetchMyContent: vi.fn().mockResolvedValue([]),
    addToMyPages: vi.fn(),
    removeFromMyPages: vi.fn(),
  };
});

describe("SearchSection", () => {
  beforeEach(() => {
    (window as unknown as { I18N: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    vi.mocked(homeApi.searchContent).mockReset();
    vi.mocked(homeApi.fetchMyContent).mockResolvedValue([]);
  });

  it("formatSearchResultCount", () => {
    expect(formatSearchResultCount(0)).toBe("0 results");
    expect(formatSearchResultCount(1)).toBe("1 result");
    expect(formatSearchResultCount(3)).toBe("3 results");
  });

  it("shows result count after a successful search", async () => {
    vi.mocked(homeApi.searchContent).mockResolvedValue([
      { id: "1", name: "Page A", path: "/Sites/Demo/a" },
      { id: "2", name: "Page B", path: "/Sites/Demo/b" },
    ]);
    render(<SearchSection />);
    fireEvent.change(screen.getByTestId("home-search-input"), {
      target: { value: "Page" },
    });
    fireEvent.click(screen.getByTestId("home-search-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("home-search-count").textContent).toContain(
        "2 results",
      );
    });
    expect(screen.getByTestId("home-search-results")).toBeDefined();
  });

  it("shows empty state with query when no hits", async () => {
    vi.mocked(homeApi.searchContent).mockResolvedValue([]);
    render(<SearchSection />);
    fireEvent.change(screen.getByTestId("home-search-input"), {
      target: { value: "zzz-none" },
    });
    fireEvent.click(screen.getByTestId("home-search-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("home-search-empty")).toBeDefined();
    });
    expect(screen.getByTestId("home-search-empty-query").textContent).toContain(
      "zzz-none",
    );
  });
});
