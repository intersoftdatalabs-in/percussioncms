/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { TrafficWidget } from "@/dashboard/TrafficWidget";
import * as gadgetApi from "@/api/dashboard/gadgetApi";
import * as analyticsApi from "@/api/dashboard/analyticsApi";
import { MSG } from "@/i18n/message";

vi.mock("@/api/dashboard/gadgetApi", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("@/api/dashboard/gadgetApi")>();
  return {
    ...actual,
    fetchDefaultContentTraffic: vi.fn(),
  };
});

vi.mock("@/api/dashboard/analyticsApi", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("@/api/dashboard/analyticsApi")>();
  return {
    ...actual,
    isAnalyticsProviderConfigured: vi.fn(),
  };
});

// Recharts needs layout; jsdom is fine for presence of data-testid chart wrapper
vi.mock("recharts", () => ({
  ResponsiveContainer: ({ children }: { children?: unknown }) => (
    <div data-testid="recharts-container">{children as never}</div>
  ),
  LineChart: ({ children }: { children?: unknown }) => (
    <div>{children as never}</div>
  ),
  Line: () => null,
  XAxis: () => null,
  YAxis: () => null,
  CartesianGrid: () => null,
  Tooltip: () => null,
  Legend: () => null,
}));

describe("TrafficWidget", () => {
  beforeEach(() => {
    vi.mocked(gadgetApi.fetchDefaultContentTraffic).mockReset();
    vi.mocked(analyticsApi.isAnalyticsProviderConfigured)
      .mockReset()
      .mockResolvedValue(true);
    delete (window as { I18N?: unknown }).I18N;
  });

  afterEach(() => {
    delete (window as { I18N?: unknown }).I18N;
  });

  it("localizes default title via I18N when key resolves (GH-1832)", async () => {
    (window as unknown as { I18N: { message: (k: string) => string } }).I18N = {
      message: (k: string) =>
        k === MSG.GADGET_TRAFFIC ? "ट्रैफ़िक" : k,
    };
    vi.mocked(gadgetApi.fetchDefaultContentTraffic).mockResolvedValue({
      path: "/Sites/Demo",
      site: "Demo",
      startDate: "01/01/2026",
      endDate: "01/31/2026",
      totalVisits: 0,
      totalLivePages: 0,
      points: [],
    });
    render(<TrafficWidget refreshInterval={0} />);
    await waitFor(() => {
      expect(screen.getByTestId("traffic-widget")).toBeDefined();
    });
    const titleEl = screen.getByTestId("traffic-widget").querySelector("div");
    expect(titleEl?.textContent).toBe("ट्रैफ़िक");
    expect(titleEl?.textContent).not.toBe("TRAFFIC");
  });

  it("renders traffic metrics and chart", async () => {
    vi.mocked(gadgetApi.fetchDefaultContentTraffic).mockResolvedValue({
      path: "/Sites/Demo",
      site: "Demo",
      startDate: "01/01/2026",
      endDate: "01/31/2026",
      totalVisits: 100,
      totalLivePages: 50,
      points: [
        {
          date: "01/01/2026",
          visits: 10,
          livePages: 5,
          newPages: 1,
          pageUpdates: 0,
          takeDowns: 0,
        },
      ],
    });
    render(<TrafficWidget refreshInterval={0} />);
    await waitFor(() => {
      expect(screen.getByTestId("traffic-widget-list")).toBeDefined();
    });
    expect(screen.getByText("100")).toBeDefined();
    expect(screen.getByTestId("traffic-chart")).toBeDefined();
  });

  it("shows analytics hint when provider not configured", async () => {
    vi.mocked(analyticsApi.isAnalyticsProviderConfigured).mockResolvedValue(
      false,
    );
    vi.mocked(gadgetApi.fetchDefaultContentTraffic).mockResolvedValue({
      path: "/Sites/Demo",
      startDate: "01/01/2026",
      endDate: "01/31/2026",
      totalVisits: 0,
      totalLivePages: 2,
      points: [
        {
          date: "01/01/2026",
          visits: 0,
          livePages: 2,
          newPages: 0,
          pageUpdates: 0,
          takeDowns: 0,
        },
      ],
    });
    render(<TrafficWidget refreshInterval={0} />);
    await waitFor(() => {
      expect(screen.getByTestId("traffic-analytics-hint")).toBeDefined();
    });
  });

  it("shows error on failure", async () => {
    vi.mocked(gadgetApi.fetchDefaultContentTraffic).mockRejectedValue(
      new Error("traffic fail"),
    );
    render(<TrafficWidget refreshInterval={0} />);
    await waitFor(() => {
      expect(screen.getByTestId("traffic-widget-error")).toBeDefined();
    });
  });
});
