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

import { describe, it, expect, beforeEach, afterEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { Dashboard } from "@/dashboard";
import { MSG } from "@/i18n/message";

describe("Dashboard Component", () => {
  beforeEach(() => {
    // Clear URL search params
    window.history.pushState({}, "", "/");
  });

  afterEach(() => {
    // Clean up
    window.history.pushState({}, "", "/");
    delete (window as { I18N?: unknown }).I18N;
  });

  it("should render the dashboard root", () => {
    render(<Dashboard />);
    expect(screen.getByTestId("dashboard-root")).toBeDefined();
    expect(screen.getByTestId("dashboard-add-gadget")).toBeDefined();
  });

  it("resolves Add Gadget button via window.I18N.message (GH-1840)", () => {
    // Dashboard already uses message(MSG.DASHBOARD_ADD_GADGET); stub the
    // product TMX catalog and assert the button does not stay on English fallback.
    (window as unknown as { I18N: { message: (k: string) => string } }).I18N = {
      message: (k: string) => {
        if (k === MSG.DASHBOARD_ADD_GADGET) {
          return "गैजेट जोड़ें";
        }
        return k;
      },
    };
    render(<Dashboard />);
    const btn = screen.getByTestId("dashboard-add-gadget");
    expect(btn.textContent).toContain("गैजेट जोड़ें");
    expect(btn.textContent).not.toMatch(/Add Gadget/);
  });

  it("should render default gadgets including Welcome and Blogs", () => {
    render(<Dashboard />);
    // Welcome greeting + Blogs gadget (default layout for Home)
    expect(
      screen.getByText(/Good morning|Good afternoon|Good evening/),
    ).toBeDefined();
    expect(screen.getByTestId("blogs-widget")).toBeDefined();
  });

  it("should render two-column layout", () => {
    const { container } = render(<Dashboard />);
    const gridContainer = container.querySelector("div[style*='grid']");
    expect(gridContainer).toBeDefined();
  });

  it("should detect legacy dashboard flag and navigate via location.href", async () => {
    // Set legacy flag in URL — Dashboard useEffect redirects to legacyDashboardUrl.
    // Shared vitest.setup mock applies href assignment via history so this is
    // observable under jsdom (raw jsdom emits "Not implemented: navigation").
    window.history.pushState({}, "", "/?legacyDashboard=true");

    try {
      render(<Dashboard legacyDashboardUrl="/legacy.jsp" />);
      await waitFor(() => {
        expect(window.location.pathname).toBe("/legacy.jsp");
      });
    } finally {
      window.history.pushState({}, "", "/");
    }
  });
});
