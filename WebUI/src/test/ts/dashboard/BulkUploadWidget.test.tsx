/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { BulkUploadWidget } from "@/dashboard/BulkUploadWidget";

describe("BulkUploadWidget", () => {
  it("shows not-available shell", () => {
    render(<BulkUploadWidget />);
    expect(screen.getByTestId("bulk-upload-widget")).toBeDefined();
    expect(screen.getByText(/Not available in React Home/i)).toBeDefined();
  });
});
