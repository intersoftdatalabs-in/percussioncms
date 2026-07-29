/*
 * Copyright 1999-2026 Percussion Software, Inc.
 *
 * Shell gadgets are now fully implemented; this file keeps a light smoke.
 */

import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { BulkUploadWidget } from "@/dashboard/BulkUploadWidget";

describe("completed former shell gadgets", () => {
  it("BulkUpload is interactive (not unavailable shell)", () => {
    render(<BulkUploadWidget />);
    expect(screen.getByTestId("bulk-upload-folder")).toBeDefined();
    expect(screen.queryByText(/Not available in React Home/i)).toBeNull();
  });
});
