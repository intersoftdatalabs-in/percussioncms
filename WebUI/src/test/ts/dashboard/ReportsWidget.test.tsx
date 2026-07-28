/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { ReportsWidget } from "@/dashboard/ReportsWidget";

describe("ReportsWidget", () => {
  it("shows not-available shell", () => {
    render(<ReportsWidget />);
    expect(screen.getByTestId("reports-widget")).toBeDefined();
    expect(screen.getByText(/Not available in React Home/i)).toBeDefined();
  });
});
