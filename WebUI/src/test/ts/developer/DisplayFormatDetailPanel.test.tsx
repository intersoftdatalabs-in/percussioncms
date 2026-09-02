/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import * as assemblyApi from "../../../main/ts/api/developer/assemblyApi";
import * as displayFormatsApi from "../../../main/ts/api/developer/displayFormatsApi";
import { DisplayFormatDetailPanel } from "../../../main/ts/developer/DisplayFormatDetailPanel";
import { DEV_MSG } from "../../../main/ts/developer/messages";

vi.mock("../../../main/ts/api/developer/displayFormatsApi", async (importOriginal) => {
  const actual = await importOriginal<
    typeof import("../../../main/ts/api/developer/displayFormatsApi")
  >();
  return {
    ...actual,
    listDisplayFormats: vi.fn(),
    getDisplayFormatDetail: vi.fn(),
    createDisplayFormat: vi.fn(),
    saveDisplayFormat: vi.fn(),
    updateDisplayFormat: vi.fn(),
    deleteDisplayFormat: vi.fn(),
    normalizeColumns: (c: unknown) => (Array.isArray(c) ? c : []),
  };
});

// ObjectAclSection loads ACL via separate API; stub to isolate detail load + assert wiring.
vi.mock("../../../main/ts/api/developer/assemblyApi", async (importOriginal) => {
  const actual = await importOriginal<
    typeof import("../../../main/ts/api/developer/assemblyApi")
  >();
  return {
    ...actual,
    listCommunities: vi.fn(),
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

const getDisplayFormatDetail = displayFormatsApi.getDisplayFormatDetail as ReturnType<
  typeof vi.fn
>;
const createDisplayFormat = displayFormatsApi.createDisplayFormat as ReturnType<typeof vi.fn>;
const saveDisplayFormat = displayFormatsApi.saveDisplayFormat as ReturnType<typeof vi.fn>;
const updateDisplayFormat = displayFormatsApi.updateDisplayFormat as ReturnType<typeof vi.fn>;
const deleteDisplayFormat = displayFormatsApi.deleteDisplayFormat as ReturnType<typeof vi.fn>;
const listCommunities = assemblyApi.listCommunities as ReturnType<typeof vi.fn>;

const sampleDetail = {
  name: "Default",
  label: "Default View",
  description: "System default",
  guid: { stringValue: "0-1-100" },
  validForFolder: true,
  validForViewsAndSearches: true,
  validForRelatedContent: false,
  columns: [
    { source: "sys_title", displayName: "Title", position: 0, renderType: "text", width: 200 },
  ],
};

describe("DisplayFormatDetailPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    getDisplayFormatDetail.mockReset();
    createDisplayFormat.mockReset();
    saveDisplayFormat.mockReset();
    updateDisplayFormat.mockReset();
    deleteDisplayFormat.mockReset();
    listCommunities.mockReset();
    listCommunities.mockResolvedValue([
      { id: 10, name: "Default", label: "Default", guid: { stringValue: "0-10-10", uuid: 10 } },
    ]);
  });

  it("loads detail on success and supports back", async () => {
    getDisplayFormatDetail.mockResolvedValue(sampleDetail);
    const onBack = vi.fn();
    render(<DisplayFormatDetailPanel idOrName="Default" onBack={onBack} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-df-detail-title")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-df-detail-title").textContent).toContain("Default View");
    expect(screen.getByTestId("developer-df-columns-table")).toBeTruthy();
    expect(screen.getByTestId("developer-df-gaps")).toBeTruthy();
    expect(getDisplayFormatDetail).toHaveBeenCalledWith("Default");
    fireEvent.click(screen.getByTestId("developer-df-back"));
    expect(onBack).toHaveBeenCalled();
  });

  it("mounts ObjectAclSection with display-format kind and object guid", async () => {
    getDisplayFormatDetail.mockResolvedValue(sampleDetail);
    render(<DisplayFormatDetailPanel idOrName="Default" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-df-acl-stub")).toBeTruthy();
    });
    const acl = screen.getByTestId("developer-df-acl-stub");
    expect(acl.getAttribute("data-object-kind")).toBe("display-format");
    expect(acl.getAttribute("data-object-guid")).toBe("0-1-100");
    expect(screen.getByTestId("developer-df-detail-guid").textContent).toBe("0-1-100");
  });

  it("uses catalogGuid fallback when detail guid has no stringValue (#2951)", async () => {
    getDisplayFormatDetail.mockResolvedValue({
      ...sampleDetail,
      guid: undefined,
    });
    render(
      <DisplayFormatDetailPanel
        idOrName="By_Author"
        catalogGuid="0-11-5"
        onBack={() => undefined}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("developer-df-acl-stub")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-df-detail-guid").textContent).toBe("0-11-5");
    expect(screen.getByTestId("developer-df-acl-stub").getAttribute("data-object-guid")).toBe(
      "0-11-5",
    );
  });

  it("uses guidString when nested guid is absent (#3200)", async () => {
    getDisplayFormatDetail.mockResolvedValue({
      ...sampleDetail,
      guid: undefined,
      guidString: "0-31-9",
    });
    render(<DisplayFormatDetailPanel idOrName="By_Author" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-df-acl-stub")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-df-detail-guid").textContent).toBe("0-31-9");
    expect(screen.getByTestId("developer-df-acl-stub").getAttribute("data-object-guid")).toBe(
      "0-31-9",
    );
  });

  it("synthesizes GUID from displayId when guid is omitted (#3200)", async () => {
    getDisplayFormatDetail.mockResolvedValue({
      ...sampleDetail,
      guid: undefined,
      guidString: undefined,
      displayId: 12,
    });
    render(<DisplayFormatDetailPanel idOrName="By_Author" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-df-acl-stub")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-df-detail-guid").textContent).toBe("0-31-12");
    expect(screen.getByTestId("developer-df-acl-stub").getAttribute("data-object-guid")).toBe(
      "0-31-12",
    );
  });

  it("synthesizes object guid from host/type/uuid parts on detail (#2951)", async () => {
    getDisplayFormatDetail.mockResolvedValue({
      ...sampleDetail,
      guid: { hostId: 0, type: 11, uuid: 301 },
    });
    render(<DisplayFormatDetailPanel idOrName="By_Author" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-df-acl-stub")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-df-detail-guid").textContent).toBe("0-11-301");
    expect(screen.getByTestId("developer-df-acl-stub").getAttribute("data-object-guid")).toBe(
      "0-11-301",
    );
  });

  it("shows empty columns section when detail has none", async () => {
    getDisplayFormatDetail.mockResolvedValue({ ...sampleDetail, columns: [] });
    render(<DisplayFormatDetailPanel idOrName="Default" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-df-columns-empty")).toBeTruthy();
    });
    expect(screen.queryByTestId("developer-df-columns-table")).toBeNull();
  });

  it("shows session-redirect message via panelErrMsg", async () => {
    getDisplayFormatDetail.mockRejectedValue(new SessionRedirectError());
    render(<DisplayFormatDetailPanel idOrName="Default" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-df-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-df-detail-error").textContent).toBe(
      DEV_MSG.SESSION_REDIRECT,
    );
    expect(screen.queryByTestId("developer-df-detail-loading")).toBeNull();
    expect(screen.queryByTestId("developer-df-detail-title")).toBeNull();
  });

  it("shows ApiError status via panelErrMsg", async () => {
    getDisplayFormatDetail.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
      body: null,
    });
    render(<DisplayFormatDetailPanel idOrName="Default" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-df-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-df-detail-error").textContent).toBe(
      `${DEV_MSG.DF_DETAIL_ERROR} (500)`,
    );
  });

  it("shows Error.message via panelErrMsg", async () => {
    getDisplayFormatDetail.mockRejectedValue(new Error("network down"));
    render(<DisplayFormatDetailPanel idOrName="Default" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-df-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-df-detail-error").textContent).toBe(
      `${DEV_MSG.DF_DETAIL_ERROR} network down`,
    );
    expect(screen.queryByTestId("developer-df-detail-title")).toBeNull();
  });

  it("shows fallback when rejection has no message", async () => {
    getDisplayFormatDetail.mockRejectedValue("boom");
    render(<DisplayFormatDetailPanel idOrName="Default" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-df-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-df-detail-error").textContent).toBe(
      DEV_MSG.DF_DETAIL_ERROR,
    );
  });

  it("disables save until the name is valid on create", () => {
    render(<DisplayFormatDetailPanel idOrName={null} onBack={() => undefined} />);
    const save = screen.getByTestId("developer-df-save") as HTMLButtonElement;
    expect(save.disabled).toBe(true);
    fireEvent.change(screen.getByTestId("developer-df-name"), {
      target: { value: "has space" },
    });
    expect(save.disabled).toBe(true);
    fireEvent.change(screen.getByTestId("developer-df-name"), {
      target: { value: "MyFmt" },
    });
    expect(save.disabled).toBe(false);
  });

  it("keeps name read-only on edit", async () => {
    getDisplayFormatDetail.mockResolvedValue(sampleDetail);
    render(<DisplayFormatDetailPanel idOrName="Default" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-df-name")).toBeTruthy();
    });
    expect((screen.getByTestId("developer-df-name") as HTMLInputElement).disabled).toBe(true);
  });

  it("surfaces 400 invalid name on create", async () => {
    createDisplayFormat.mockRejectedValue({
      status: 400,
      statusText: "Bad Request",
      body: { message: "name cannot contain whitespace" },
    });
    const onSaved = vi.fn();
    render(
      <DisplayFormatDetailPanel idOrName={null} onBack={() => undefined} onSaved={onSaved} />,
    );
    fireEvent.change(screen.getByTestId("developer-df-name"), {
      target: { value: "MyFmt" },
    });
    fireEvent.click(screen.getByTestId("developer-df-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-df-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-df-detail-error").textContent).toContain(
      DEV_MSG.DF_INVALID_NAME,
    );
    expect(screen.getByTestId("developer-df-detail-error").textContent).toContain(
      "name cannot contain whitespace",
    );
    expect(onSaved).not.toHaveBeenCalled();
  });

  it("surfaces 409 duplicate name on create", async () => {
    createDisplayFormat.mockRejectedValue({
      status: 409,
      statusText: "Conflict",
      body: { message: "Display format already exists: MyFmt" },
    });
    const onSaved = vi.fn();
    render(
      <DisplayFormatDetailPanel idOrName={null} onBack={() => undefined} onSaved={onSaved} />,
    );
    fireEvent.change(screen.getByTestId("developer-df-name"), {
      target: { value: "MyFmt" },
    });
    fireEvent.click(screen.getByTestId("developer-df-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-df-detail-error")).toBeTruthy();
    });
    expect(createDisplayFormat).toHaveBeenCalled();
    expect(screen.getByTestId("developer-df-detail-error").textContent).toContain(
      DEV_MSG.DF_DUPLICATE,
    );
    expect(screen.getByTestId("developer-df-detail-error").textContent).toContain(
      "already exists",
    );
    expect(onSaved).not.toHaveBeenCalled();
  });

  it("surfaces 403 non-Admin on create", async () => {
    createDisplayFormat.mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: { message: "Admin role required" },
    });
    render(<DisplayFormatDetailPanel idOrName={null} onBack={() => undefined} />);
    fireEvent.change(screen.getByTestId("developer-df-name"), {
      target: { value: "MyFmt" },
    });
    fireEvent.click(screen.getByTestId("developer-df-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-df-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-df-detail-error").textContent).toContain(
      DEV_MSG.DF_FORBIDDEN,
    );
    expect(screen.getByTestId("developer-df-detail-error").textContent).toContain(
      "Admin role required",
    );
  });

  it("does not POST create twice when save is clicked twice", async () => {
    let resolveCreate: (value: typeof sampleDetail) => void = () => undefined;
    createDisplayFormat.mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveCreate = resolve;
        }),
    );
    render(<DisplayFormatDetailPanel idOrName={null} onBack={() => undefined} />);
    fireEvent.change(screen.getByTestId("developer-df-name"), {
      target: { value: "MyFmt" },
    });
    fireEvent.click(screen.getByTestId("developer-df-save"));
    fireEvent.click(screen.getByTestId("developer-df-save"));
    expect(createDisplayFormat).toHaveBeenCalledTimes(1);
    resolveCreate({
      name: "MyFmt",
      label: "My Format",
      description: "",
      columns: [],
    });
    await waitFor(() => {
      expect(screen.getByTestId("developer-df-editor-notice")).toBeTruthy();
    });
  });

  it("creates a display format when the name is valid", async () => {
    createDisplayFormat.mockResolvedValue({
      name: "MyFmt",
      label: "My Format",
      description: "Created via SPA",
      columns: [],
    });
    const onSaved = vi.fn();
    render(
      <DisplayFormatDetailPanel idOrName={null} onBack={() => undefined} onSaved={onSaved} />,
    );
    fireEvent.change(screen.getByTestId("developer-df-name"), {
      target: { value: "MyFmt" },
    });
    fireEvent.change(screen.getByTestId("developer-df-label"), {
      target: { value: "My Format" },
    });
    fireEvent.change(screen.getByTestId("developer-df-description"), {
      target: { value: "Created via SPA" },
    });
    fireEvent.click(screen.getByTestId("developer-df-save"));
    await waitFor(() => {
      expect(onSaved).toHaveBeenCalled();
    });
    expect(createDisplayFormat).toHaveBeenCalledWith(
      expect.objectContaining({
        name: "MyFmt",
        label: "My Format",
        description: "Created via SPA",
      }),
    );
    expect(screen.getByTestId("developer-df-editor-notice").textContent).toBe(DEV_MSG.DF_SAVED);
  });

  it("saves label changes on an existing display format", async () => {
    getDisplayFormatDetail.mockResolvedValue(sampleDetail);
    saveDisplayFormat.mockResolvedValue({
      ...sampleDetail,
      label: "Updated label",
    });
    const onSaved = vi.fn();
    render(
      <DisplayFormatDetailPanel
        idOrName="Default"
        onBack={() => undefined}
        onSaved={onSaved}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("developer-df-label")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-df-label"), {
      target: { value: "Updated label" },
    });
    fireEvent.click(screen.getByTestId("developer-df-save"));
    await waitFor(() => {
      expect(onSaved).toHaveBeenCalled();
    });
    expect(saveDisplayFormat).toHaveBeenCalledWith(
      "Default",
      expect.objectContaining({
        name: "Default",
        label: "Updated label",
        description: "System default",
      }),
    );
  });

  it("deletes after confirm and omits delete chrome in create mode", async () => {
    getDisplayFormatDetail.mockResolvedValue(sampleDetail);
    deleteDisplayFormat.mockResolvedValue(undefined);
      const onDeleted = vi.fn();
      render(
        <DisplayFormatDetailPanel
          idOrName="Default"
          onBack={() => undefined}
          onDeleted={onDeleted}
        />,
      );
      await waitFor(() => {
        expect(screen.getByTestId("developer-df-delete")).toBeTruthy();
      });
      fireEvent.click(screen.getByTestId("developer-df-delete"));
      fireEvent.click(screen.getByTestId("developer-catalog-confirm-submit"));
      await waitFor(() => {
        expect(onDeleted).toHaveBeenCalled();
      });
      expect(deleteDisplayFormat).toHaveBeenCalledWith("Default");
  });

  it("surfaces 404 missing display format on delete", async () => {
    getDisplayFormatDetail.mockResolvedValue(sampleDetail);
    deleteDisplayFormat.mockRejectedValue({
      status: 404,
      statusText: "Not Found",
      body: { message: "Display format not found" },
    });
      render(<DisplayFormatDetailPanel idOrName="Default" onBack={() => undefined} />);
      await waitFor(() => {
        expect(screen.getByTestId("developer-df-delete")).toBeTruthy();
      });
      fireEvent.click(screen.getByTestId("developer-df-delete"));
      fireEvent.click(screen.getByTestId("developer-catalog-confirm-submit"));
      await waitFor(() => {
        expect(screen.getByTestId("developer-df-detail-error")).toBeTruthy();
      });
      expect(screen.getByTestId("developer-df-detail-error").textContent).toContain(
        DEV_MSG.DF_NOT_FOUND,
      );
  });

  it("does not show delete on create", () => {
    render(<DisplayFormatDetailPanel idOrName={null} onBack={() => undefined} />);
    expect(screen.queryByTestId("developer-df-delete")).toBeNull();
  });

  it("saves PUT body columns on a user format", async () => {
    const userDetail = {
      ...sampleDetail,
      name: "qa4097fmt",
      label: "User format",
    };
    getDisplayFormatDetail.mockResolvedValue(userDetail);
    updateDisplayFormat.mockResolvedValue({
      ...userDetail,
      columns: [
        userDetail.columns[0],
        { source: "sys_contentid", displayName: "Content id", position: 1 },
      ],
    });
    render(<DisplayFormatDetailPanel idOrName="qa4097fmt" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-df-column-editor")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-df-column-source"), {
      target: { value: "sys_contentid" },
    });
    fireEvent.click(screen.getByTestId("developer-df-column-add"));
    fireEvent.click(screen.getByTestId("developer-df-columns-save"));
    await waitFor(() => {
      expect(updateDisplayFormat).toHaveBeenCalled();
    });
    const [, body] = updateDisplayFormat.mock.calls[0];
    expect(body.columns.map((c: { source?: string }) => c.source)).toEqual([
      "sys_title",
      "sys_contentid",
    ]);
    await waitFor(() => {
      expect(screen.getByTestId("developer-df-editor-notice").textContent).toBe(
        DEV_MSG.DF_COLUMNS_SAVED,
      );
    });
  });

  it("surfaces 400 invalid source from PUT", async () => {
    const userDetail = { ...sampleDetail, name: "qa4097fmt" };
    getDisplayFormatDetail.mockResolvedValue(userDetail);
    updateDisplayFormat.mockRejectedValue({
      status: 400,
      statusText: "Bad Request",
      body: "column source is required",
    });
    render(<DisplayFormatDetailPanel idOrName="qa4097fmt" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-df-column-editor")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-df-column-source"), {
      target: { value: "sys_contentid" },
    });
    fireEvent.click(screen.getByTestId("developer-df-column-add"));
    fireEvent.click(screen.getByTestId("developer-df-columns-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-df-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-df-detail-error").textContent).toContain(
      DEV_MSG.DF_COLUMNS_INVALID_SOURCE,
    );
  });

  it("surfaces 403 non-Admin from PUT", async () => {
    const userDetail = { ...sampleDetail, name: "qa4097fmt" };
    getDisplayFormatDetail.mockResolvedValue(userDetail);
    updateDisplayFormat.mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: "Admin role required",
    });
    render(<DisplayFormatDetailPanel idOrName="qa4097fmt" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-df-column-editor")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-df-column-source"), {
      target: { value: "sys_workflow" },
    });
    fireEvent.click(screen.getByTestId("developer-df-column-add"));
    fireEvent.click(screen.getByTestId("developer-df-columns-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-df-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-df-detail-error").textContent).toContain(
      DEV_MSG.DF_COLUMNS_FORBIDDEN,
    );
  });

  it("does not mutate a packaged/system format", async () => {
    getDisplayFormatDetail.mockResolvedValue(sampleDetail);
    render(<DisplayFormatDetailPanel idOrName="Default" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-df-columns-readonly")).toBeTruthy();
    });
    expect(screen.queryByTestId("developer-df-column-editor")).toBeNull();
    expect(screen.queryByTestId("developer-df-columns-save")).toBeNull();
    expect(screen.getByTestId("developer-df-communities-readonly")).toBeTruthy();
    expect(screen.queryByTestId("developer-df-community-editor")).toBeNull();
    expect(updateDisplayFormat).not.toHaveBeenCalled();
  });

  it("PUTs allowedCommunities for a user display format", async () => {
    const userDetail = { ...sampleDetail, name: "qa4098fmt", allowedCommunities: {} };
    getDisplayFormatDetail.mockResolvedValue(userDetail);
    updateDisplayFormat.mockResolvedValue({
      ...userDetail,
      allowedCommunities: { "0-10-10": "Default" },
    });
    render(<DisplayFormatDetailPanel idOrName="qa4098fmt" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-df-community-Default")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-df-communities-all"));
    fireEvent.click(screen.getByTestId("developer-df-community-Default"));
    fireEvent.click(screen.getByTestId("developer-df-communities-save"));
    await waitFor(() => {
      expect(updateDisplayFormat).toHaveBeenCalled();
    });
    const [, body] = updateDisplayFormat.mock.calls[0];
    expect(body.allowedCommunities).toEqual([{ guid: "0-10-10", name: "Default" }]);
    expect(body.columns).toBeUndefined();
    await waitFor(() => {
      expect(screen.getByTestId("developer-df-editor-notice").textContent).toBe(
        DEV_MSG.DF_COMMUNITIES_SAVED,
      );
    });
  });

  it("surfaces 400 unknown community from PUT", async () => {
    const userDetail = { ...sampleDetail, name: "qa4098fmt", allowedCommunities: {} };
    getDisplayFormatDetail.mockResolvedValue(userDetail);
    updateDisplayFormat.mockRejectedValue({
      status: 400,
      statusText: "Bad Request",
      body: "unknown community: missing",
    });
    render(<DisplayFormatDetailPanel idOrName="qa4098fmt" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-df-community-Default")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-df-communities-all"));
    fireEvent.click(screen.getByTestId("developer-df-community-Default"));
    fireEvent.click(screen.getByTestId("developer-df-communities-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-df-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-df-detail-error").textContent).toContain(
      DEV_MSG.DF_COMMUNITIES_UNKNOWN,
    );
    expect(screen.getByTestId("developer-df-detail-error").textContent).toContain(
      "unknown community",
    );
  });

  it("surfaces 403 non-Admin from community PUT", async () => {
    const userDetail = { ...sampleDetail, name: "qa4098fmt", allowedCommunities: {} };
    getDisplayFormatDetail.mockResolvedValue(userDetail);
    updateDisplayFormat.mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: "Admin role required",
    });
    render(<DisplayFormatDetailPanel idOrName="qa4098fmt" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-df-community-Default")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-df-communities-all"));
    fireEvent.click(screen.getByTestId("developer-df-community-Default"));
    fireEvent.click(screen.getByTestId("developer-df-communities-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-df-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-df-detail-error").textContent).toContain(
      DEV_MSG.DF_COMMUNITIES_FORBIDDEN,
    );
  });
});
