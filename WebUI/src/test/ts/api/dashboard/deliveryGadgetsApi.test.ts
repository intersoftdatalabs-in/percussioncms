/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { describe, it, expect, vi, beforeEach } from "vitest";
import {
  fetchPagesWithComments,
  fetchCookieConsentTotals,
  fetchMembershipUsers,
  fetchNonSeoPages,
  fetchSiteimproveStatus,
  normalizePageCommentsSummary,
  normalizeSeoPageRow,
} from "@/api/dashboard/deliveryGadgetsApi";
import * as client from "@/api/client";
import { PATHS } from "@/api/paths";

vi.mock("@/api/client", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/api/client")>();
  return { ...actual, get: vi.fn(), post: vi.fn() };
});

vi.mock("@/api/home/homeApi", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/api/home/homeApi")>();
  return {
    ...actual,
    fetchSites: vi.fn().mockResolvedValue([{ name: "Demo" }]),
  };
});

vi.mock("@/api/dashboard/gadgetApi", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("@/api/dashboard/gadgetApi")>();
  return {
    ...actual,
    resolveDefaultActivityPath: vi.fn().mockResolvedValue("/Sites/Demo"),
    fetchDefaultWorkflowName: vi.fn().mockResolvedValue("Default Workflow"),
  };
});

describe("deliveryGadgetsApi", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("normalizePageCommentsSummary", () => {
    expect(
      normalizePageCommentsSummary({
        id: "1",
        pageLinkTitle: "Home",
        commentCount: 3,
        newCount: 1,
        approvedCount: 2,
      }),
    ).toMatchObject({ pageLinkTitle: "Home", commentCount: 3 });
  });

  it("fetchPagesWithComments GETs delivery path", async () => {
    vi.mocked(client.get).mockResolvedValue({
      commentsSummary: [
        { id: "1", pageLinkTitle: "P", commentCount: 1, newCount: 0, approvedCount: 1 },
      ],
    });
    const pages = await fetchPagesWithComments("Demo", 5);
    expect(client.get).toHaveBeenCalledWith(
      `${PATHS.COMMENTS_PAGES_WITH_COMMENTS}/Demo?max=5`,
    );
    expect(pages).toHaveLength(1);
  });

  it("fetchCookieConsentTotals parses map", async () => {
    vi.mocked(client.get).mockResolvedValue({ Demo: 10, Other: 2 });
    const t = await fetchCookieConsentTotals();
    expect(t.grandTotal).toBe(12);
    expect(t.bySite[0].site).toBe("Demo");
  });

  it("fetchMembershipUsers GETs users path", async () => {
    vi.mocked(client.get).mockResolvedValue([
      { email: "a@b.com", status: "Active" },
    ]);
    const users = await fetchMembershipUsers("Demo");
    expect(client.get).toHaveBeenCalledWith(
      `${PATHS.MEMBERSHIP_USERS}/Demo`,
    );
    expect(users[0].email).toBe("a@b.com");
  });

  it("normalizeSeoPageRow and fetchNonSeoPages POST", async () => {
    expect(
      normalizeSeoPageRow({
        path: "/Sites/Demo/x",
        severity: 2,
        issues: ["MISSING_DESCRIPTION"],
      }),
    ).toMatchObject({ path: "/Sites/Demo/x", severity: 2 });

    vi.mocked(client.post).mockResolvedValue({
      SEOStatistics: [{ path: "/Sites/Demo/x", severity: 1, issues: [] }],
    });
    const rows = await fetchNonSeoPages({ path: "/Sites/Demo" });
    expect(client.post).toHaveBeenCalledWith(
      PATHS.PAGE_NON_SEO,
      expect.objectContaining({
        NonSEOPagesRequest: expect.objectContaining({ path: "/Sites/Demo" }),
      }),
    );
    expect(rows).toHaveLength(1);
  });

  it("fetchSiteimproveStatus checks token", async () => {
    vi.mocked(client.get).mockImplementation(async (url: string) => {
      if (url.includes("/token") && !url.includes("publish")) {
        return { data: "abc123token" };
      }
      return null;
    });
    const s = await fetchSiteimproveStatus("Demo");
    expect(s.hasToken).toBe(true);
    expect(s.siteName).toBe("Demo");
  });
});
