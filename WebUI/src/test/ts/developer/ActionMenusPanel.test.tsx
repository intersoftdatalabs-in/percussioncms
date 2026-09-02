/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import {
  deleteActionMenu,
  getActionMenuDetail,
  listActionMenus,
} from "../../../main/ts/api/developer/actionMenusApi";
import { ActionMenusPanel } from "../../../main/ts/developer/ActionMenusPanel";
import { DEV_MSG } from "../../../main/ts/developer/messages";

vi.mock("../../../main/ts/api/developer/actionMenusApi", async (importOriginal) => {
  const actual = await importOriginal<
    typeof import("../../../main/ts/api/developer/actionMenusApi")
  >();
  return {
    ...actual,
    listActionMenus: vi.fn(),
    getActionMenuDetail: vi.fn(),
    createActionMenu: vi.fn(),
    saveActionMenu: vi.fn(),
    deleteActionMenu: vi.fn(),
  };
});

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

const listMock = vi.mocked(listActionMenus);
const detailMock = vi.mocked(getActionMenuDetail);
const deleteMock = vi.mocked(deleteActionMenu);

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
    listMock.mockReset();
    detailMock.mockReset();
    deleteMock.mockReset();
  });

  it("lists action menus and opens detail", async () => {
    listMock.mockResolvedValue([sampleMenu]);
    detailMock.mockResolvedValue(sampleDetail);
    render(<ActionMenusPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-am-table")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-am-table").textContent).toContain("Edit");
    expect(screen.getByTestId("developer-am-new")).toBeTruthy();
    fireEvent.click(screen.getByTestId("developer-am-open"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-am-detail")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-am-params-table")).toBeTruthy();
    expect(screen.getByTestId("developer-am-save")).toBeTruthy();
    expect(screen.getByTestId("developer-am-delete")).toBeTruthy();
    fireEvent.click(screen.getByTestId("developer-am-back"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-am-table")).toBeTruthy();
    });
  });

  it("opens create chrome from New action menu", async () => {
    listMock.mockResolvedValue([]);
    render(<ActionMenusPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-am-empty")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-am-new"));
    expect(screen.getByTestId("developer-am-detail")).toBeTruthy();
    expect(screen.getByTestId("developer-am-save")).toBeDisabled();
    expect(detailMock).not.toHaveBeenCalled();
  });

  it("shows empty state when API returns no action menus", async () => {
    listMock.mockResolvedValue([]);
    render(<ActionMenusPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-am-empty")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-am-new")).toBeTruthy();
  });

  it("shows session-redirect message via panelErrMsg", async () => {
    listMock.mockRejectedValue(new SessionRedirectError());
    render(<ActionMenusPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-am-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-am-error").textContent).toBe(DEV_MSG.SESSION_REDIRECT);
    expect(screen.queryByTestId("developer-am-empty")).toBeNull();
  });

  it("shows ApiError status via panelErrMsg", async () => {
    listMock.mockRejectedValue({
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
    listMock.mockRejectedValue(new Error("network down"));
    render(<ActionMenusPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-am-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-am-error").textContent).toBe(
      `${DEV_MSG.AM_ERROR} network down`,
    );
    expect(screen.queryByTestId("developer-am-table")).toBeNull();
  });

  it("shows deleted notice on the catalog after delete", async () => {
    listMock.mockResolvedValue([sampleMenu]);
    detailMock.mockResolvedValue(sampleDetail);
    deleteMock.mockResolvedValue(undefined);
      render(<ActionMenusPanel />);
      await waitFor(() => {
        expect(screen.getByTestId("developer-am-table")).toBeTruthy();
      });
      fireEvent.click(screen.getByTestId("developer-am-open"));
      await waitFor(() => {
        expect(screen.getByTestId("developer-am-delete")).toBeTruthy();
      });
      fireEvent.click(screen.getByTestId("developer-am-delete"));
      fireEvent.click(screen.getByTestId("developer-catalog-confirm-submit"));
      await waitFor(() => {
        expect(screen.getByTestId("developer-am-list-notice")).toBeTruthy();
      });
      expect(screen.getByTestId("developer-am-list-notice").textContent).toBe(DEV_MSG.AM_DELETED);
      expect(screen.getByTestId("developer-am-panel")).toBeTruthy();
  });

  it("shows fallback when rejection has no message", async () => {
    listMock.mockRejectedValue("boom");
    render(<ActionMenusPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-am-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-am-error").textContent).toBe(DEV_MSG.AM_ERROR);
  });
});
