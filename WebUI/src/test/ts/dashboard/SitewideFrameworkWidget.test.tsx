/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { SitewideFrameworkWidget } from "@/dashboard/SitewideFrameworkWidget";

describe("SitewideFrameworkWidget", () => {
  it("shows not-available shell", () => {
    render(<SitewideFrameworkWidget />);
    expect(screen.getByTestId("sitewide-framework-widget")).toBeDefined();
    expect(screen.getByText(/Not available in React Home/i)).toBeDefined();
  });
});
