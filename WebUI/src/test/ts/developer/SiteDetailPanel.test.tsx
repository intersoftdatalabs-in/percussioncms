/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { SiteDef } from "../../../main/ts/api/developer/types";
import { DEV_MSG } from "../../../main/ts/developer/messages";
import { SiteDetailPanel } from "../../../main/ts/developer/SiteDetailPanel";

// ObjectAclSection loads ACL via separate API; stub to isolate detail render + assert wiring.
vi.mock("../../../main/ts/developer/ObjectAclSection", () => ({
  ObjectAclSection: (props: {
    objectGuid?: string | null;
    objectKind?: string | null;
    testIdPrefix?: string;
  }) => (
    <div
      data-testid={`${props.testIdPrefix ?? "developer-acl"}-stub`}
      data-object-guid={props.objectGuid ?? ""}
      data-object-kind={props.objectKind ?? ""}
    />
  ),
}));

/**
 * SiteDetailPanel is prop-driven from the Sites list payload (no separate detail GET).
 * panelErrMsg ladders for load failures live on SitesPanel; this suite covers success +
 * default design-gaps empty fallback for the detail view itself.
 */
const sampleSite: SiteDef = {
  name: "Corporate",
  description: "Main site",
  baseUrl: "https://example.com",
  siteProtocol: "https",
  defaultDocument: "index.html",
  defaultFileExtention: "html",
  pageBasedSite: true,
  guid: { stringValue: "0-10-1" },
  designGaps: ["gap-a"],
};

describe("SiteDetailPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
  });

  it("renders site detail from list payload and supports back", () => {
    const onBack = vi.fn();
    render(<SiteDetailPanel site={sampleSite} onBack={onBack} />);
    expect(screen.getByTestId("developer-site-detail")).toBeTruthy();
    expect(screen.getByTestId("developer-site-detail-title").textContent).toContain("Corporate");
    expect(screen.getByTestId("developer-site-gaps").textContent).toContain("gap-a");
    expect(screen.getByText("https://example.com")).toBeTruthy();
    const acl = screen.getByTestId("developer-site-acl-stub");
    expect(acl.getAttribute("data-object-kind")).toBe("site");
    expect(acl.getAttribute("data-object-guid")).toBe("0-10-1");
    const back = screen.getByTestId("developer-site-back");
    expect(back.getAttribute("aria-label")).toBe("Back to list");
    fireEvent.click(back);
    expect(onBack).toHaveBeenCalled();
  });

  it("shows default design gaps when site has none", () => {
    const site: SiteDef = {
      name: "Bare",
      description: "",
      designGaps: [],
    };
    render(<SiteDetailPanel site={site} onBack={() => undefined} />);
    const gaps = screen.getByTestId("developer-site-gaps");
    expect(gaps.textContent).toContain(DEV_MSG.SITE_GAP_WRITE);
    expect(gaps.textContent).toContain(DEV_MSG.SITE_GAP_PUBLISH);
    expect(gaps.textContent).toContain(DEV_MSG.SITE_GAP_WF);
  });

  it("renders em-dash placeholders for missing optional fields", () => {
    const site: SiteDef = {
      name: "Minimal",
    };
    render(<SiteDetailPanel site={site} onBack={() => undefined} />);
    expect(screen.getByTestId("developer-site-detail-title").textContent).toContain("Minimal");
    // description / url / protocol empty → "—" in the meta grid
    const detail = screen.getByTestId("developer-site-detail");
    expect(detail.textContent).toMatch(/—/);
    expect(screen.getByTestId("developer-site-gaps").textContent).toContain(
      DEV_MSG.SITE_GAP_WRITE,
    );
  });
});
