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

describe("HomeShell", () => {
  beforeEach(() => {
    (window as unknown as { I18N: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
  });

  it("renders shell and section navigation", () => {
    render(<HomeShell initialSection="list" />);
    expect(screen.getByTestId("home-shell")).toBeDefined();
    expect(screen.getByText("perc.ui.home@My Recent")).toBeDefined();
    expect(screen.getByText("perc.ui.home.modern@My Bookmarks")).toBeDefined();
    expect(screen.getByText("perc.ui.home.modern@Library")).toBeDefined();
    expect(screen.getByText("perc.ui.home.modern@Search")).toBeDefined();
    expect(screen.getByText("perc.ui.home@Add New")).toBeDefined();
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

  it("starts on library when initialScreen is library", async () => {
    render(<HomeShell initialSection="library" />);
    await waitFor(() => {
      expect(screen.getByText("perc.ui.home@No Site Exists")).toBeDefined();
    });
  });

  it("switches sections on nav click", async () => {
    render(<HomeShell initialSection="list" />);
    fireEvent.click(screen.getByText("perc.ui.home.modern@Search"));
    await waitFor(() => {
      expect(
        screen.getByLabelText("perc.ui.home.modern@Search"),
      ).toBeDefined();
    });
  });
});
