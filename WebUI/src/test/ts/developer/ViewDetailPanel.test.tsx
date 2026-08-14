/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import * as viewsApi from "../../../main/ts/api/developer/viewsApi";
import { DEV_MSG } from "../../../main/ts/developer/messages";
import { ViewDetailPanel } from "../../../main/ts/developer/ViewDetailPanel";

vi.mock("../../../main/ts/api/developer/viewsApi", () => ({
  listViews: vi.fn(),
  getViewDetail: vi.fn(),
}));

// ObjectAclSection loads ACL via separate API; stub to isolate detail load + assert wiring.
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

const getViewDetail = viewsApi.getViewDetail as ReturnType<typeof vi.fn>;

const sampleDetail = {
  name: "My View",
  label: "My View",
  description: "Custom view",
  displayFormatId: "Default",
  maximumResultSize: 50,
  caseSensitive: true,
  guid: { stringValue: "0-27-3" },
  fields: [{ fieldName: "sys_contentid", operator: "=", fieldValue: "1", fieldType: "number" }],
  designGaps: ["gap-a"],
};

describe("ViewDetailPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    getViewDetail.mockReset();
  });

  it("loads detail on success and supports back", async () => {
    getViewDetail.mockResolvedValue(sampleDetail);
    const onBack = vi.fn();
    render(<ViewDetailPanel idOrName="My View" onBack={onBack} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-vw-detail-title")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-vw-detail-title").textContent).toContain("My View");
    expect(screen.getByTestId("developer-vw-fields-table")).toBeTruthy();
    expect(screen.getByTestId("developer-vw-gaps").textContent).toContain("gap-a");
    expect(getViewDetail).toHaveBeenCalledWith("My View");
    const acl = screen.getByTestId("developer-vw-acl-stub");
    expect(acl.getAttribute("data-object-kind")).toBe("view");
    expect(acl.getAttribute("data-object-guid")).toBe("0-27-3");
    expect(screen.getByTestId("developer-vw-detail-guid").textContent).toBe("0-27-3");
    fireEvent.click(screen.getByTestId("developer-vw-back"));
    expect(onBack).toHaveBeenCalled();
  });

  it("uses catalogGuid fallback when detail guid has no stringValue (#3380)", async () => {
    getViewDetail.mockResolvedValue({
      ...sampleDetail,
      guid: undefined,
      id: undefined,
    });
    render(
      <ViewDetailPanel idOrName="My View" catalogGuid="0-18-5" onBack={() => undefined} />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("developer-vw-acl-stub")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-vw-detail-guid").textContent).toBe("0-18-5");
    expect(screen.getByTestId("developer-vw-acl-stub").getAttribute("data-object-guid")).toBe(
      "0-18-5",
    );
  });

  it("uses guidString when nested guid is absent (#3380)", async () => {
    getViewDetail.mockResolvedValue({
      ...sampleDetail,
      guid: undefined,
      guidString: "0-18-9",
    });
    render(<ViewDetailPanel idOrName="My View" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-vw-acl-stub")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-vw-detail-guid").textContent).toBe("0-18-9");
    expect(screen.getByTestId("developer-vw-acl-stub").getAttribute("data-object-guid")).toBe(
      "0-18-9",
    );
  });

  it("synthesizes GUID from view id when guid is omitted (#3380)", async () => {
    getViewDetail.mockResolvedValue({
      ...sampleDetail,
      guid: undefined,
      guidString: undefined,
      id: 12,
    });
    render(<ViewDetailPanel idOrName="My View" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-vw-acl-stub")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-vw-detail-guid").textContent).toBe("0-18-12");
    expect(screen.getByTestId("developer-vw-acl-stub").getAttribute("data-object-guid")).toBe(
      "0-18-12",
    );
  });

  it("passes empty guid so Object ACL can show no-GUID message (#3380)", async () => {
    getViewDetail.mockResolvedValue({
      name: "My View",
      label: "My View",
    });
    render(<ViewDetailPanel idOrName="My View" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-vw-acl-stub")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-vw-detail-guid").textContent).toBe("—");
    expect(screen.getByTestId("developer-vw-acl-stub").getAttribute("data-object-guid")).toBe("");
  });

  it("shows empty fields section when detail has none", async () => {
    getViewDetail.mockResolvedValue({ ...sampleDetail, fields: [] });
    render(<ViewDetailPanel idOrName="My View" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-vw-fields-empty")).toBeTruthy();
    });
    expect(screen.queryByTestId("developer-vw-fields-table")).toBeNull();
  });

  it("shows session-redirect message via panelErrMsg", async () => {
    getViewDetail.mockRejectedValue(new SessionRedirectError());
    render(<ViewDetailPanel idOrName="My View" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-vw-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-vw-detail-error").textContent).toBe(
      DEV_MSG.SESSION_REDIRECT,
    );
    expect(screen.queryByTestId("developer-vw-detail-loading")).toBeNull();
    expect(screen.queryByTestId("developer-vw-detail-title")).toBeNull();
  });

  it("shows ApiError status via panelErrMsg", async () => {
    getViewDetail.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
      body: null,
    });
    render(<ViewDetailPanel idOrName="My View" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-vw-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-vw-detail-error").textContent).toBe(
      `${DEV_MSG.VW_DETAIL_ERROR} (500)`,
    );
  });

  it("shows Error.message via panelErrMsg", async () => {
    getViewDetail.mockRejectedValue(new Error("network down"));
    render(<ViewDetailPanel idOrName="My View" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-vw-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-vw-detail-error").textContent).toBe(
      `${DEV_MSG.VW_DETAIL_ERROR} network down`,
    );
    expect(screen.queryByTestId("developer-vw-detail-title")).toBeNull();
  });

  it("shows fallback when rejection has no message", async () => {
    getViewDetail.mockRejectedValue("boom");
    render(<ViewDetailPanel idOrName="My View" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-vw-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-vw-detail-error").textContent).toBe(
      DEV_MSG.VW_DETAIL_ERROR,
    );
  });
});
