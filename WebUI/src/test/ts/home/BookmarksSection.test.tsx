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
import { BookmarksSection } from "@/home/sections/BookmarksSection";
import * as homeApi from "@/api/home/homeApi";

vi.mock("@/api/home/homeApi", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/api/home/homeApi")>();
  return {
    ...actual,
    fetchMyContent: vi.fn(),
    removeFromMyPages: vi.fn(),
  };
});

describe("BookmarksSection", () => {
  beforeEach(() => {
    (window as unknown as { I18N: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    vi.mocked(homeApi.fetchMyContent).mockReset();
    vi.mocked(homeApi.removeFromMyPages).mockReset();
  });

  it("shows empty state when there are no bookmarks", async () => {
    vi.mocked(homeApi.fetchMyContent).mockResolvedValue([]);
    render(<BookmarksSection />);
    await waitFor(() => {
      expect(screen.getByTestId("home-bookmarks-empty")).toBeDefined();
    });
  });

  it("lists bookmarks and removes one on click", async () => {
    vi.mocked(homeApi.fetchMyContent).mockResolvedValue([
      {
        id: "guid-1",
        name: "Home Page",
        path: "/Sites/Demo/index.html",
      },
      {
        id: "guid-2",
        name: "About",
        path: "/Sites/Demo/about.html",
      },
    ]);
    vi.mocked(homeApi.removeFromMyPages).mockResolvedValue(undefined);

    const onOpen = vi.fn();
    render(<BookmarksSection onOpenItem={onOpen} />);

    await waitFor(() => {
      expect(screen.getByTestId("home-bookmarks-list")).toBeDefined();
    });
    expect(screen.getByText("Home Page")).toBeDefined();
    expect(screen.getByText("About")).toBeDefined();

    const removeButtons = screen.getAllByTestId("home-bookmarks-bookmark");
    expect(removeButtons).toHaveLength(2);
    fireEvent.click(removeButtons[0]);

    await waitFor(() => {
      expect(homeApi.removeFromMyPages).toHaveBeenCalledWith("guid-1");
    });
    await waitFor(() => {
      expect(screen.queryByText("Home Page")).toBeNull();
    });
    expect(screen.getByText("About")).toBeDefined();

    fireEvent.click(screen.getAllByTestId("home-bookmarks-open")[0]);
    expect(onOpen).toHaveBeenCalledWith(
      expect.objectContaining({ id: "guid-2", name: "About" }),
    );
  });

  it("surfaces remove errors without dropping the list", async () => {
    vi.mocked(homeApi.fetchMyContent).mockResolvedValue([
      { id: "g1", name: "Keep Me", path: "/Sites/x" },
    ]);
    vi.mocked(homeApi.removeFromMyPages).mockRejectedValue({
      status: 500,
      statusText: "Error",
      body: "server down",
    });

    render(<BookmarksSection />);
    await waitFor(() => {
      expect(screen.getByText("Keep Me")).toBeDefined();
    });
    fireEvent.click(screen.getByTestId("home-bookmarks-bookmark"));
    await waitFor(() => {
      expect(screen.getByTestId("home-bookmarks-action-error").textContent).toContain(
        "server down",
      );
    });
    expect(screen.getByText("Keep Me")).toBeDefined();
  });
});
