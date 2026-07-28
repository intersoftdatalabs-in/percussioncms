/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { GoogleSetupWidget } from "@/dashboard/GoogleSetupWidget";
import * as analyticsApi from "@/api/dashboard/analyticsApi";

vi.mock("@/api/dashboard/analyticsApi", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("@/api/dashboard/analyticsApi")>();
  return {
    ...actual,
    fetchGoogleSetupSummary: vi.fn(),
    deleteAnalyticsProviderConfig: vi.fn(),
  };
});

describe("GoogleSetupWidget", () => {
  beforeEach(() => {
    vi.mocked(analyticsApi.fetchGoogleSetupSummary).mockReset();
  });

  it("shows not configured state", async () => {
    vi.mocked(analyticsApi.fetchGoogleSetupSummary).mockResolvedValue({
      provider: { configured: false, userId: null, siteProfiles: [] },
      sites: [{ siteName: "Demo", profileConfigured: false }],
    });
    render(<GoogleSetupWidget />);
    await waitFor(() => {
      expect(screen.getByTestId("google-setup-content")).toBeDefined();
    });
    expect(
      screen.getByText(/Analytics provider not configured/i),
    ).toBeDefined();
    expect(screen.getByTestId("google-setup-hint")).toBeDefined();
    expect(screen.getByText(/Traffic and What's Working/i)).toBeDefined();
  });

  it("shows configured account and site readiness", async () => {
    vi.mocked(analyticsApi.fetchGoogleSetupSummary).mockResolvedValue({
      provider: {
        configured: true,
        userId: "svc@example.iam.gserviceaccount.com",
        siteProfiles: [
          {
            siteName: "Demo",
            mapped: true,
            profileId: "p1",
            webPropertyId: "G-1",
          },
        ],
      },
      sites: [
        {
          siteName: "Demo",
          profileConfigured: true,
          mapping: {
            siteName: "Demo",
            mapped: true,
            profileId: "p1",
            webPropertyId: "G-1",
          },
        },
      ],
    });
    render(<GoogleSetupWidget />);
    await waitFor(() => {
      expect(screen.getByTestId("google-setup-userid")).toBeDefined();
    });
    expect(screen.getByText(/Analytics provider configured/i)).toBeDefined();
    expect(
      screen.getByText(/svc@example.iam.gserviceaccount.com/),
    ).toBeDefined();
    expect(screen.getByText(/1\/1 ready/)).toBeDefined();
    expect(screen.getByText(/Ready · G-1/)).toBeDefined();
  });

  it("shows error on failure", async () => {
    vi.mocked(analyticsApi.fetchGoogleSetupSummary).mockRejectedValue(
      new Error("analytics down"),
    );
    render(<GoogleSetupWidget />);
    await waitFor(() => {
      expect(screen.getByTestId("google-setup-error")).toBeDefined();
    });
    expect(screen.getByText(/analytics down/i)).toBeDefined();
  });
});
