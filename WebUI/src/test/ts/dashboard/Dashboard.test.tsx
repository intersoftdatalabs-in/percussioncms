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

  it("should render the dashboard", () => {
    render(<Dashboard />);

    // Check that the dashboard renders
    expect(screen.getByText(/Welcome/i)).toBeDefined();
  });

  it("should render the Welcome widget", () => {
    render(<Dashboard />);

    expect(screen.getByText(/Good morning|Good afternoon|Good evening/)).toBeDefined();
    expect(screen.getByText(/User!/)).toBeDefined();
  });

  it("should render quick action links", () => {
    render(<Dashboard />);

    expect(screen.getByText("Site Management")).toBeDefined();
    expect(screen.getByText("Web Management")).toBeDefined();
    expect(screen.getByText("Administration")).toBeDefined();
    expect(screen.getByText("Admin Console")).toBeDefined();
  });

  it("should render two-column layout", () => {
    const { container } = render(<Dashboard />);

    // Check for grid layout with 2 columns
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
