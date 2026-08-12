/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import * as sitesApi from "../../../main/ts/api/developer/sitesApi";
import { DEV_MSG } from "../../../main/ts/developer/messages";
import { SitesPanel } from "../../../main/ts/developer/SitesPanel";

vi.mock("../../../main/ts/api/developer/sitesApi", () => ({
  listSites: vi.fn(),
  getVirtualSiteProperties: vi.fn().mockResolvedValue({ virtual: false }),
  updateVirtualSiteProperties: vi.fn(),
  coerceDisplayString: (value: unknown) =>
    typeof value === "string" ? value.trim() : "",
  SITE_DESIGN_GAPS: ["gap-write", "gap-publish", "gap-wf"],
}));

const listSites = sitesApi.listSites as ReturnType<typeof vi.fn>;

describe("SitesPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    listSites.mockReset();
  });

  it("lists sites and opens detail from list payload", async () => {
    listSites.mockResolvedValue([
      {
        name: "Corporate",
        description: "Main site",
        baseUrl: "https://example.com",
        siteProtocol: "https",
        pageBasedSite: true,
        designGaps: ["gap-a"],
      },
    ]);
    render(<SitesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-table")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-site-table").textContent).toContain("Corporate");
    fireEvent.click(screen.getByTestId("developer-site-open"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-detail")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-site-detail-title").textContent).toContain("Corporate");
    expect(screen.getByTestId("developer-site-gaps").textContent).toContain("gap-a");
    fireEvent.click(screen.getByTestId("developer-site-back"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-table")).toBeTruthy();
    });
  });

  it("shows loading then empty", async () => {
    let resolveList!: (v: unknown) => void;
    listSites.mockReturnValue(
      new Promise((resolve) => {
        resolveList = resolve;
      }),
    );
    render(<SitesPanel />);
    expect(screen.getByTestId("developer-site-loading")).toBeTruthy();
    resolveList([]);
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-empty")).toBeTruthy();
    });
  });

  it("shows session-redirect message via panelErrMsg", async () => {
    listSites.mockRejectedValue(new SessionRedirectError());
    render(<SitesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-site-error").textContent).toBe(DEV_MSG.SESSION_REDIRECT);
    expect(screen.queryByTestId("developer-site-empty")).toBeNull();
  });

  it("shows ApiError status via panelErrMsg", async () => {
    listSites.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
      body: null,
    });
    render(<SitesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-site-error").textContent).toBe(
      `${DEV_MSG.SITE_ERROR} (500)`,
    );
  });

  it("shows Error.message via panelErrMsg", async () => {
    listSites.mockRejectedValue(new Error("sites down"));
    render(<SitesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-site-error").textContent).toBe(
      `${DEV_MSG.SITE_ERROR} sites down`,
    );
    expect(screen.queryByTestId("developer-site-empty")).toBeNull();
    expect(screen.queryByTestId("developer-site-table")).toBeNull();
  });

  it("shows fallback when rejection has no message", async () => {
    listSites.mockRejectedValue("boom");
    render(<SitesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-site-error").textContent).toBe(DEV_MSG.SITE_ERROR);
  });

  it("shows bind error when API rows have no usable name (#3198)", async () => {
    listSites.mockResolvedValue([{ description: "orphan" }]);
    render(<SitesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-site-error").textContent).toBe(DEV_MSG.SITE_BIND_ERROR);
    expect(screen.queryByTestId("developer-site-empty")).toBeNull();
  });

  it("lists a site by guid when name is missing", async () => {
    listSites.mockResolvedValue([
      { guid: { stringValue: "0-1-301" }, description: "no name" },
    ]);
    render(<SitesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-table")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-site-table").textContent).toContain("0-1-301");
    expect(screen.queryByTestId("developer-site-empty")).toBeNull();
  });
});
