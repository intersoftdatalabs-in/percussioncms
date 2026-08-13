/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import * as contentTypesApi from "../../../main/ts/api/developer/contentTypesApi";
import { DEV_MSG } from "../../../main/ts/developer/messages";
import { ContentTypeDetailPanel } from "../../../main/ts/developer/ContentTypeDetailPanel";

vi.mock("../../../main/ts/api/developer/contentTypesApi", () => ({
  getContentTypeDetail: vi.fn(),
  updateContentTypeDetail: vi.fn(),
}));

// ObjectAclSection loads ACL via separate API; stub so detail success path stays isolated.
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

const getContentTypeDetail = contentTypesApi.getContentTypeDetail as ReturnType<typeof vi.fn>;
const updateContentTypeDetail = contentTypesApi.updateContentTypeDetail as ReturnType<
  typeof vi.fn
>;

const sampleDetail = {
  name: "percPage",
  label: "Page",
  description: "Page type",
  enabled: true,
  hideFromMenu: false,
  appName: "rx_cm",
  guid: { stringValue: "0-2-301" },
  fields: [],
  allowedWorkflows: [],
  allowedTemplates: [],
  designGaps: [] as Array<string | { code?: string; message?: string }>,
};

describe("ContentTypeDetailPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    getContentTypeDetail.mockReset();
    updateContentTypeDetail.mockReset();
  });

  it("loads detail on success and supports back", async () => {
    getContentTypeDetail.mockResolvedValue(sampleDetail);
    const onBack = vi.fn();
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={onBack} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-detail-title")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ct-detail-title").textContent).toContain("Page");
    expect(screen.getByTestId("developer-ct-fields-table")).toBeTruthy();
    expect(screen.getByTestId("developer-ct-wf-empty")).toBeTruthy();
    expect(screen.getByTestId("developer-ct-tpl-empty")).toBeTruthy();
    const back = screen.getByTestId("developer-ct-back");
    expect(back.getAttribute("aria-label")).toBe("Back to list");
    fireEvent.click(back);
    expect(onBack).toHaveBeenCalled();
  });

  it("shows empty workflows and templates when detail has none", async () => {
    getContentTypeDetail.mockResolvedValue({
      ...sampleDetail,
      allowedWorkflows: [],
      allowedTemplates: [],
    });
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-wf-empty")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ct-tpl-empty")).toBeTruthy();
  });

  it("shows session-redirect message via panelErrMsg", async () => {
    getContentTypeDetail.mockRejectedValue(new SessionRedirectError());
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ct-detail-error").textContent).toBe(
      DEV_MSG.SESSION_REDIRECT,
    );
    expect(screen.queryByTestId("developer-ct-detail-loading")).toBeNull();
    expect(screen.queryByTestId("developer-ct-detail-title")).toBeNull();
  });

  it("shows ApiError status via panelErrMsg", async () => {
    getContentTypeDetail.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
      body: null,
    });
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ct-detail-error").textContent).toBe(
      `${DEV_MSG.CT_DETAIL_ERROR} (500)`,
    );
  });

  it("shows Error.message via panelErrMsg", async () => {
    getContentTypeDetail.mockRejectedValue(new Error("network down"));
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ct-detail-error").textContent).toBe(
      `${DEV_MSG.CT_DETAIL_ERROR} network down`,
    );
    expect(screen.queryByTestId("developer-ct-detail-title")).toBeNull();
  });

  it("shows fallback when rejection has no message", async () => {
    getContentTypeDetail.mockRejectedValue("boom");
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ct-detail-error").textContent).toBe(
      DEV_MSG.CT_DETAIL_ERROR,
    );
  });

  it("mounts ObjectAclSection with content-type kind and object guid (#3319)", async () => {
    getContentTypeDetail.mockResolvedValue(sampleDetail);
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-acl-stub")).toBeTruthy();
    });
    const acl = screen.getByTestId("developer-ct-acl-stub");
    expect(acl.getAttribute("data-object-kind")).toBe("content-type");
    expect(acl.getAttribute("data-object-guid")).toBe("0-2-301");
    expect(screen.getByTestId("developer-ct-detail-guid").textContent).toBe("0-2-301");
  });

  it("uses catalogGuid fallback when detail guid has no stringValue (#3319)", async () => {
    getContentTypeDetail.mockResolvedValue({
      ...sampleDetail,
      guid: undefined,
    });
    render(
      <ContentTypeDetailPanel
        idOrName="percPage"
        catalogGuid="0-2-9"
        onBack={() => undefined}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-acl-stub")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ct-detail-guid").textContent).toBe("0-2-9");
    expect(screen.getByTestId("developer-ct-acl-stub").getAttribute("data-object-guid")).toBe(
      "0-2-9",
    );
  });

  it("uses guidString when nested guid is absent (#3319)", async () => {
    getContentTypeDetail.mockResolvedValue({
      ...sampleDetail,
      guid: undefined,
      guidString: "0-2-88",
    });
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-acl-stub")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ct-detail-guid").textContent).toBe("0-2-88");
    expect(screen.getByTestId("developer-ct-acl-stub").getAttribute("data-object-guid")).toBe(
      "0-2-88",
    );
  });

  it("synthesizes object guid from host/type/uuid parts on detail (#3319)", async () => {
    getContentTypeDetail.mockResolvedValue({
      ...sampleDetail,
      guid: { hostId: 0, type: 2, uuid: 301 },
    });
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-acl-stub")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ct-detail-guid").textContent).toBe("0-2-301");
    expect(screen.getByTestId("developer-ct-acl-stub").getAttribute("data-object-guid")).toBe(
      "0-2-301",
    );
  });

  it("passes empty guid to ObjectAclSection when none can be resolved (#3319)", async () => {
    getContentTypeDetail.mockResolvedValue({
      ...sampleDetail,
      guid: undefined,
      guidString: undefined,
    });
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-acl-stub")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ct-detail-guid").textContent).toBe("—");
    expect(screen.getByTestId("developer-ct-acl-stub").getAttribute("data-object-guid")).toBe("");
  });

  it("renders structured designGaps message with data-gap-code (REST-GAPS-01)", async () => {
    getContentTypeDetail.mockResolvedValue({
      ...sampleDetail,
      designGaps: [
        { code: "CT_ITEM_EXITS", message: "Item-level pre/post exits not exposed" },
        "legacy free-text gap",
      ],
    });
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-gaps")).toBeTruthy();
    });
    const gaps = screen.getByTestId("developer-ct-gaps");
    expect(gaps.textContent).toContain("Item-level pre/post exits not exposed");
    expect(gaps.textContent).toContain("legacy free-text gap");
    const coded = gaps.querySelector('[data-gap-code="CT_ITEM_EXITS"]');
    expect(coded).toBeTruthy();
    expect(coded?.textContent).toBe("Item-level pre/post exits not exposed");
  });
});
