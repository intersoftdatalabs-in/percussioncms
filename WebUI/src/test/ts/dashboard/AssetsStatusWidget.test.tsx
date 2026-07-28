/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { AssetsStatusWidget } from "@/dashboard/AssetsStatusWidget";
import * as gadgetApi from "@/api/dashboard/gadgetApi";

vi.mock("@/api/dashboard/gadgetApi", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("@/api/dashboard/gadgetApi")>();
  return {
    ...actual,
    fetchAssetsByStatusSummary: vi.fn(),
  };
});

describe("AssetsStatusWidget", () => {
  beforeEach(() => {
    vi.mocked(gadgetApi.fetchAssetsByStatusSummary).mockReset();
  });

  it("renders asset status buckets", async () => {
    vi.mocked(gadgetApi.fetchAssetsByStatusSummary).mockResolvedValue({
      path: "/Assets",
      workflow: "Default Workflow",
      totalItems: 2,
      buckets: [
        { state: "Draft", count: 2, sampleNames: ["logo", "banner"] },
      ],
    });
    render(<AssetsStatusWidget refreshInterval={0} />);
    await waitFor(() => {
      expect(screen.getByTestId("assets-status-list")).toBeDefined();
    });
    expect(screen.getByText("Draft")).toBeDefined();
    expect(screen.getAllByText("2").length).toBeGreaterThan(0);
    expect(screen.getByText(/logo, banner/)).toBeDefined();
  });

  it("shows empty state", async () => {
    vi.mocked(gadgetApi.fetchAssetsByStatusSummary).mockResolvedValue({
      path: "/Assets",
      workflow: "Default Workflow",
      totalItems: 0,
      buckets: [],
    });
    render(<AssetsStatusWidget refreshInterval={0} />);
    await waitFor(() => {
      expect(screen.getByTestId("assets-status-empty")).toBeDefined();
    });
  });
});
