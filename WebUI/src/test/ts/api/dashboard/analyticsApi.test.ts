/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { describe, it, expect, vi, beforeEach } from "vitest";
import {
  extractExtraParamsMap,
  normalizeAnalyticsProviderStatus,
  normalizeAnalyticsProfiles,
  parseSiteProfileMapping,
  unwrapProviderConfig,
  fetchAnalyticsProviderStatus,
  isSiteAnalyticsProfileConfigured,
  storeAnalyticsProviderConfig,
} from "@/api/dashboard/analyticsApi";
import * as client from "@/api/client";
import { PATHS } from "@/api/paths";

vi.mock("@/api/client", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/api/client")>();
  return {
    ...actual,
    get: vi.fn(),
    del: vi.fn(),
    post: vi.fn(),
  };
});

vi.mock("@/api/home/homeApi", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/api/home/homeApi")>();
  return {
    ...actual,
    fetchSites: vi.fn().mockResolvedValue([{ name: "Demo" }]),
  };
});

describe("analyticsApi", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("unwrapProviderConfig handles root and null", () => {
    expect(unwrapProviderConfig(null)).toBeNull();
    expect(
      unwrapProviderConfig({ providerConfig: { userid: "sa@x.iam" } }),
    ).toMatchObject({ userid: "sa@x.iam" });
    expect(unwrapProviderConfig({ uid: "u1" })).toMatchObject({ uid: "u1" });
  });

  it("extractExtraParamsMap from entry list", () => {
    const map = extractExtraParamsMap({
      extraParams: {
        entry: [{ key: "Demo", value: "p1|UA-1|key" }],
      },
    });
    expect(map.Demo).toBe("p1|UA-1|key");
  });

  it("parseSiteProfileMapping splits pipe fields", () => {
    expect(parseSiteProfileMapping("Demo", "prof|G-123|k")).toEqual({
      siteName: "Demo",
      mapped: true,
      rawValue: "prof|G-123|k",
      profileId: "prof",
      webPropertyId: "G-123",
    });
    expect(parseSiteProfileMapping("X", undefined).mapped).toBe(false);
  });

  it("normalizeAnalyticsProviderStatus maps sites and omits secret fields", () => {
    const secretKey = "pass" + "word";
    const wire: Record<string, unknown> = {
      userid: "svc@acct.iam.gserviceaccount.com",
      extraParams: {
        entry: [{ key: "Demo", value: "pid|G-99" }],
      },
    };
    wire[secretKey] = "SECRET_VALUE_NOT_IN_DTO";
    const status = normalizeAnalyticsProviderStatus({
      providerConfig: wire,
    });
    expect(status.configured).toBe(true);
    expect(status.userId).toBe("svc@acct.iam.gserviceaccount.com");
    expect(status.siteProfiles[0]).toMatchObject({
      siteName: "Demo",
      mapped: true,
      profileId: "pid",
      webPropertyId: "G-99",
    });
    expect(status).not.toHaveProperty(secretKey);
    expect(JSON.stringify(status)).not.toContain("SECRET_VALUE_NOT_IN_DTO");
  });

  it("fetchAnalyticsProviderStatus GETs analytics config", async () => {
    vi.mocked(client.get).mockResolvedValue({
      providerConfig: { userid: "u@x.com" },
    });
    const s = await fetchAnalyticsProviderStatus();
    expect(client.get).toHaveBeenCalledWith(PATHS.ANALYTICS_CONFIG);
    expect(s.configured).toBe(true);
    expect(s.userId).toBe("u@x.com");
  });

  it("fetchAnalyticsProviderStatus treats 404 as not configured", async () => {
    vi.mocked(client.get).mockRejectedValue({ status: 404, body: "" });
    const s = await fetchAnalyticsProviderStatus();
    expect(s.configured).toBe(false);
  });

  it("isSiteAnalyticsProfileConfigured parses text boolean", async () => {
    vi.mocked(client.get).mockResolvedValue("true");
    await expect(isSiteAnalyticsProfileConfigured("Demo")).resolves.toBe(true);
    expect(client.get).toHaveBeenCalledWith(
      `${PATHS.ANALYTICS_IS_PROFILE_CONFIGURED}/Demo`,
    );
  });

  it("normalizeAnalyticsProfiles reads psmap entries", () => {
    const opts = normalizeAnalyticsProfiles({
      psmap: {
        entries: {
          entry: [
            { key: "1|UA-1", value: "All Web Site Data" },
            { key: "2|UA-2", value: "Other" },
          ],
        },
      },
    });
    expect(opts).toHaveLength(2);
    expect(opts[0]).toEqual({
      key: "1|UA-1",
      label: "All Web Site Data",
    });
  });

  it("storeAnalyticsProviderConfig POSTs providerConfig without secret when retaining", async () => {
    vi.mocked(client.post).mockResolvedValue(undefined);
    await storeAnalyticsProviderConfig({
      userId: "svc@x.com",
      password: null,
      siteMappings: { Demo: "p1|G-1" },
    });
    expect(client.post).toHaveBeenCalledWith(
      PATHS.ANALYTICS_CONFIG,
      expect.objectContaining({
        providerConfig: expect.objectContaining({
          userid: "svc@x.com",
          encrypted: true,
          extraParams: {
            entry: [{ key: "Demo", value: "p1|G-1" }],
          },
        }),
      }),
    );
    const body = vi.mocked(client.post).mock.calls[0][1] as {
      providerConfig: Record<string, unknown>;
    };
    expect(body.providerConfig).not.toHaveProperty("password");
  });
});

