/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
import { HomeShell } from "@/home/HomeShell";

vi.mock("@/api/home/homeApi", () => ({
  fetchRecentItems: vi.fn().mockResolvedValue([]),
  fetchMyContent: vi.fn().mockResolvedValue([]),
  fetchSites: vi.fn().mockResolvedValue([]),
  fetchFolderChildren: vi.fn().mockResolvedValue([]),
  searchContent: vi.fn().mockResolvedValue([]),
  createPage: vi.fn().mockResolvedValue({}),
}));

// Lazy GadgetsSection pulls the full dashboard graph; stub for Home shell tests
vi.mock("@/home/sections/GadgetsSection", () => ({
  GadgetsSection: () => (
    <div data-testid="home-gadgets-section">
      <div data-testid="dashboard-root" data-embedded="1" />
    </div>
  ),
}));

describe("HomeShell", () => {
  beforeEach(() => {
    (window as unknown as { I18N: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
  });

  it("renders shell and section navigation including gadgets", () => {
    render(<HomeShell initialSection="list" />);
    expect(screen.getByTestId("home-shell")).toBeDefined();
    // TMX stub returns key; message() falls back to text after @
    expect(screen.getByText("My Recent")).toBeDefined();
    expect(screen.getByText("My Bookmarks")).toBeDefined();
    expect(screen.getByText("Library")).toBeDefined();
    expect(screen.getByText("Search")).toBeDefined();
    expect(screen.getByText("Add New")).toBeDefined();
    expect(screen.getByText("Gadgets")).toBeDefined();
  });

  it("opens gadgets section with dashboard widgets embedded", async () => {
    render(<HomeShell embedded initialSection="gadgets" />);
    await waitFor(() => {
      expect(screen.getByTestId("home-gadgets-section")).toBeDefined();
    });
    expect(screen.getByTestId("dashboard-root")).toBeDefined();
    expect(screen.getByTestId("dashboard-root").getAttribute("data-embedded")).toBe(
      "1",
    );
  });

  it("wraps the shell in the intersoft theme and renders branded chrome", () => {
    const { container } = render(<HomeShell initialSection="list" />);
    const scope = container.querySelector("[data-perc-theme='intersoft']");
    expect(scope).not.toBeNull();
    expect(screen.getByTestId("perc-brand-bar")).toBeDefined();
    expect(screen.getByTestId("perc-brand-product").textContent).toBe(
      "Percussion CMS",
    );
    expect(screen.getByTestId("perc-brand-footer")).toBeDefined();
  });

  it("embedded mode omits brand chrome for SPA AppLayout", () => {
    render(<HomeShell embedded initialSection="list" />);
    expect(screen.getByTestId("home-shell")).toBeDefined();
    expect(screen.queryByTestId("perc-brand-bar")).toBeNull();
    expect(screen.queryByTestId("perc-brand-footer")).toBeNull();
  });

  it("starts on library when initialScreen is library", async () => {
    render(<HomeShell initialSection="library" />);
    await waitFor(() => {
      expect(screen.getByText("No Site Exists")).toBeDefined();
    });
  });

  it("switches sections on nav click", async () => {
    render(<HomeShell initialSection="list" />);
    fireEvent.click(screen.getByTestId("home-nav-search"));
    await waitFor(() => {
      expect(screen.getByTestId("home-nav-search").getAttribute("aria-current")).toBe(
        "page",
      );
    });
  });

  it("notifies onSectionChange when a section tab is clicked", () => {
    const onSectionChange = vi.fn();
    render(
      <HomeShell
        embedded
        initialSection="recent"
        onSectionChange={onSectionChange}
      />,
    );
    fireEvent.click(screen.getByTestId("home-nav-gadgets"));
    expect(onSectionChange).toHaveBeenCalledWith("gadgets");
  });
});

