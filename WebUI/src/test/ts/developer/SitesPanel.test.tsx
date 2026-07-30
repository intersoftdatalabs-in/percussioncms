/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SitesPanel } from "../../../main/ts/developer/SitesPanel";
import * as sitesApi from "../../../main/ts/api/developer/sitesApi";

vi.mock("../../../main/ts/api/developer/sitesApi", () => ({
  listSites: vi.fn(),
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

  it("shows error without treating as empty", async () => {
    listSites.mockRejectedValue(new Error("sites down"));
    render(<SitesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-error")).toBeTruthy();
    });
    expect(screen.queryByTestId("developer-site-empty")).toBeNull();
  });
});
