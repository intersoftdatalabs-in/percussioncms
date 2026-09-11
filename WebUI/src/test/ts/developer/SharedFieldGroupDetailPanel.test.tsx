/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import * as sharedFieldsApi from "../../../main/ts/api/developer/sharedFieldsApi";
import { DEV_MSG } from "../../../main/ts/developer/messages";
import { SharedFieldGroupDetailPanel } from "../../../main/ts/developer/SharedFieldGroupDetailPanel";

vi.mock("../../../main/ts/api/developer/sharedFieldsApi", async (importOriginal) => {
  const actual = await importOriginal<
    typeof import("../../../main/ts/api/developer/sharedFieldsApi")
  >();
  return {
    ...actual,
    listSharedFieldGroups: vi.fn(),
    getSharedFieldGroupDetail: vi.fn(),
    createSharedFieldGroup: vi.fn(),
    updateSharedFieldGroup: vi.fn(),
    deleteSharedFieldGroup: vi.fn(),
    addSharedField: vi.fn(),
    deleteSharedField: vi.fn(),
    getSharedFieldControlProperties: vi.fn(),
    replaceSharedFieldControlProperties: vi.fn(),
  };
});

const getSharedFieldGroupDetail = sharedFieldsApi.getSharedFieldGroupDetail as ReturnType<
  typeof vi.fn
>;
const createSharedFieldGroup = sharedFieldsApi.createSharedFieldGroup as ReturnType<
  typeof vi.fn
>;
const updateSharedFieldGroup = sharedFieldsApi.updateSharedFieldGroup as ReturnType<
  typeof vi.fn
>;
const deleteSharedFieldGroup = sharedFieldsApi.deleteSharedFieldGroup as ReturnType<
  typeof vi.fn
>;
const addSharedField = sharedFieldsApi.addSharedField as ReturnType<typeof vi.fn>;
const deleteSharedField = sharedFieldsApi.deleteSharedField as ReturnType<typeof vi.fn>;
const getSharedFieldControlProperties =
  sharedFieldsApi.getSharedFieldControlProperties as ReturnType<typeof vi.fn>;
const replaceSharedFieldControlProperties =
  sharedFieldsApi.replaceSharedFieldControlProperties as ReturnType<typeof vi.fn>;

const sampleDetail = {
  name: "shared",
  filename: "shared.xml",
  fields: [
    {
      name: "rx_title",
      dataType: "text",
      required: true,
      searchable: true,
      readOnly: false,
      occurrence: "required",
    },
  ],
  designGaps: ["control properties not in this chrome"],
};

describe("SharedFieldGroupDetailPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    getSharedFieldGroupDetail.mockReset();
    createSharedFieldGroup.mockReset();
    updateSharedFieldGroup.mockReset();
    deleteSharedFieldGroup.mockReset();
    addSharedField.mockReset();
    deleteSharedField.mockReset();
    getSharedFieldControlProperties.mockReset();
    replaceSharedFieldControlProperties.mockReset();
    getSharedFieldControlProperties.mockResolvedValue({
      fieldName: "rx_title",
      control: "sys_EditBox",
      properties: [{ name: "height", value: "200" }],
    });
    replaceSharedFieldControlProperties.mockResolvedValue({
      fieldName: "rx_title",
      control: "sys_EditBox",
      properties: [{ name: "height", value: "240" }],
    });
  });

  it("loads detail on success and supports back", async () => {
    getSharedFieldGroupDetail.mockResolvedValue(sampleDetail);
    const onBack = vi.fn();
    render(<SharedFieldGroupDetailPanel name="shared" onBack={onBack} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sf-detail-title")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sf-detail-title").textContent).toBe("shared");
    expect(screen.getByTestId("developer-sf-fields-table")).toBeTruthy();
    expect(screen.getByTestId("developer-sf-fields-table").querySelector('[data-sf-field="rx_title"]')).toBeTruthy();
    expect(screen.getByTestId("developer-sf-gaps")).toBeTruthy();
    expect(getSharedFieldGroupDetail).toHaveBeenCalledWith("shared");
    fireEvent.click(screen.getByTestId("developer-sf-back"));
    expect(onBack).toHaveBeenCalled();
  });

  it("shows empty fields section when detail has none", async () => {
    getSharedFieldGroupDetail.mockResolvedValue({
      ...sampleDetail,
      fields: [],
    });
    render(<SharedFieldGroupDetailPanel name="shared" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sf-fields-empty")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sf-fields-empty").textContent).toBe(DEV_MSG.SF_NONE);
    expect(screen.queryByTestId("developer-sf-fields-table")).toBeNull();
  });

  it("shows session-redirect message via panelErrMsg", async () => {
    getSharedFieldGroupDetail.mockRejectedValue(new SessionRedirectError());
    render(<SharedFieldGroupDetailPanel name="shared" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sf-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sf-detail-error").textContent).toBe(
      DEV_MSG.SESSION_REDIRECT,
    );
    expect(screen.queryByTestId("developer-sf-detail-loading")).toBeNull();
    expect(screen.queryByTestId("developer-sf-detail-title")).toBeNull();
  });

  it("shows ApiError status via panelErrMsg", async () => {
    getSharedFieldGroupDetail.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
      body: null,
    });
    render(<SharedFieldGroupDetailPanel name="shared" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sf-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sf-detail-error").textContent).toBe(
      `${DEV_MSG.SF_DETAIL_ERROR} (500)`,
    );
  });

  it("shows 404 missing group via panelErrMsg", async () => {
    getSharedFieldGroupDetail.mockRejectedValue({
      status: 404,
      statusText: "Not Found",
      body: { message: "Shared field group not found" },
    });
    render(<SharedFieldGroupDetailPanel name="missing" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sf-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sf-detail-error").textContent).toContain(
      DEV_MSG.SF_DETAIL_ERROR,
    );
    expect(screen.getByTestId("developer-sf-detail-error").textContent).toContain(
      "Shared field group not found",
    );
    expect(screen.queryByTestId("developer-sf-save")).toBeNull();
  });

  it("shows 404 status when missing group has no body message", async () => {
    getSharedFieldGroupDetail.mockRejectedValue({
      status: 404,
      statusText: "Not Found",
      body: null,
    });
    render(<SharedFieldGroupDetailPanel name="missing" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sf-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sf-detail-error").textContent).toBe(
      `${DEV_MSG.SF_DETAIL_ERROR} (404)`,
    );
  });

  it("shows Error.message via panelErrMsg", async () => {
    getSharedFieldGroupDetail.mockRejectedValue(new Error("network down"));
    render(<SharedFieldGroupDetailPanel name="shared" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sf-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sf-detail-error").textContent).toBe(
      `${DEV_MSG.SF_DETAIL_ERROR} network down`,
    );
    expect(screen.queryByTestId("developer-sf-detail-title")).toBeNull();
  });

  it("shows fallback when rejection has no message", async () => {
    getSharedFieldGroupDetail.mockRejectedValue("boom");
    render(<SharedFieldGroupDetailPanel name="shared" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sf-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sf-detail-error").textContent).toBe(
      DEV_MSG.SF_DETAIL_ERROR,
    );
  });

  it("disables save until the group name is valid on create", () => {
    render(<SharedFieldGroupDetailPanel name={null} onBack={() => undefined} />);
    const save = screen.getByTestId("developer-sf-save") as HTMLButtonElement;
    expect(save.disabled).toBe(true);
    fireEvent.change(screen.getByTestId("developer-sf-name"), {
      target: { value: "has space" },
    });
    expect(save.disabled).toBe(true);
    fireEvent.change(screen.getByTestId("developer-sf-name"), {
      target: { value: "customShared" },
    });
    expect(save.disabled).toBe(false);
  });

  it("surfaces 409 duplicate name on create", async () => {
    createSharedFieldGroup.mockRejectedValue({
      status: 409,
      statusText: "Conflict",
      body: { message: "Shared field group already exists: shared" },
    });
    const onSaved = vi.fn();
    render(
      <SharedFieldGroupDetailPanel name={null} onBack={() => undefined} onSaved={onSaved} />,
    );
    fireEvent.change(screen.getByTestId("developer-sf-name"), {
      target: { value: "shared" },
    });
    fireEvent.click(screen.getByTestId("developer-sf-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-sf-detail-error")).toBeTruthy();
    });
    expect(createSharedFieldGroup).toHaveBeenCalled();
    expect(screen.getByTestId("developer-sf-detail-error").textContent).toContain(
      DEV_MSG.SF_DUPLICATE,
    );
    expect(screen.getByTestId("developer-sf-detail-error").textContent).toContain(
      "already exists",
    );
    expect(onSaved).not.toHaveBeenCalled();
  });

  it("does not POST create twice when save is clicked twice", async () => {
    let resolveCreate: (value: typeof sampleDetail) => void = () => undefined;
    createSharedFieldGroup.mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveCreate = resolve;
        }),
    );
    render(<SharedFieldGroupDetailPanel name={null} onBack={() => undefined} />);
    fireEvent.change(screen.getByTestId("developer-sf-name"), {
      target: { value: "customShared" },
    });
    fireEvent.click(screen.getByTestId("developer-sf-save"));
    fireEvent.click(screen.getByTestId("developer-sf-save"));
    expect(createSharedFieldGroup).toHaveBeenCalledTimes(1);
    resolveCreate({
      name: "customShared",
      filename: "customShared.xml",
      fields: [],
    });
    await waitFor(() => {
      expect(screen.getByTestId("developer-sf-editor-notice")).toBeTruthy();
    });
  });

  it("creates a group when the name is valid", async () => {
    createSharedFieldGroup.mockResolvedValue({
      name: "customShared",
      filename: "customShared.xml",
      fields: [],
    });
    const onSaved = vi.fn();
    render(
      <SharedFieldGroupDetailPanel name={null} onBack={() => undefined} onSaved={onSaved} />,
    );
    fireEvent.change(screen.getByTestId("developer-sf-name"), {
      target: { value: "customShared" },
    });
    fireEvent.click(screen.getByTestId("developer-sf-save"));
    await waitFor(() => {
      expect(onSaved).toHaveBeenCalled();
    });
    expect(createSharedFieldGroup).toHaveBeenCalledWith(
      expect.objectContaining({
        name: "customShared",
      }),
    );
    expect(screen.getByTestId("developer-sf-editor-notice").textContent).toBe(
      DEV_MSG.SF_SAVED,
    );
  });

  it("disables save in edit mode until a field changes", async () => {
    getSharedFieldGroupDetail.mockResolvedValue(sampleDetail);
    render(<SharedFieldGroupDetailPanel name="shared" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sf-save")).toBeTruthy();
    });
    expect((screen.getByTestId("developer-sf-save") as HTMLButtonElement).disabled).toBe(true);
    fireEvent.change(screen.getByTestId("developer-sf-filename"), {
      target: { value: "shared-renamed.xml" },
    });
    expect((screen.getByTestId("developer-sf-save") as HTMLButtonElement).disabled).toBe(false);
  });

  it("saves filename changes on an existing group", async () => {
    getSharedFieldGroupDetail.mockResolvedValue(sampleDetail);
    updateSharedFieldGroup.mockResolvedValue({
      ...sampleDetail,
      filename: "shared-renamed.xml",
    });
    const onSaved = vi.fn();
    render(
      <SharedFieldGroupDetailPanel name="shared" onBack={() => undefined} onSaved={onSaved} />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("developer-sf-filename")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-sf-filename"), {
      target: { value: "shared-renamed.xml" },
    });
    fireEvent.click(screen.getByTestId("developer-sf-save"));
    await waitFor(() => {
      expect(onSaved).toHaveBeenCalled();
    });
    expect(updateSharedFieldGroup).toHaveBeenCalledWith(
      "shared",
      expect.objectContaining({ name: "shared", filename: "shared-renamed.xml" }),
    );
  });

  it("deletes after confirm and omits delete chrome in create mode", async () => {
    getSharedFieldGroupDetail.mockResolvedValue(sampleDetail);
    deleteSharedFieldGroup.mockResolvedValue(undefined);
    const onDeleted = vi.fn();
    render(
      <SharedFieldGroupDetailPanel
        name="shared"
        onBack={() => undefined}
        onDeleted={onDeleted}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("developer-sf-delete")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-sf-delete"));
    fireEvent.click(screen.getByTestId("developer-catalog-confirm-submit"));
    await waitFor(() => {
      expect(onDeleted).toHaveBeenCalled();
    });
    expect(deleteSharedFieldGroup).toHaveBeenCalledWith("shared");
  });

  it("does not show delete on create", () => {
    render(<SharedFieldGroupDetailPanel name={null} onBack={() => undefined} />);
    expect(screen.queryByTestId("developer-sf-delete")).toBeNull();
  });

  it("disables add until the nested field name is valid", async () => {
    getSharedFieldGroupDetail.mockResolvedValue(sampleDetail);
    render(<SharedFieldGroupDetailPanel name="shared" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sf-add-btn")).toBeTruthy();
    });
    const add = screen.getByTestId("developer-sf-add-btn") as HTMLButtonElement;
    expect(add.disabled).toBe(true);
    fireEvent.change(screen.getByTestId("developer-sf-new-name"), {
      target: { value: "1bad" },
    });
    expect(add.disabled).toBe(true);
    fireEvent.change(screen.getByTestId("developer-sf-new-name"), {
      target: { value: "rx_note" },
    });
    expect(add.disabled).toBe(false);
  });

  it("adds a nested field and lists it", async () => {
    getSharedFieldGroupDetail.mockResolvedValue(sampleDetail);
    addSharedField.mockResolvedValue({
      ...sampleDetail,
      fields: [
        ...sampleDetail.fields,
        { name: "rx_note", dataType: "text", occurrence: "optional" },
      ],
    });
    render(<SharedFieldGroupDetailPanel name="shared" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sf-new-name")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-sf-new-name"), {
      target: { value: "rx_note" },
    });
    fireEvent.click(screen.getByTestId("developer-sf-add-btn"));
    await waitFor(() => {
      expect(
        screen.getByTestId("developer-sf-fields-table").querySelector('[data-sf-field="rx_note"]'),
      ).toBeTruthy();
    });
    expect(addSharedField).toHaveBeenCalledWith("shared", {
      name: "rx_note",
      dataType: "text",
      occurrence: "optional",
    });
    expect(screen.getByTestId("developer-sf-editor-notice").textContent).toBe(DEV_MSG.SF_ADDED);
  });

  it("surfaces 409 duplicate nested field name", async () => {
    getSharedFieldGroupDetail.mockResolvedValue(sampleDetail);
    addSharedField.mockRejectedValue({
      status: 409,
      statusText: "Conflict",
      body: { message: "Shared field already exists: rx_title" },
    });
    render(<SharedFieldGroupDetailPanel name="shared" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sf-new-name")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-sf-new-name"), {
      target: { value: "rx_title" },
    });
    fireEvent.click(screen.getByTestId("developer-sf-add-btn"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-sf-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sf-detail-error").textContent).toContain(
      DEV_MSG.SF_FIELD_DUPLICATE,
    );
  });

  it("deletes a nested field after confirm", async () => {
    getSharedFieldGroupDetail
      .mockResolvedValueOnce(sampleDetail)
      .mockResolvedValueOnce({ ...sampleDetail, fields: [] });
    deleteSharedField.mockResolvedValue(undefined);
    render(<SharedFieldGroupDetailPanel name="shared" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sf-field-delete")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-sf-field-delete"));
    fireEvent.click(screen.getByTestId("developer-catalog-confirm-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-sf-fields-empty")).toBeTruthy();
    });
    expect(deleteSharedField).toHaveBeenCalledWith("shared", "rx_title");
    expect(screen.getByTestId("developer-sf-editor-notice").textContent).toBe(
      DEV_MSG.SF_FIELD_DELETED,
    );
  });

  it("drops the field from the table when re-fetch fails after delete", async () => {
    getSharedFieldGroupDetail
      .mockResolvedValueOnce(sampleDetail)
      .mockRejectedValueOnce({
        status: 500,
        statusText: "Server Error",
        body: { message: "reload failed" },
      });
    deleteSharedField.mockResolvedValue(undefined);
    render(<SharedFieldGroupDetailPanel name="shared" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sf-field-delete")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-sf-field-delete"));
    fireEvent.click(screen.getByTestId("developer-catalog-confirm-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-sf-detail-error")).toBeTruthy();
    });
    expect(deleteSharedField).toHaveBeenCalledWith("shared", "rx_title");
    expect(screen.getByTestId("developer-sf-fields-empty")).toBeTruthy();
    expect(screen.queryByTestId("developer-sf-fields-table")).toBeNull();
  });

  it("loads and saves a control property without sending choices", async () => {
    getSharedFieldGroupDetail.mockResolvedValue(sampleDetail);
    render(<SharedFieldGroupDetailPanel name="shared" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sf-cp-value-0")).toBeTruthy();
    });
    expect(getSharedFieldControlProperties).toHaveBeenCalledWith("shared", "rx_title");
    fireEvent.change(screen.getByTestId("developer-sf-cp-value-0"), {
      target: { value: "240" },
    });
    const save = screen.getByTestId("developer-sf-cp-save") as HTMLButtonElement;
    expect(save.disabled).toBe(false);
    fireEvent.click(save);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sf-editor-notice")).toBeTruthy();
    });
    expect(replaceSharedFieldControlProperties).toHaveBeenCalledWith("shared", "rx_title", {
      properties: [{ name: "height", value: "240" }],
    });
    expect(screen.getByTestId("developer-sf-editor-notice").textContent).toBe(
      DEV_MSG.SF_CONTROL_PROPS_SAVED,
    );
  });

  it("sends choices type none when the catalog is cleared", async () => {
    getSharedFieldGroupDetail.mockResolvedValue(sampleDetail);
    getSharedFieldControlProperties.mockResolvedValue({
      fieldName: "rx_title",
      control: "sys_EditBox",
      properties: [{ name: "height", value: "200" }],
      choices: { type: "local", entries: [{ value: "a", label: "A" }] },
    });
    replaceSharedFieldControlProperties.mockResolvedValue({
      fieldName: "rx_title",
      control: "sys_EditBox",
      properties: [{ name: "height", value: "200" }],
    });
    render(<SharedFieldGroupDetailPanel name="shared" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-ch-type")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-ct-ch-type"), {
      target: { value: "none" },
    });
    fireEvent.click(screen.getByTestId("developer-sf-cp-save"));
    await waitFor(() => {
      expect(replaceSharedFieldControlProperties).toHaveBeenCalled();
    });
    expect(replaceSharedFieldControlProperties).toHaveBeenCalledWith("shared", "rx_title", {
      properties: [{ name: "height", value: "200" }],
      choices: { type: "none" },
    });
  });
});
