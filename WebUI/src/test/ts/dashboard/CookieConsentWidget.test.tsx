/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { CookieConsentWidget } from "@/dashboard/CookieConsentWidget";
import * as api from "@/api/dashboard/deliveryGadgetsApi";

vi.mock("@/api/dashboard/deliveryGadgetsApi", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("@/api/dashboard/deliveryGadgetsApi")>();
  return { ...actual, fetchCookieConsentTotals: vi.fn() };
});

describe("CookieConsentWidget", () => {
  beforeEach(() => {
    vi.mocked(api.fetchCookieConsentTotals).mockReset();
  });

  it("lists totals by site", async () => {
    vi.mocked(api.fetchCookieConsentTotals).mockResolvedValue({
      raw: { Demo: 5 },
      bySite: [{ site: "Demo", total: 5 }],
      grandTotal: 5,
    });
    render(<CookieConsentWidget refreshInterval={0} />);
    await waitFor(() => {
      expect(screen.getByTestId("cookie-consent-list")).toBeDefined();
    });
    expect(screen.getByText("Demo")).toBeDefined();
  });
});
