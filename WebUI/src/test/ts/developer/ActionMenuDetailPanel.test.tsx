/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import {
  createActionMenu,
  deleteActionMenu,
  getActionMenuDetail,
  saveActionMenu,
} from "../../../main/ts/api/developer/actionMenusApi";
import { ActionMenuDetailPanel } from "../../../main/ts/developer/ActionMenuDetailPanel";
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

const getActionMenuDetailMock = vi.mocked(getActionMenuDetail);
const createActionMenuMock = vi.mocked(createActionMenu);
const saveActionMenuMock = vi.mocked(saveActionMenu);
const deleteActionMenuMock = vi.mocked(deleteActionMenu);

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
    getActionMenuDetailMock.mockReset();
    createActionMenuMock.mockReset();
    saveActionMenuMock.mockReset();
    deleteActionMenuMock.mockReset();
  });

  it("loads detail on success and supports back", async () => {
    getActionMenuDetailMock.mockResolvedValue(sampleDetail);
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
    expect(getActionMenuDetailMock).toHaveBeenCalledWith("Edit");
    expect(screen.getByTestId("developer-am-detail-guid").textContent).toBe("0-11-42");
    const back = screen.getByTestId("developer-am-back");
    expect(back.getAttribute("aria-label")).toBe("Back to list");
    fireEvent.click(back);
    expect(onBack).toHaveBeenCalled();
  });

  it("uses catalogGuid fallback when detail guid has no stringValue (#3380)", async () => {
    getActionMenuDetailMock.mockResolvedValue({
      ...sampleDetail,
      guid: undefined,
      id: undefined,
    });
    render(
      <ActionMenuDetailPanel
        idOrName="Edit"
        catalogGuid="0-107-5"
        onBack={() => undefined}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("developer-am-acl-stub")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-am-detail-guid").textContent).toBe("0-107-5");
    expect(screen.getByTestId("developer-am-acl-stub").getAttribute("data-object-guid")).toBe(
      "0-107-5",
    );
  });

  it("uses guidString when nested guid is absent (#3380)", async () => {
    getActionMenuDetailMock.mockResolvedValue({
      ...sampleDetail,
      guid: undefined,
      guidString: "0-107-9",
    });
    render(<ActionMenuDetailPanel idOrName="Edit" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-am-acl-stub")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-am-detail-guid").textContent).toBe("0-107-9");
    expect(screen.getByTestId("developer-am-acl-stub").getAttribute("data-object-guid")).toBe(
      "0-107-9",
    );
  });

  it("synthesizes GUID from action id when guid is omitted (#3380)", async () => {
    getActionMenuDetailMock.mockResolvedValue({
      ...sampleDetail,
      guid: undefined,
      guidString: undefined,
      id: 12,
    });
    render(<ActionMenuDetailPanel idOrName="Edit" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-am-acl-stub")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-am-detail-guid").textContent).toBe("0-107-12");
    expect(screen.getByTestId("developer-am-acl-stub").getAttribute("data-object-guid")).toBe(
      "0-107-12",
    );
  });

  it("passes empty guid so Object ACL can show no-GUID message (#3380)", async () => {
    getActionMenuDetailMock.mockResolvedValue({
      name: "Edit",
      label: "Edit",
    });
    render(<ActionMenuDetailPanel idOrName="Edit" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-am-acl-stub")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-am-detail-guid").textContent).toBe("—");
    expect(screen.getByTestId("developer-am-acl-stub").getAttribute("data-object-guid")).toBe("");
  });

  it("shows empty params and props sections when detail has none", async () => {
    getActionMenuDetailMock.mockResolvedValue({
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
    getActionMenuDetailMock.mockRejectedValue(new SessionRedirectError());
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
    getActionMenuDetailMock.mockRejectedValue({
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
    getActionMenuDetailMock.mockRejectedValue(new Error("network down"));
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
    getActionMenuDetailMock.mockRejectedValue("boom");
    render(<ActionMenuDetailPanel idOrName="Edit" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-am-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-am-detail-error").textContent).toBe(
      DEV_MSG.AM_DETAIL_ERROR,
    );
  });

  it("disables save until the name is valid on create", () => {
    render(<ActionMenuDetailPanel idOrName={null} onBack={() => undefined} />);
    const save = screen.getByTestId("developer-am-save") as HTMLButtonElement;
    expect(save.disabled).toBe(true);
    fireEvent.change(screen.getByTestId("developer-am-name"), {
      target: { value: "has space" },
    });
    expect(save.disabled).toBe(true);
    fireEvent.change(screen.getByTestId("developer-am-name"), {
      target: { value: "MyMenu" },
    });
    expect(save.disabled).toBe(false);
  });

  it("keeps name read-only on edit", async () => {
    getActionMenuDetailMock.mockResolvedValue(sampleDetail);
    render(<ActionMenuDetailPanel idOrName="Edit" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-am-name")).toBeTruthy();
    });
    expect((screen.getByTestId("developer-am-name") as HTMLInputElement).disabled).toBe(true);
  });

  it("surfaces 400 invalid name on create", async () => {
    createActionMenuMock.mockRejectedValue({
      status: 400,
      statusText: "Bad Request",
      body: { message: "name cannot contain whitespace" },
    });
    const onSaved = vi.fn();
    render(<ActionMenuDetailPanel idOrName={null} onBack={() => undefined} onSaved={onSaved} />);
    fireEvent.change(screen.getByTestId("developer-am-name"), {
      target: { value: "MyMenu" },
    });
    fireEvent.click(screen.getByTestId("developer-am-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-am-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-am-detail-error").textContent).toContain(
      DEV_MSG.AM_INVALID_NAME,
    );
    expect(onSaved).not.toHaveBeenCalled();
  });

  it("surfaces 409 duplicate name on create", async () => {
    createActionMenuMock.mockRejectedValue({
      status: 409,
      statusText: "Conflict",
      body: { message: "Action menu already exists: MyMenu" },
    });
    const onSaved = vi.fn();
    render(<ActionMenuDetailPanel idOrName={null} onBack={() => undefined} onSaved={onSaved} />);
    fireEvent.change(screen.getByTestId("developer-am-name"), {
      target: { value: "MyMenu" },
    });
    fireEvent.click(screen.getByTestId("developer-am-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-am-detail-error")).toBeTruthy();
    });
    expect(createActionMenuMock).toHaveBeenCalled();
    expect(screen.getByTestId("developer-am-detail-error").textContent).toContain(
      "Action menu already exists: MyMenu",
    );
    expect(screen.getByTestId("developer-am-detail-error").textContent).not.toContain(
      DEV_MSG.AM_SYSTEM,
    );
    expect(onSaved).not.toHaveBeenCalled();
  });

  it("surfaces 409 lock body on create instead of duplicate chrome", async () => {
    createActionMenuMock.mockRejectedValue({
      status: 409,
      statusText: "Conflict",
      body: { message: "Action menu is locked by another designer" },
    });
    const onSaved = vi.fn();
    render(<ActionMenuDetailPanel idOrName={null} onBack={() => undefined} onSaved={onSaved} />);
    fireEvent.change(screen.getByTestId("developer-am-name"), {
      target: { value: "MyMenu" },
    });
    fireEvent.click(screen.getByTestId("developer-am-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-am-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-am-detail-error").textContent).toContain(
      "Action menu is locked by another designer",
    );
    expect(screen.getByTestId("developer-am-detail-error").textContent).not.toContain(
      DEV_MSG.AM_DUPLICATE,
    );
    expect(onSaved).not.toHaveBeenCalled();
  });

  it("surfaces 403 non-Admin on create", async () => {
    createActionMenuMock.mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: { message: "Admin role required" },
    });
    render(<ActionMenuDetailPanel idOrName={null} onBack={() => undefined} />);
    fireEvent.change(screen.getByTestId("developer-am-name"), {
      target: { value: "MyMenu" },
    });
    fireEvent.click(screen.getByTestId("developer-am-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-am-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-am-detail-error").textContent).toContain(
      DEV_MSG.AM_FORBIDDEN,
    );
  });

  it("does not POST create twice when save is clicked twice", async () => {
    let resolveCreate: (value: typeof sampleDetail) => void = () => undefined;
    createActionMenuMock.mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveCreate = resolve;
        }),
    );
    render(<ActionMenuDetailPanel idOrName={null} onBack={() => undefined} />);
    fireEvent.change(screen.getByTestId("developer-am-name"), {
      target: { value: "MyMenu" },
    });
    fireEvent.click(screen.getByTestId("developer-am-save"));
    fireEvent.click(screen.getByTestId("developer-am-save"));
    expect(createActionMenuMock).toHaveBeenCalledTimes(1);
    resolveCreate({
      name: "MyMenu",
      label: "My Menu",
      description: "",
      menuType: "MENUITEM",
      parameters: [],
      properties: [],
    });
    await waitFor(() => {
      expect(screen.getByTestId("developer-am-editor-notice")).toBeTruthy();
    });
  });

  it("creates a user action menu when the name is valid", async () => {
    createActionMenuMock.mockResolvedValue({
      name: "MyMenu",
      label: "My Menu",
      description: "Created via SPA",
      menuType: "MENUITEM",
      parameters: [],
      properties: [],
    });
    const onSaved = vi.fn();
    render(<ActionMenuDetailPanel idOrName={null} onBack={() => undefined} onSaved={onSaved} />);
    fireEvent.change(screen.getByTestId("developer-am-name"), {
      target: { value: "MyMenu" },
    });
    fireEvent.change(screen.getByTestId("developer-am-label"), {
      target: { value: "My Menu" },
    });
    fireEvent.change(screen.getByTestId("developer-am-description"), {
      target: { value: "Created via SPA" },
    });
    fireEvent.click(screen.getByTestId("developer-am-save"));
    await waitFor(() => {
      expect(onSaved).toHaveBeenCalled();
    });
    expect(createActionMenuMock).toHaveBeenCalledWith(
      expect.objectContaining({
        name: "MyMenu",
        label: "My Menu",
        description: "Created via SPA",
        menuType: "MENUITEM",
      }),
    );
    expect(screen.getByTestId("developer-am-editor-notice").textContent).toBe(DEV_MSG.AM_SAVED);
  });

  it("deletes after confirm and omits delete chrome in create mode", async () => {
    getActionMenuDetailMock.mockResolvedValue(sampleDetail);
    deleteActionMenuMock.mockResolvedValue(undefined);
    const confirmSpy = vi.spyOn(window, "confirm").mockReturnValue(true);
    try {
      const onDeleted = vi.fn();
      render(
        <ActionMenuDetailPanel
          idOrName="Edit"
          onBack={() => undefined}
          onDeleted={onDeleted}
        />,
      );
      await waitFor(() => {
        expect(screen.getByTestId("developer-am-delete")).toBeTruthy();
      });
      fireEvent.click(screen.getByTestId("developer-am-delete"));
      await waitFor(() => {
        expect(onDeleted).toHaveBeenCalled();
      });
      expect(deleteActionMenuMock).toHaveBeenCalledWith("Edit");
    } finally {
      confirmSpy.mockRestore();
    }
  });

  it("surfaces 404 missing action menu on delete", async () => {
    getActionMenuDetailMock.mockResolvedValue(sampleDetail);
    deleteActionMenuMock.mockRejectedValue({
      status: 404,
      statusText: "Not Found",
      body: { message: "Action menu not found" },
    });
    const confirmSpy = vi.spyOn(window, "confirm").mockReturnValue(true);
    try {
      render(<ActionMenuDetailPanel idOrName="Edit" onBack={() => undefined} />);
      await waitFor(() => {
        expect(screen.getByTestId("developer-am-delete")).toBeTruthy();
      });
      fireEvent.click(screen.getByTestId("developer-am-delete"));
      await waitFor(() => {
        expect(screen.getByTestId("developer-am-detail-error")).toBeTruthy();
      });
      expect(screen.getByTestId("developer-am-detail-error").textContent).toContain(
        DEV_MSG.AM_NOT_FOUND,
      );
    } finally {
      confirmSpy.mockRestore();
    }
  });

  it("surfaces 409 and does not steal lock when deleting a system menu", async () => {
    getActionMenuDetailMock.mockResolvedValue(sampleDetail);
    deleteActionMenuMock.mockRejectedValue({
      status: 409,
      statusText: "Conflict",
      body: { message: "System action menus cannot be updated or deleted via this API" },
    });
    const confirmSpy = vi.spyOn(window, "confirm").mockReturnValue(true);
    try {
      const onDeleted = vi.fn();
      render(
        <ActionMenuDetailPanel
          idOrName="Edit"
          onBack={() => undefined}
          onDeleted={onDeleted}
        />,
      );
      await waitFor(() => {
        expect(screen.getByTestId("developer-am-delete")).toBeTruthy();
      });
      fireEvent.click(screen.getByTestId("developer-am-delete"));
      await waitFor(() => {
        expect(screen.getByTestId("developer-am-detail-error")).toBeTruthy();
      });
      expect(screen.getByTestId("developer-am-detail-error").textContent).toContain(
        DEV_MSG.AM_SYSTEM,
      );
      expect(onDeleted).not.toHaveBeenCalled();
    } finally {
      confirmSpy.mockRestore();
    }
  });

  it("does not show delete on create", () => {
    render(<ActionMenuDetailPanel idOrName={null} onBack={() => undefined} />);
    expect(screen.queryByTestId("developer-am-delete")).toBeNull();
  });
});
