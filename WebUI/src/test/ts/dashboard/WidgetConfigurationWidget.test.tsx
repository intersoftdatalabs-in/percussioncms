/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { describe, it, expect, beforeEach } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { WidgetConfigurationWidget } from "@/dashboard/WidgetConfigurationWidget";
import { PREFERRED_GADGETS_STORAGE_KEY } from "@/dashboard/gadgetsCatalog";

describe("WidgetConfigurationWidget", () => {
  beforeEach(() => {
    sessionStorage.clear();
  });

  it("lists gadgets and applies layout", () => {
    render(<WidgetConfigurationWidget />);
    expect(screen.getByTestId("widget-configuration-list")).toBeDefined();
    expect(screen.getByTestId("widget-config-cb-blogs")).toBeDefined();
    fireEvent.click(screen.getByTestId("widget-configuration-apply"));
    const raw = sessionStorage.getItem(PREFERRED_GADGETS_STORAGE_KEY);
    expect(raw).toBeTruthy();
    expect(JSON.parse(raw as string)).toContain("blogs");
    expect(screen.getByTestId("widget-configuration-message")).toBeDefined();
  });
});
