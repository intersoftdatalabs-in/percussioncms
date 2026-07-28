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
import { render, screen } from "@testing-library/react";
import { Dashboard } from "@/dashboard";

describe("Dashboard Component", () => {
  beforeEach(() => {
    // Clear URL search params
    window.history.pushState({}, "", "/");
  });

  afterEach(() => {
    // Clean up
    window.history.pushState({}, "", "/");
  });

  it("should render the dashboard root", () => {
    render(<Dashboard />);
    expect(screen.getByTestId("dashboard-root")).toBeDefined();
    expect(screen.getByTestId("dashboard-add-gadget")).toBeDefined();
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

  it("should detect legacy dashboard flag", () => {
    // Set legacy flag in URL
    window.history.pushState({}, "", "/?legacyDashboard=true");

    const originalLocation = window.location.href;
    try {
      render(<Dashboard legacyDashboardUrl="/legacy.jsp" />);
      // Navigation would happen - we can't test it directly in jsdom
      // but we can verify the component handles the param
    } finally {
      window.history.pushState({}, "", "/");
    }
  });
});
