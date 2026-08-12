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

import { cleanup, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { MemoryRouter, Route, Routes } from "react-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ArchitectureRoute } from "../../../../main/ts/app/routes/ArchitectureRoute";
import * as homeApi from "../../../../main/ts/api/home/homeApi";
import * as sectionApi from "../../../../main/ts/api/architecture/sectionApi";

const bootstrapState = {
  isAdmin: true,
  isDesigner: true,
  isWidgetBuilderActive: false,
  userName: "Admin",
  locale: "en-us",
  entry: "architecture",
  allowExternalAvatarFetch: true,
};

vi.mock("../../../../main/ts/app/bootstrap/BootstrapContext", () => ({
  useSpaBootstrap: () => bootstrapState,
}));

vi.mock("../../../../main/ts/registry", () => ({
  loadComponent: async (name: string) => {
    if (name !== "ArchitectureShell") {
      throw new Error(`unexpected component: ${name}`);
    }
    const mod = await import(
      "../../../../main/ts/architecture/ArchitectureShell"
    );
    return mod.ArchitectureShell;
  },
}));

describe("ArchitectureRoute (#3094 / #3095)", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => {
        const at = key.indexOf("@");
        return at >= 0 ? key.slice(at + 1) : key;
      },
    };
    bootstrapState.isAdmin = true;
    bootstrapState.isDesigner = true;
    vi.spyOn(homeApi, "fetchSites").mockResolvedValue([]);
    vi.spyOn(sectionApi, "loadSectionTree").mockResolvedValue(null);
  });

  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  function renderAt(path: string) {
    return render(
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route path="/architecture" element={<ArchitectureRoute />} />
          <Route path="/architecture/:site" element={<ArchitectureRoute />} />
          <Route path="/home" element={<div data-testid="home-redirect" />} />
        </Routes>
      </MemoryRouter>,
    );
  }

  it("mounts shell for Admin/Designer with empty state when no sites", async () => {
    renderAt("/architecture");
    await waitFor(() => {
      expect(screen.getByTestId("perc-architecture-shell")).toBeTruthy();
    });
    await waitFor(() => {
      expect(screen.getByTestId("architecture-empty-state")).toBeTruthy();
    });
  });

  it("passes path site param into shell and loads tree", async () => {
    vi.spyOn(homeApi, "fetchSites").mockResolvedValue([{ name: "DemoSite" }]);
    vi.spyOn(sectionApi, "loadSectionTree").mockResolvedValue({
      id: "r",
      title: "Home",
      folderPath: null,
      sectionType: "section",
      requiresLogin: false,
      children: [],
    });
    renderAt("/architecture/DemoSite");
    await waitFor(() => {
      expect(screen.getByTestId("perc-architecture-shell")).toBeTruthy();
    });
    await waitFor(() => {
      expect(
        screen.getByTestId("perc-architecture-shell").getAttribute("data-site"),
      ).toBe("DemoSite");
    });
    await waitFor(() => {
      expect(screen.getByTestId("architecture-nav-tree")).toBeTruthy();
    });
  });

  it("role gate redirects non-designer/non-admin to home", async () => {
    bootstrapState.isAdmin = false;
    bootstrapState.isDesigner = false;
    renderAt("/architecture");
    await waitFor(() => {
      expect(screen.getByTestId("home-redirect")).toBeTruthy();
    });
    expect(screen.queryByTestId("perc-architecture-shell")).toBeNull();
  });
});
