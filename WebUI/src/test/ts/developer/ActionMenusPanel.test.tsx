/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import * as actionMenusApi from "../../../main/ts/api/developer/actionMenusApi";
import { ActionMenusPanel } from "../../../main/ts/developer/ActionMenusPanel";
import { DEV_MSG } from "../../../main/ts/developer/messages";

vi.mock("../../../main/ts/api/developer/actionMenusApi", () => ({
  listActionMenus: vi.fn(),
  getActionMenuDetail: vi.fn(),
}));

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

const listActionMenus = actionMenusApi.listActionMenus as ReturnType<typeof vi.fn>;
const getActionMenuDetail = actionMenusApi.getActionMenuDetail as ReturnType<typeof vi.fn>;

const sampleMenu = {
  id: 1,
  name: "Edit",
  label: "Edit Item",
  menuType: "MENUITEM",
  handler: "CLIENT",
  parameters: [{ name: "sys_contentid", value: "0" }],
  properties: [],
};

const sampleDetail = {
  id: 1,
  name: "Edit",
  label: "Edit",
  menuType: "MENUITEM",
  parameters: [{ name: "sys_contentid", value: "0" }],
  properties: [{ name: "AcceleratorKey", value: "E" }],
};

describe("ActionMenusPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    listActionMenus.mockReset();
    getActionMenuDetail.mockReset();
  });

  it("lists action menus and opens detail", async () => {
    listActionMenus.mockResolvedValue([sampleMenu]);
    getActionMenuDetail.mockResolvedValue(sampleDetail);
    render(<ActionMenusPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-am-table")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-am-table").textContent).toContain("Edit");
    fireEvent.click(screen.getByTestId("developer-am-open"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-am-detail")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-am-params-table")).toBeTruthy();
    fireEvent.click(screen.getByTestId("developer-am-back"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-am-table")).toBeTruthy();
    });
  });

  it("shows empty state when API returns no action menus", async () => {
    listActionMenus.mockResolvedValue([]);
    render(<ActionMenusPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-am-empty")).toBeTruthy();
    });
  });

  it("shows session-redirect message via panelErrMsg", async () => {
    listActionMenus.mockRejectedValue(new SessionRedirectError());
    render(<ActionMenusPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-am-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-am-error").textContent).toBe(DEV_MSG.SESSION_REDIRECT);
    expect(screen.queryByTestId("developer-am-empty")).toBeNull();
  });

  it("shows ApiError status via panelErrMsg", async () => {
    listActionMenus.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
      body: null,
    });
    render(<ActionMenusPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-am-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-am-error").textContent).toBe(`${DEV_MSG.AM_ERROR} (500)`);
  });

  it("shows Error.message via panelErrMsg", async () => {
    listActionMenus.mockRejectedValue(new Error("network down"));
    render(<ActionMenusPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-am-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-am-error").textContent).toBe(
      `${DEV_MSG.AM_ERROR} network down`,
    );
    expect(screen.queryByTestId("developer-am-table")).toBeNull();
  });

  it("shows fallback when rejection has no message", async () => {
    listActionMenus.mockRejectedValue("boom");
    render(<ActionMenusPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-am-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-am-error").textContent).toBe(DEV_MSG.AM_ERROR);
  });
});
