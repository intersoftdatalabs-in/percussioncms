/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { EffectivenessWidget } from "@/dashboard/EffectivenessWidget";
import * as gadgetApi from "@/api/dashboard/gadgetApi";
import * as analyticsApi from "@/api/dashboard/analyticsApi";

vi.mock("@/api/dashboard/gadgetApi", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("@/api/dashboard/gadgetApi")>();
  return {
    ...actual,
    fetchDefaultEffectiveness: vi.fn(),
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

describe("EffectivenessWidget", () => {
  beforeEach(() => {
    vi.mocked(gadgetApi.fetchDefaultEffectiveness).mockReset();
    vi.mocked(analyticsApi.isAnalyticsProviderConfigured)
      .mockReset()
      .mockResolvedValue(true);
  });

  it("prompts for Google Setup when analytics missing", async () => {
    vi.mocked(analyticsApi.isAnalyticsProviderConfigured).mockResolvedValue(
      false,
    );
    render(<EffectivenessWidget refreshInterval={0} />);
    await waitFor(() => {
      expect(
        screen.getByTestId("effectiveness-widget-needs-analytics"),
      ).toBeDefined();
    });
    expect(gadgetApi.fetchDefaultEffectiveness).not.toHaveBeenCalled();
    expect(screen.getByText(/Google Setup/i)).toBeDefined();
  });

  it("lists effectiveness rows when configured", async () => {
    vi.mocked(gadgetApi.fetchDefaultEffectiveness).mockResolvedValue({
      path: "/Sites/Demo",
      rows: [
        { name: "Section A", effectiveness: 42 },
        { name: "Section B", effectiveness: 10 },
      ],
    });
    render(<EffectivenessWidget refreshInterval={0} />);
    await waitFor(() => {
      expect(screen.getByTestId("effectiveness-widget-list")).toBeDefined();
    });
    expect(screen.getByText("Section A")).toBeDefined();
    expect(screen.getByText("42")).toBeDefined();
  });

  it("shows error when load fails", async () => {
    vi.mocked(gadgetApi.fetchDefaultEffectiveness).mockRejectedValue(
      new Error("eff fail"),
    );
    render(<EffectivenessWidget refreshInterval={0} />);
    await waitFor(() => {
      expect(screen.getByTestId("effectiveness-widget-error")).toBeDefined();
    });
  });
});
