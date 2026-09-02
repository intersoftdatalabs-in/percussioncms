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

vi.mock("../../../main/ts/api/developer/viewsApi", async (importOriginal) => {
  const actual = await importOriginal<typeof import("../../../main/ts/api/developer/viewsApi")>();
  return {
    ...actual,
    listViews: vi.fn(),
    getViewDetail: vi.fn(),
    createView: vi.fn(),
    saveView: vi.fn(),
    deleteView: vi.fn(),
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

const getViewDetail = viewsApi.getViewDetail as ReturnType<typeof vi.fn>;
const createView = viewsApi.createView as ReturnType<typeof vi.fn>;
const saveView = viewsApi.saveView as ReturnType<typeof vi.fn>;
const deleteView = viewsApi.deleteView as ReturnType<typeof vi.fn>;

const sampleDetail = {
  name: "My View",
  label: "My View",
  description: "Custom view",
  type: "View",
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
    createView.mockReset();
    saveView.mockReset();
    deleteView.mockReset();
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

  it("disables save until the name is valid on create", () => {
    render(<ViewDetailPanel idOrName={null} onBack={() => undefined} />);
    const save = screen.getByTestId("developer-vw-save") as HTMLButtonElement;
    expect(save.disabled).toBe(true);
    fireEvent.change(screen.getByTestId("developer-vw-name"), {
      target: { value: "has space" },
    });
    expect(save.disabled).toBe(true);
    fireEvent.change(screen.getByTestId("developer-vw-name"), {
      target: { value: "MyView" },
    });
    expect(save.disabled).toBe(false);
  });

  it("keeps name read-only on edit", async () => {
    getViewDetail.mockResolvedValue(sampleDetail);
    render(<ViewDetailPanel idOrName="My View" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-vw-name")).toBeTruthy();
    });
    expect((screen.getByTestId("developer-vw-name") as HTMLInputElement).disabled).toBe(true);
  });

  it("surfaces 400 invalid name on create", async () => {
    createView.mockRejectedValue({
      status: 400,
      statusText: "Bad Request",
      body: { message: "name cannot contain whitespace" },
    });
    const onSaved = vi.fn();
    render(<ViewDetailPanel idOrName={null} onBack={() => undefined} onSaved={onSaved} />);
    fireEvent.change(screen.getByTestId("developer-vw-name"), {
      target: { value: "MyView" },
    });
    fireEvent.click(screen.getByTestId("developer-vw-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-vw-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-vw-detail-error").textContent).toContain(
      DEV_MSG.VW_INVALID_NAME,
    );
    expect(screen.getByTestId("developer-vw-detail-error").textContent).toContain(
      "name cannot contain whitespace",
    );
    expect(onSaved).not.toHaveBeenCalled();
  });

  it("surfaces 409 duplicate name on create", async () => {
    createView.mockRejectedValue({
      status: 409,
      statusText: "Conflict",
      body: { message: "View already exists: MyView" },
    });
    const onSaved = vi.fn();
    render(<ViewDetailPanel idOrName={null} onBack={() => undefined} onSaved={onSaved} />);
    fireEvent.change(screen.getByTestId("developer-vw-name"), {
      target: { value: "MyView" },
    });
    fireEvent.click(screen.getByTestId("developer-vw-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-vw-detail-error")).toBeTruthy();
    });
    expect(createView).toHaveBeenCalled();
    expect(screen.getByTestId("developer-vw-detail-error").textContent).toContain(
      DEV_MSG.VW_DUPLICATE,
    );
    expect(screen.getByTestId("developer-vw-detail-error").textContent).toContain(
      "already exists",
    );
    expect(onSaved).not.toHaveBeenCalled();
  });

  it("surfaces 403 non-Admin on create", async () => {
    createView.mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: { message: "Admin role required" },
    });
    render(<ViewDetailPanel idOrName={null} onBack={() => undefined} />);
    fireEvent.change(screen.getByTestId("developer-vw-name"), {
      target: { value: "MyView" },
    });
    fireEvent.click(screen.getByTestId("developer-vw-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-vw-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-vw-detail-error").textContent).toContain(
      DEV_MSG.VW_FORBIDDEN,
    );
    expect(screen.getByTestId("developer-vw-detail-error").textContent).toContain(
      "Admin role required",
    );
  });

  it("does not POST create twice when save is clicked twice", async () => {
    let resolveCreate: (value: typeof sampleDetail) => void = () => undefined;
    createView.mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveCreate = resolve;
        }),
    );
    render(<ViewDetailPanel idOrName={null} onBack={() => undefined} />);
    fireEvent.change(screen.getByTestId("developer-vw-name"), {
      target: { value: "MyView" },
    });
    fireEvent.click(screen.getByTestId("developer-vw-save"));
    fireEvent.click(screen.getByTestId("developer-vw-save"));
    expect(createView).toHaveBeenCalledTimes(1);
    resolveCreate({
      name: "MyView",
      label: "My View",
      description: "",
      type: "View",
      fields: [],
    });
    await waitFor(() => {
      expect(screen.getByTestId("developer-vw-editor-notice")).toBeTruthy();
    });
  });

  it("creates a standard view when the name is valid", async () => {
    createView.mockResolvedValue({
      name: "MyView",
      label: "My View",
      description: "Created via SPA",
      type: "View",
      displayFormatId: "1",
      fields: [],
    });
    const onSaved = vi.fn();
    render(<ViewDetailPanel idOrName={null} onBack={() => undefined} onSaved={onSaved} />);
    fireEvent.change(screen.getByTestId("developer-vw-name"), {
      target: { value: "MyView" },
    });
    fireEvent.change(screen.getByTestId("developer-vw-label"), {
      target: { value: "My View" },
    });
    fireEvent.change(screen.getByTestId("developer-vw-description"), {
      target: { value: "Created via SPA" },
    });
    fireEvent.change(screen.getByTestId("developer-vw-display-format"), {
      target: { value: "1" },
    });
    fireEvent.click(screen.getByTestId("developer-vw-save"));
    await waitFor(() => {
      expect(onSaved).toHaveBeenCalled();
    });
    expect(createView).toHaveBeenCalledWith(
      expect.objectContaining({
        name: "MyView",
        label: "My View",
        description: "Created via SPA",
        type: "View",
        displayFormatId: "1",
      }),
    );
    expect(screen.getByTestId("developer-vw-editor-notice").textContent).toBe(DEV_MSG.VW_SAVED);
  });

  it("saves label changes on an existing view", async () => {
    getViewDetail.mockResolvedValue(sampleDetail);
    saveView.mockResolvedValue({
      ...sampleDetail,
      label: "Updated label",
    });
    const onSaved = vi.fn();
    render(
      <ViewDetailPanel idOrName="My View" onBack={() => undefined} onSaved={onSaved} />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("developer-vw-label")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-vw-label"), {
      target: { value: "Updated label" },
    });
    fireEvent.click(screen.getByTestId("developer-vw-save"));
    await waitFor(() => {
      expect(onSaved).toHaveBeenCalled();
    });
    expect(saveView).toHaveBeenCalledWith(
      "0-27-3",
      expect.objectContaining({
        name: "My View",
        label: "Updated label",
        description: "Custom view",
        type: "View",
        displayFormatId: "Default",
      }),
    );
  });

  it("deletes after confirm and omits delete chrome in create mode", async () => {
    getViewDetail.mockResolvedValue(sampleDetail);
    deleteView.mockResolvedValue(undefined);
      const onDeleted = vi.fn();
      render(
        <ViewDetailPanel idOrName="My View" onBack={() => undefined} onDeleted={onDeleted} />,
      );
      await waitFor(() => {
        expect(screen.getByTestId("developer-vw-delete")).toBeTruthy();
      });
      fireEvent.click(screen.getByTestId("developer-vw-delete"));
      fireEvent.click(screen.getByTestId("developer-catalog-confirm-submit"));
      await waitFor(() => {
        expect(onDeleted).toHaveBeenCalled();
      });
      expect(deleteView).toHaveBeenCalledWith("0-27-3");
  });

  it("surfaces 404 missing view on delete", async () => {
    getViewDetail.mockResolvedValue(sampleDetail);
    deleteView.mockRejectedValue({
      status: 404,
      statusText: "Not Found",
      body: { message: "View not found" },
    });
      render(<ViewDetailPanel idOrName="My View" onBack={() => undefined} />);
      await waitFor(() => {
        expect(screen.getByTestId("developer-vw-delete")).toBeTruthy();
      });
      fireEvent.click(screen.getByTestId("developer-vw-delete"));
      fireEvent.click(screen.getByTestId("developer-catalog-confirm-submit"));
      await waitFor(() => {
        expect(screen.getByTestId("developer-vw-detail-error")).toBeTruthy();
      });
      expect(screen.getByTestId("developer-vw-detail-error").textContent).toContain(
        DEV_MSG.VW_NOT_FOUND,
      );
  });

  it("does not show delete on create", () => {
    render(<ViewDetailPanel idOrName={null} onBack={() => undefined} />);
    expect(screen.queryByTestId("developer-vw-delete")).toBeNull();
  });

  it("does not delete Inbox-family or custom URL views", async () => {
    getViewDetail.mockResolvedValue({
      ...sampleDetail,
      name: "Inbox",
      customView: true,
      url: "../sys_cxViews/inbox.xml",
    });
    render(<ViewDetailPanel idOrName="Inbox" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-vw-protected-hint")).toBeTruthy();
    });
    expect(screen.queryByTestId("developer-vw-delete")).toBeNull();
    expect((screen.getByTestId("developer-vw-save") as HTMLButtonElement).disabled).toBe(true);
    expect(deleteView).not.toHaveBeenCalled();
  });

  it("saves PUT body fields on a writable view", async () => {
    getViewDetail.mockResolvedValue(sampleDetail);
    saveView.mockResolvedValue({
      ...sampleDetail,
      fields: [
        sampleDetail.fields[0],
        { fieldName: "sys_title", operator: "like", fieldValue: "News%", fieldType: "Text" },
      ],
    });
    render(<ViewDetailPanel idOrName="My View" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-vw-field-editor")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-vw-field-source"), {
      target: { value: "sys_title" },
    });
    fireEvent.change(screen.getByTestId("developer-vw-field-add-op"), {
      target: { value: "like" },
    });
    fireEvent.change(screen.getByTestId("developer-vw-field-add-value"), {
      target: { value: "News%" },
    });
    fireEvent.click(screen.getByTestId("developer-vw-field-add"));
    fireEvent.click(screen.getByTestId("developer-vw-fields-save"));
    await waitFor(() => {
      expect(saveView).toHaveBeenCalled();
    });
    const [, body] = saveView.mock.calls[0];
    expect(body.fields.map((f: { fieldName?: string }) => f.fieldName)).toEqual([
      "sys_contentid",
      "sys_title",
    ]);
    expect(body.fields[1]).toEqual(
      expect.objectContaining({
        fieldName: "sys_title",
        operator: "like",
        fieldValue: "News%",
      }),
    );
    await waitFor(() => {
      expect(screen.getByTestId("developer-vw-editor-notice").textContent).toBe(
        DEV_MSG.VW_FIELDS_SAVED,
      );
    });
  });

  it("surfaces 400 invalid field from PUT", async () => {
    getViewDetail.mockResolvedValue(sampleDetail);
    saveView.mockRejectedValue({
      status: 400,
      statusText: "Bad Request",
      body: "unknown field: not_a_cx_field",
    });
    render(<ViewDetailPanel idOrName="My View" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-vw-field-editor")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-vw-field-source"), {
      target: { value: "sys_title" },
    });
    fireEvent.click(screen.getByTestId("developer-vw-field-add"));
    fireEvent.click(screen.getByTestId("developer-vw-fields-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-vw-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-vw-detail-error").textContent).toContain(
      DEV_MSG.VW_FIELDS_INVALID,
    );
    expect(screen.getByTestId("developer-vw-detail-error").textContent).toContain("unknown field");
  });

  it("surfaces 403 non-Admin from field PUT", async () => {
    getViewDetail.mockResolvedValue(sampleDetail);
    saveView.mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: "Admin role required",
    });
    render(<ViewDetailPanel idOrName="My View" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-vw-field-editor")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-vw-field-source"), {
      target: { value: "sys_title" },
    });
    fireEvent.click(screen.getByTestId("developer-vw-field-add"));
    fireEvent.click(screen.getByTestId("developer-vw-fields-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-vw-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-vw-detail-error").textContent).toContain(
      DEV_MSG.VW_FIELDS_FORBIDDEN,
    );
  });

  it("does not mutate Inbox-family or system views from the field editor", async () => {
    getViewDetail.mockResolvedValue({
      ...sampleDetail,
      name: "Inbox",
      customView: true,
      url: "../sys_cxViews/inbox.xml",
    });
    render(<ViewDetailPanel idOrName="Inbox" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-vw-fields-readonly")).toBeTruthy();
    });
    expect(screen.queryByTestId("developer-vw-field-editor")).toBeNull();
    expect(screen.queryByTestId("developer-vw-fields-save")).toBeNull();
    expect(saveView).not.toHaveBeenCalled();
  });
});
