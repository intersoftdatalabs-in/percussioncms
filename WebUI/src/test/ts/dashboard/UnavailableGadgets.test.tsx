/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { BulkUploadWidget } from "@/dashboard/BulkUploadWidget";
import { ReportsWidget } from "@/dashboard/ReportsWidget";
import { WidgetConfigurationWidget } from "@/dashboard/WidgetConfigurationWidget";

describe("unavailable gadget shells", () => {
  it("BulkUpload explains no job API", () => {
    render(<BulkUploadWidget />);
    expect(screen.getByTestId("bulk-upload-widget")).toBeDefined();
    expect(screen.getByText(/Not available in React Home/i)).toBeDefined();
  });

  it("Reports explains no catalog API", () => {
    render(<ReportsWidget />);
    expect(screen.getByTestId("reports-widget")).toBeDefined();
  });

  it("Widget configuration points at host add/remove", () => {
    render(<WidgetConfigurationWidget />);
    expect(screen.getByTestId("widget-configuration-widget")).toBeDefined();
    expect(screen.getByText(/Add \/ Remove Gadget/i)).toBeDefined();
  });
});
