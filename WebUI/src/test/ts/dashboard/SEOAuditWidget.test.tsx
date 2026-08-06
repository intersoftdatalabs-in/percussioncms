/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { SEOAuditWidget } from "@/dashboard/SEOAuditWidget";
import * as api from "@/api/dashboard/deliveryGadgetsApi";

vi.mock("@/api/dashboard/deliveryGadgetsApi", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("@/api/dashboard/deliveryGadgetsApi")>();
  return { ...actual, fetchNonSeoPages: vi.fn() };
});

describe("SEOAuditWidget", () => {
  beforeEach(() => {
    vi.mocked(api.fetchNonSeoPages).mockReset();
  });

  it("lists non-SEO pages", async () => {
    vi.mocked(api.fetchNonSeoPages).mockResolvedValue([
      {
        path: "/Sites/Demo/x",
        pageName: "X",
        severity: 2,
        issues: ["MISSING_DESCRIPTION"],
      },
    ]);
    render(<SEOAuditWidget refreshInterval={0} />);
    await waitFor(() => {
      expect(screen.getByTestId("seo-audit-list")).toBeDefined();
    });
    expect(screen.getByText("X")).toBeDefined();
  });
});
