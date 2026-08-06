/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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

vi.mock("@/api/client", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/api/client")>();
  return { ...actual, put: vi.fn() };
});

describe("SiteimproveWidget", () => {
  beforeEach(() => {
    vi.mocked(api.fetchSiteimproveStatus).mockReset();
  });

  it("shows token status and save form", async () => {
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
    expect(screen.getByTestId("siteimprove-token")).toBeDefined();
    expect(screen.getByTestId("siteimprove-save")).toBeDefined();
  });
});
