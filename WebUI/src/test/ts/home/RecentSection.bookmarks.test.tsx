/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { RecentSection } from "@/home/sections/RecentSection";
import * as homeApi from "@/api/home/homeApi";

vi.mock("@/api/home/homeApi", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/api/home/homeApi")>();
  return {
    ...actual,
    fetchRecentItems: vi.fn(),
    fetchMyContent: vi.fn(),
    addToMyPages: vi.fn(),
    removeFromMyPages: vi.fn(),
  };
});

describe("RecentSection bookmarks", () => {
  beforeEach(() => {
    (window as unknown as { I18N: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    vi.mocked(homeApi.fetchRecentItems).mockReset();
    vi.mocked(homeApi.fetchMyContent).mockReset();
    vi.mocked(homeApi.addToMyPages).mockReset();
    vi.mocked(homeApi.removeFromMyPages).mockReset();
  });

  it("adds a bookmark from Recent when not already favorited", async () => {
    vi.mocked(homeApi.fetchRecentItems).mockResolvedValue([
      { id: "r1", name: "Recent Page", path: "/Sites/Demo/r" },
    ]);
    vi.mocked(homeApi.fetchMyContent).mockResolvedValue([]);
    vi.mocked(homeApi.addToMyPages).mockResolvedValue(undefined);

    render(<RecentSection />);
    await waitFor(() => {
      expect(screen.getByTestId("home-recent-list")).toBeDefined();
    });

    const btn = screen.getByTestId("home-recent-bookmark");
    expect(btn.getAttribute("aria-pressed")).toBe("false");
    fireEvent.click(btn);

    await waitFor(() => {
      expect(homeApi.addToMyPages).toHaveBeenCalledWith("r1");
    });
    await waitFor(() => {
      expect(
        screen.getByTestId("home-recent-bookmark").getAttribute("aria-pressed"),
      ).toBe("true");
    });
  });

  it("removes a bookmark from Recent when already favorited", async () => {
    vi.mocked(homeApi.fetchRecentItems).mockResolvedValue([
      { id: "r2", name: "Starred", path: "/Sites/Demo/s" },
    ]);
    vi.mocked(homeApi.fetchMyContent).mockResolvedValue([
      { id: "r2", name: "Starred" },
    ]);
    vi.mocked(homeApi.removeFromMyPages).mockResolvedValue(undefined);

    render(<RecentSection />);
    await waitFor(() => {
      expect(
        screen.getByTestId("home-recent-bookmark").getAttribute("aria-pressed"),
      ).toBe("true");
    });

    fireEvent.click(screen.getByTestId("home-recent-bookmark"));
    await waitFor(() => {
      expect(homeApi.removeFromMyPages).toHaveBeenCalledWith("r2");
    });
    await waitFor(() => {
      expect(
        screen.getByTestId("home-recent-bookmark").getAttribute("aria-pressed"),
      ).toBe("false");
    });
  });
});
