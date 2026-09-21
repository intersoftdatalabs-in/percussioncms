/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { SiteDef } from "../../../main/ts/api/developer/types";
import * as sitesApi from "../../../main/ts/api/developer/sitesApi";
import { DEV_MSG } from "../../../main/ts/developer/messages";
import { SiteDetailPanel } from "../../../main/ts/developer/SiteDetailPanel";

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

vi.mock("../../../main/ts/developer/VirtualSiteSourcePanel", () => ({
  VirtualSiteSourcePanel: (props: { siteName: string }) => (
    <div data-testid="developer-site-virtual-stub" data-site-name={props.siteName} />
  ),
}));

vi.mock("../../../main/ts/api/developer/sitesApi", async () => {
  const actual = await vi.importActual<typeof sitesApi>(
    "../../../main/ts/api/developer/sitesApi",
  );
  return {
    ...actual,
    updateSite: vi.fn(),
    deleteSite: vi.fn(),
  };
});

const updateSite = sitesApi.updateSite as ReturnType<typeof vi.fn>;
const deleteSite = sitesApi.deleteSite as ReturnType<typeof vi.fn>;

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
    updateSite.mockReset();
    deleteSite.mockReset();
  });

  it("renders site detail from list payload and supports back", () => {
    const onBack = vi.fn();
    render(<SiteDetailPanel site={sampleSite} onBack={onBack} />);
    expect(screen.getByTestId("developer-site-detail")).toBeTruthy();
    expect(screen.getByTestId("developer-site-detail-title").textContent).toContain("Corporate");
    expect(screen.getByTestId("developer-site-gaps").textContent).toContain("gap-a");
    expect(
      (screen.getByTestId("developer-site-base-url-input") as HTMLInputElement).value,
    ).toBe("https://example.com");
    const acl = screen.getByTestId("developer-site-acl-stub");
    expect(acl.getAttribute("data-object-kind")).toBe("site");
    expect(acl.getAttribute("data-object-guid")).toBe("0-10-1");
    expect(screen.getByTestId("developer-site-detail-guid").textContent).toBe("0-10-1");
    const virt = screen.getByTestId("developer-site-virtual-stub");
    expect(virt.getAttribute("data-site-name")).toBe("Corporate");
    const back = screen.getByTestId("developer-site-back");
    expect(back.getAttribute("aria-label")).toBe("Back to list");
    fireEvent.click(back);
    expect(onBack).toHaveBeenCalled();
  });

  it("shows remaining design gaps when site has none", () => {
    const site: SiteDef = {
      name: "Bare",
      description: "",
      designGaps: [],
    };
    render(<SiteDetailPanel site={site} onBack={() => undefined} />);
    const gaps = screen.getByTestId("developer-site-gaps");
    expect(gaps.textContent).toContain(DEV_MSG.SITE_GAP_PUBLISH);
    expect(gaps.textContent).toContain(DEV_MSG.SITE_GAP_WF);
    expect(gaps.textContent).not.toContain("not supported from this Developer surface");
  });

  it("renders em-dash placeholders for missing optional fields", () => {
    const site: SiteDef = {
      name: "Minimal",
    };
    render(<SiteDetailPanel site={site} onBack={() => undefined} />);
    expect(screen.getByTestId("developer-site-detail-title").textContent).toContain("Minimal");
    const detail = screen.getByTestId("developer-site-detail");
    expect(detail.textContent).toMatch(/—/);
  });

  it("synthesizes object guid from host/type/uuid parts for Object ACL (#3203)", () => {
    const site: SiteDef = {
      name: "PartsOnly",
      guid: { hostId: 0, type: 20, uuid: 99 },
    };
    render(<SiteDetailPanel site={site} onBack={() => undefined} />);
    expect(screen.getByTestId("developer-site-detail-guid").textContent).toBe("0-20-99");
    expect(screen.getByTestId("developer-site-acl-stub").getAttribute("data-object-guid")).toBe(
      "0-20-99",
    );
  });

  it("omits object guid when the list payload has none (#3203)", () => {
    const site: SiteDef = { name: "NoGuid" };
    render(<SiteDetailPanel site={site} onBack={() => undefined} />);
    expect(screen.getByTestId("developer-site-detail-guid").textContent).toBe("—");
    expect(screen.getByTestId("developer-site-acl-stub").getAttribute("data-object-guid")).toBe(
      "",
    );
  });

  it("saves description and base URL", async () => {
    updateSite.mockResolvedValue({
      name: "Corporate",
      description: "New",
      baseUrl: "https://n.example",
    });
    render(<SiteDetailPanel site={sampleSite} onBack={() => undefined} />);
    fireEvent.change(screen.getByTestId("developer-site-description-input"), {
      target: { value: "New" },
    });
    fireEvent.change(screen.getByTestId("developer-site-base-url-input"), {
      target: { value: "https://n.example" },
    });
    fireEvent.click(screen.getByTestId("developer-site-save"));
    await waitFor(() => {
      expect(updateSite).toHaveBeenCalledWith("Corporate", {
        name: "Corporate",
        description: "New",
        baseUrl: "https://n.example",
      });
    });
    expect(screen.getByTestId("developer-site-save-notice").textContent).toBe(
      DEV_MSG.SITE_SAVED,
    );
  });

  it("confirms delete then calls API", async () => {
    deleteSite.mockResolvedValue(undefined);
    const onDeleted = vi.fn();
    render(
      <SiteDetailPanel site={sampleSite} onBack={() => undefined} onDeleted={onDeleted} />,
    );
    fireEvent.click(screen.getByTestId("developer-site-delete"));
    expect(deleteSite).not.toHaveBeenCalled();
    fireEvent.click(screen.getByTestId("developer-catalog-confirm-submit"));
    await waitFor(() => {
      expect(deleteSite).toHaveBeenCalledWith("Corporate");
    });
    expect(onDeleted).toHaveBeenCalled();
  });
});
