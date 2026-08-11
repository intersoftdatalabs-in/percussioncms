/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import * as actionMenusApi from "../../../main/ts/api/developer/actionMenusApi";
import { ActionMenuDetailPanel } from "../../../main/ts/developer/ActionMenuDetailPanel";
import { DEV_MSG } from "../../../main/ts/developer/messages";

vi.mock("../../../main/ts/api/developer/actionMenusApi", () => ({
  listActionMenus: vi.fn(),
  getActionMenuDetail: vi.fn(),
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

const getActionMenuDetail = actionMenusApi.getActionMenuDetail as ReturnType<typeof vi.fn>;

const sampleDetail = {
  id: 1,
  name: "Edit",
  label: "Edit Item",
  description: "Edit content item",
  menuType: "MENUITEM",
  handler: "CLIENT",
  url: "/Rhythmyx/edit",
  sortRank: 10,
  guid: { stringValue: "0-11-42" },
  parameters: [{ name: "sys_contentid", value: "0" }],
  properties: [{ name: "AcceleratorKey", value: "E" }],
};

describe("ActionMenuDetailPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    getActionMenuDetail.mockReset();
  });

  it("loads detail on success and supports back", async () => {
    getActionMenuDetail.mockResolvedValue(sampleDetail);
    const onBack = vi.fn();
    render(<ActionMenuDetailPanel idOrName="Edit" onBack={onBack} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-am-detail-title")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-am-detail-title").textContent).toContain("Edit Item");
    expect(screen.getByTestId("developer-am-params-table")).toBeTruthy();
    expect(screen.getByTestId("developer-am-props-table")).toBeTruthy();
    const acl = screen.getByTestId("developer-am-acl-stub");
    expect(acl.getAttribute("data-object-kind")).toBe("action-menu");
    expect(acl.getAttribute("data-object-guid")).toBe("0-11-42");
    expect(screen.getByTestId("developer-am-gaps")).toBeTruthy();
    expect(getActionMenuDetail).toHaveBeenCalledWith("Edit");
    const back = screen.getByTestId("developer-am-back");
    expect(back.getAttribute("aria-label")).toBe("Back to action menus list");
    fireEvent.click(back);
    expect(onBack).toHaveBeenCalled();
  });

  it("shows empty params and props sections when detail has none", async () => {
    getActionMenuDetail.mockResolvedValue({
      ...sampleDetail,
      parameters: [],
      properties: [],
    });
    render(<ActionMenuDetailPanel idOrName="Edit" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-am-params-empty")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-am-props-empty")).toBeTruthy();
    expect(screen.queryByTestId("developer-am-params-table")).toBeNull();
    expect(screen.queryByTestId("developer-am-props-table")).toBeNull();
  });

  it("shows session-redirect message via panelErrMsg", async () => {
    getActionMenuDetail.mockRejectedValue(new SessionRedirectError());
    render(<ActionMenuDetailPanel idOrName="Edit" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-am-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-am-detail-error").textContent).toBe(
      DEV_MSG.SESSION_REDIRECT,
    );
    expect(screen.queryByTestId("developer-am-detail-loading")).toBeNull();
    expect(screen.queryByTestId("developer-am-detail-title")).toBeNull();
  });

  it("shows ApiError status via panelErrMsg", async () => {
    getActionMenuDetail.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
      body: null,
    });
    render(<ActionMenuDetailPanel idOrName="Edit" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-am-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-am-detail-error").textContent).toBe(
      `${DEV_MSG.AM_DETAIL_ERROR} (500)`,
    );
  });

  it("shows Error.message via panelErrMsg", async () => {
    getActionMenuDetail.mockRejectedValue(new Error("network down"));
    render(<ActionMenuDetailPanel idOrName="Edit" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-am-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-am-detail-error").textContent).toBe(
      `${DEV_MSG.AM_DETAIL_ERROR} network down`,
    );
    expect(screen.queryByTestId("developer-am-detail-title")).toBeNull();
  });

  it("shows fallback when rejection has no message", async () => {
    getActionMenuDetail.mockRejectedValue("boom");
    render(<ActionMenuDetailPanel idOrName="Edit" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-am-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-am-detail-error").textContent).toBe(
      DEV_MSG.AM_DETAIL_ERROR,
    );
  });
});
