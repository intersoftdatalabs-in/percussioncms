/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { WidgetConfigurationWidget } from "@/dashboard/WidgetConfigurationWidget";

describe("WidgetConfigurationWidget", () => {
  it("points at host add/remove", () => {
    render(<WidgetConfigurationWidget />);
    expect(screen.getByTestId("widget-configuration-widget")).toBeDefined();
    expect(screen.getByText(/Add \/ Remove Gadget/i)).toBeDefined();
  });
});
