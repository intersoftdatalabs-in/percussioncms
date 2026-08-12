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

import {
  cleanup,
  render,
  screen,
  waitFor,
  fireEvent,
} from "@testing-library/react";
import React from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ArchitectureShell } from "../../../main/ts/architecture/ArchitectureShell";
import * as homeApi from "../../../main/ts/api/home/homeApi";
import * as sectionApi from "../../../main/ts/api/architecture/sectionApi";

describe("ArchitectureShell (#3095 read-only tree)", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => {
        const at = key.indexOf("@");
        return at >= 0 ? key.slice(at + 1) : key;
      },
    };
    vi.restoreAllMocks();
  });

  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("shows empty state when no site is selected and no sites load", async () => {
    vi.spyOn(homeApi, "fetchSites").mockResolvedValue([]);
    render(<ArchitectureShell embedded />);
    await waitFor(() => {
      expect(screen.getByTestId("architecture-sites-empty")).toBeTruthy();
    });
    expect(screen.getByTestId("architecture-empty-state")).toBeTruthy();
    expect(screen.getByTestId("architecture-shell-title").textContent).toMatch(
      /Architecture/i,
    );
  });

  it("loads tree when initial site is provided", async () => {
    vi.spyOn(homeApi, "fetchSites").mockResolvedValue([
      { name: "Corporate Investments" },
      { name: "Demo" },
    ]);
    vi.spyOn(sectionApi, "loadSectionTree").mockResolvedValue({
      id: "root",
      title: "Home",
      folderPath: "//Sites/Corporate Investments",
      sectionType: "section",
      requiresLogin: false,
      children: [
        {
          id: "c1",
          title: "About",
          folderPath: null,
          sectionType: "section",
          requiresLogin: false,
          children: [],
        },
      ],
    });

    render(
      <ArchitectureShell embedded initialSite="Corporate Investments" />,
    );

    await waitFor(() => {
      expect(screen.getByTestId("architecture-nav-tree")).toBeTruthy();
    });
    await waitFor(() => {
      expect(screen.getByTestId("nav-tree-item-root")).toBeTruthy();
    });
    expect(screen.getByTestId("nav-tree-item-root").textContent).toMatch(
      /Home/i,
    );
    expect(screen.getByTestId("architecture-site-hint").textContent).toContain(
      "Corporate Investments",
    );
    expect(screen.getByTestId("architecture-readonly-note")).toBeTruthy();
    expect(sectionApi.loadSectionTree).toHaveBeenCalledWith(
      "Corporate Investments",
    );
  });

  it("surfaces tree load errors", async () => {
    vi.spyOn(homeApi, "fetchSites").mockResolvedValue([{ name: "Demo" }]);
    vi.spyOn(sectionApi, "loadSectionTree").mockRejectedValue({
      status: 404,
      statusText: "Not Found",
      body: { message: "Site not found" },
    });

    render(<ArchitectureShell embedded initialSite="Demo" />);

    await waitFor(() => {
      expect(screen.getByTestId("architecture-nav-tree-error")).toBeTruthy();
    });
    expect(
      screen.getByTestId("architecture-nav-tree-error").textContent,
    ).toMatch(/Site not found|Could not load/i);
  });

  it("refresh reloads the tree", async () => {
    vi.spyOn(homeApi, "fetchSites").mockResolvedValue([{ name: "Demo" }]);
    const loadSpy = vi
      .spyOn(sectionApi, "loadSectionTree")
      .mockResolvedValue({
        id: "root",
        title: "Home",
        folderPath: null,
        sectionType: "section",
        requiresLogin: false,
        children: [],
      });

    render(<ArchitectureShell embedded initialSite="Demo" />);
    await waitFor(() => {
      expect(loadSpy).toHaveBeenCalledTimes(1);
    });
    fireEvent.click(screen.getByTestId("architecture-refresh"));
    await waitFor(() => {
      expect(loadSpy).toHaveBeenCalledTimes(2);
    });
  });
});
