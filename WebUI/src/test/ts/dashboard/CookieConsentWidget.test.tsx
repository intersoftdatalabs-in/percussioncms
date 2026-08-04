/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { CookieConsentWidget } from "@/dashboard/CookieConsentWidget";
import * as api from "@/api/dashboard/deliveryGadgetsApi";
import { MSG } from "@/i18n/message";

vi.mock("@/api/dashboard/deliveryGadgetsApi", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("@/api/dashboard/deliveryGadgetsApi")>();
  return { ...actual, fetchCookieConsentTotals: vi.fn() };
});

describe("CookieConsentWidget", () => {
  beforeEach(() => {
    vi.mocked(api.fetchCookieConsentTotals).mockReset();
    delete (window as { I18N?: unknown }).I18N;
  });

  afterEach(() => {
    delete (window as { I18N?: unknown }).I18N;
  });

  it("localizes default title via I18N when key resolves (GH-1836)", async () => {
    (window as unknown as { I18N: { message: (k: string) => string } }).I18N = {
      message: (k: string) =>
        k === MSG.GADGET_COOKIE_CONSENT ? "कुकी सहमति" : k,
    };
    vi.mocked(api.fetchCookieConsentTotals).mockResolvedValue({
      raw: {},
      bySite: [],
      grandTotal: 0,
    });
    render(<CookieConsentWidget refreshInterval={0} />);
    await waitFor(() => {
      expect(screen.getByTestId("cookie-consent-widget")).toBeDefined();
    });
    const titleEl = screen
      .getByTestId("cookie-consent-widget")
      .querySelector("div");
    expect(titleEl?.textContent).toBe("कुकी सहमति");
    expect(titleEl?.textContent).not.toBe("COOKIE CONSENT");
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
