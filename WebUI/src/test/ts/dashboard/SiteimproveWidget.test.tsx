/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { SiteimproveWidget } from "@/dashboard/SiteimproveWidget";
import * as api from "@/api/dashboard/deliveryGadgetsApi";

vi.mock("@/api/dashboard/deliveryGadgetsApi", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("@/api/dashboard/deliveryGadgetsApi")>();
  return { ...actual, fetchSiteimproveStatus: vi.fn() };
});

describe("SiteimproveWidget", () => {
  beforeEach(() => {
    vi.mocked(api.fetchSiteimproveStatus).mockReset();
  });

  it("shows token status", async () => {
    vi.mocked(api.fetchSiteimproveStatus).mockResolvedValue({
      hasToken: true,
      tokenPreview: "abc…",
      siteConfigPresent: false,
      siteName: "Demo",
    });
    render(<SiteimproveWidget refreshInterval={0} />);
    await waitFor(() => {
      expect(screen.getByTestId("siteimprove-status")).toBeDefined();
    });
    expect(screen.getByText(/Configured/i)).toBeDefined();
  });
});
