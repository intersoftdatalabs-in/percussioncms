/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import { GoogleSetupWidget } from "@/dashboard/GoogleSetupWidget";
import * as analyticsApi from "@/api/dashboard/analyticsApi";
import { MSG } from "@/i18n/message";

vi.mock("@/api/dashboard/analyticsApi", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("@/api/dashboard/analyticsApi")>();
  return {
    ...actual,
    fetchGoogleSetupSummary: vi.fn(),
    fetchAnalyticsProfiles: vi.fn(),
    deleteAnalyticsProviderConfig: vi.fn(),
    testAnalyticsConnection: vi.fn(),
    saveAnalyticsSiteMappings: vi.fn(),
  };
});

describe("GoogleSetupWidget", () => {
  beforeEach(() => {
    vi.mocked(analyticsApi.fetchGoogleSetupSummary).mockReset();
    vi.mocked(analyticsApi.fetchAnalyticsProfiles).mockReset();
    vi.mocked(analyticsApi.testAnalyticsConnection).mockReset();
    vi.mocked(analyticsApi.saveAnalyticsSiteMappings).mockReset();
    delete (window as { I18N?: unknown }).I18N;
  });

  afterEach(() => {
    delete (window as { I18N?: unknown }).I18N;
  });

  it("localizes default title via I18N when key resolves (GH-1833)", async () => {
    (window as unknown as { I18N: { message: (k: string) => string } }).I18N = {
      message: (k: string) =>
        k === MSG.GADGET_GOOGLE_SETUP ? "गूगल सेटअप" : k,
    };
    vi.mocked(analyticsApi.fetchGoogleSetupSummary).mockResolvedValue({
      provider: { configured: false, userId: null, siteProfiles: [] },
      sites: [],
    });
    render(<GoogleSetupWidget />);
    await waitFor(() => {
      expect(screen.getByTestId("google-setup-content")).toBeDefined();
    });
    const titleEl = screen
      .getByTestId("google-setup-widget")
      .querySelector("div");
    expect(titleEl?.textContent).toBe("गूगल सेटअप");
    expect(titleEl?.textContent).not.toBe("Google Setup");
  });

  it("shows not configured and configure form", async () => {
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
    expect(screen.getByTestId("google-setup-configure")).toBeDefined();
    expect(screen.getByTestId("google-setup-uid")).toBeDefined();
  });

  it("shows configured account and profile dropdowns", async () => {
    vi.mocked(analyticsApi.fetchGoogleSetupSummary).mockResolvedValue({
      provider: {
        configured: true,
        userId: "svc@example.iam.gserviceaccount.com",
        siteProfiles: [
          {
            siteName: "Demo",
            mapped: true,
            rawValue: "p1|G-1",
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
            rawValue: "p1|G-1",
            profileId: "p1",
            webPropertyId: "G-1",
          },
        },
      ],
    });
    vi.mocked(analyticsApi.fetchAnalyticsProfiles).mockResolvedValue([
      { key: "p1|G-1", label: "All Web Site Data" },
      { key: "p2|G-2", label: "Other" },
    ]);
    render(<GoogleSetupWidget />);
    await waitFor(() => {
      expect(screen.getByTestId("google-setup-userid")).toBeDefined();
    });
    expect(screen.getByText(/Analytics provider configured/i)).toBeDefined();
    expect(screen.getByTestId("google-setup-map-Demo")).toBeDefined();
    expect(screen.getByTestId("google-setup-save-maps")).toBeDefined();
  });

  it("uploads key and tests connection", async () => {
    vi.mocked(analyticsApi.fetchGoogleSetupSummary).mockResolvedValue({
      provider: { configured: false, userId: null, siteProfiles: [] },
      sites: [],
    });
    vi.mocked(analyticsApi.testAnalyticsConnection).mockResolvedValue();
    render(<GoogleSetupWidget />);
    await waitFor(() => screen.getByTestId("google-setup-uid"));
    fireEvent.change(screen.getByTestId("google-setup-uid"), {
      target: { value: "svc@x.iam" },
    });
    const file = new File([JSON.stringify({ type: "service_account" })], "k.json", {
      type: "application/json",
    });
    fireEvent.change(screen.getByTestId("google-setup-keyfile"), {
      target: { files: [file] },
    });
    fireEvent.click(screen.getByTestId("google-setup-test"));
    await waitFor(() => {
      expect(analyticsApi.testAnalyticsConnection).toHaveBeenCalled();
    });
  });

  it("shows error on failure", async () => {
    vi.mocked(analyticsApi.fetchGoogleSetupSummary).mockRejectedValue(
      new Error("analytics down"),
    );
    render(<GoogleSetupWidget />);
    await waitFor(() => {
      expect(screen.getByTestId("google-setup-error")).toBeDefined();
    });
  });
});
