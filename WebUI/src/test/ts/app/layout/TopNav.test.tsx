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

import React from "react";
import { cleanup, render, screen, within } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { TopNav } from "../../../../main/ts/app/layout/TopNav";

const bootstrapState = {
  isAdmin: true,
  isDesigner: true,
  isWidgetBuilderActive: true,
};

vi.mock("../../../../main/ts/app/bootstrap/BootstrapContext", () => ({
  useSpaBootstrap: () => bootstrapState,
}));

vi.mock("../../../../main/ts/app/layout/UserMenu", () => ({
  UserMenu: () => <div data-testid="user-menu-stub" />,
}));

describe("TopNav (#2702)", () => {
  beforeEach(() => {
    (window as unknown as { I18N: { message: (k: string) => string } }).I18N = {
      // Return the English fallback after @ for readable assertions
      message: (key: string) => {
        const at = key.indexOf("@");
        return at >= 0 ? key.slice(at + 1) : key;
      },
    };
    bootstrapState.isAdmin = true;
    bootstrapState.isDesigner = true;
    bootstrapState.isWidgetBuilderActive = true;
  });

  afterEach(() => {
    cleanup();
  });

  function renderNav(path = "/cm/app/home") {
    return render(
      <MemoryRouter initialEntries={[path]} basename="/cm/app">
        <TopNav />
      </MemoryRouter>,
    );
  }

  it("renders Home then Explorer, no Dashboard, single Admin", () => {
    renderNav();
    const nav = screen.getByTestId("perc-spa-topnav");
    const links = within(nav).getAllByRole("link");
    const testIds = links
      .map((el) => el.getAttribute("data-testid"))
      .filter(Boolean);

    expect(testIds[0]).toBe("nav-home");
    expect(testIds[1]).toBe("nav-explorer");
    expect(testIds).not.toContain("nav-dashboard");
    expect(testIds).not.toContain("nav-workflow");
    expect(testIds.filter((id) => id === "nav-admin")).toEqual(["nav-admin"]);
    expect(screen.getByTestId("nav-admin").textContent).toMatch(/Admin/i);
  });

  it("lands consolidated Admin on Admin tools shell path (#2784)", () => {
    renderNav();
    const admin = screen.getByTestId("nav-admin");
    // React Router NavLink href is basename-relative under MemoryRouter
    const href = admin.getAttribute("href") || "";
    expect(href === "/admin" || href.endsWith("/admin")).toBe(true);
  });

  it("Architecture is SPA NavLink to /architecture (#3094)", () => {
    renderNav();
    const arch = screen.getByTestId("nav-architecture");
    const href = arch.getAttribute("href") || "";
    expect(href === "/architecture" || href.endsWith("/architecture")).toBe(
      true,
    );
    // Must not be a full-page legacy exit
    expect(href).not.toMatch(/view=arch/);
    expect(arch.tagName.toLowerCase()).toBe("a");
  });

  it("hides Admin for non-admin", () => {
    bootstrapState.isAdmin = false;
    bootstrapState.isDesigner = false;
    renderNav();
    expect(screen.queryByTestId("nav-admin")).toBeNull();
    expect(screen.getByTestId("nav-home")).toBeTruthy();
    expect(screen.getByTestId("nav-explorer")).toBeTruthy();
  });

  it("marks Admin active on admin-tools, workflow tabs, and legacy /workflow", () => {
    const { unmount } = renderNav("/cm/app/admin/tools");
    expect(screen.getByTestId("nav-admin").getAttribute("data-nav-active")).toBe(
      "true",
    );
    unmount();
    const { unmount: unmountRoles } = renderNav("/cm/app/admin/roles");
    expect(screen.getByTestId("nav-admin").getAttribute("data-nav-active")).toBe(
      "true",
    );
    unmountRoles();
    // Legacy path still highlights Admin while redirect resolves (#3088)
    renderNav("/cm/app/workflow/roles");
    expect(screen.getByTestId("nav-admin").getAttribute("data-nav-active")).toBe(
      "true",
    );
  });
});


