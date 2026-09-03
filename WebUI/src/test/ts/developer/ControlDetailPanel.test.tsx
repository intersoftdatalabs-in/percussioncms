/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import * as controlsApi from "../../../main/ts/api/developer/controlsApi";
import { ControlDetailPanel } from "../../../main/ts/developer/ControlDetailPanel";
import { DEV_MSG } from "../../../main/ts/developer/messages";

vi.mock("../../../main/ts/api/developer/controlsApi", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("../../../main/ts/api/developer/controlsApi")>();
  return {
    ...actual,
    listControls: vi.fn(),
    getControlDetail: vi.fn(),
    updateControl: vi.fn(),
    deleteControl: vi.fn(),
  };
});

const getControlDetail = controlsApi.getControlDetail as ReturnType<typeof vi.fn>;
const updateControl = controlsApi.updateControl as ReturnType<typeof vi.fn>;
const deleteControl = controlsApi.deleteControl as ReturnType<typeof vi.fn>;

const sampleDetail = {
  name: "sys_EditBox",
  displayName: "Edit Box",
  scope: "system",
  dimension: "single",
  choiceSet: null,
  description: "Text edit control",
  parameters: [{ name: "maxlength", dataType: "number", required: false, defaultValue: "255" }],
  designGaps: ["gap-a"],
};

describe("ControlDetailPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    getControlDetail.mockReset();
    updateControl.mockReset();
    deleteControl.mockReset();
  });

  it("loads detail on success and supports back", async () => {
    getControlDetail.mockResolvedValue(sampleDetail);
    const onBack = vi.fn();
    render(<ControlDetailPanel name="sys_EditBox" onBack={onBack} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ctl-detail-title")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ctl-detail-title").textContent).toContain("Edit Box");
    expect(screen.getByTestId("developer-ctl-params-table")).toBeTruthy();
    expect(screen.getByTestId("developer-ctl-gaps").textContent).toContain("gap-a");
    expect(getControlDetail).toHaveBeenCalledWith("sys_EditBox");
    fireEvent.click(screen.getByTestId("developer-ctl-back"));
    expect(onBack).toHaveBeenCalled();
  });

  it("shows name as read-only text and system controls as non-editable", async () => {
    getControlDetail.mockResolvedValue(sampleDetail);
    render(<ControlDetailPanel name="sys_EditBox" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ctl-detail-name")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ctl-detail-name").textContent).toBe("sys_EditBox");
    expect(screen.queryByTestId("developer-ctl-create-name")).toBeNull();
    expect(screen.queryByTestId("developer-ctl-create-save")).toBeNull();
    expect(screen.getByTestId("developer-ctl-system-readonly").textContent).toBe(
      DEV_MSG.CTL_SYSTEM_READONLY,
    );
    expect(screen.queryByTestId("developer-ctl-save")).toBeNull();
    expect(screen.queryByTestId("developer-ctl-delete")).toBeNull();
  });

  it("does not mark user controls as system-readonly", async () => {
    getControlDetail.mockResolvedValue({
      ...sampleDetail,
      name: "qaCtl",
      displayName: "QA",
      scope: "user",
    });
    render(<ControlDetailPanel name="qaCtl" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ctl-detail-name").textContent).toBe("qaCtl");
    });
    expect(screen.queryByTestId("developer-ctl-system-readonly")).toBeNull();
    expect(screen.queryByTestId("developer-ctl-create-save")).toBeNull();
    expect(screen.getByTestId("developer-ctl-save")).toBeTruthy();
    expect(screen.getByTestId("developer-ctl-delete")).toBeTruthy();
  });

  it("shows empty params section when detail has none", async () => {
    getControlDetail.mockResolvedValue({
      ...sampleDetail,
      parameters: [],
    });
    render(<ControlDetailPanel name="sys_EditBox" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ctl-params")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ctl-params").textContent).toContain(DEV_MSG.CTL_NONE);
    expect(screen.queryByTestId("developer-ctl-params-table")).toBeNull();
  });

  it("shows session-redirect message via panelErrMsg", async () => {
    getControlDetail.mockRejectedValue(new SessionRedirectError());
    render(<ControlDetailPanel name="sys_EditBox" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ctl-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ctl-detail-error").textContent).toBe(
      DEV_MSG.SESSION_REDIRECT,
    );
    expect(screen.queryByTestId("developer-ctl-detail-loading")).toBeNull();
    expect(screen.queryByTestId("developer-ctl-detail-title")).toBeNull();
  });

  it("shows ApiError status via panelErrMsg", async () => {
    getControlDetail.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
      body: null,
    });
    render(<ControlDetailPanel name="sys_EditBox" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ctl-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ctl-detail-error").textContent).toBe(
      `${DEV_MSG.CTL_DETAIL_ERROR} (500)`,
    );
  });

  it("shows Error.message via panelErrMsg", async () => {
    getControlDetail.mockRejectedValue(new Error("network down"));
    render(<ControlDetailPanel name="sys_EditBox" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ctl-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ctl-detail-error").textContent).toBe(
      `${DEV_MSG.CTL_DETAIL_ERROR} network down`,
    );
    expect(screen.queryByTestId("developer-ctl-detail-title")).toBeNull();
  });

  it("shows fallback when rejection has no message", async () => {
    getControlDetail.mockRejectedValue("boom");
    render(<ControlDetailPanel name="sys_EditBox" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ctl-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ctl-detail-error").textContent).toBe(
      DEV_MSG.CTL_DETAIL_ERROR,
    );
  });

  it("surfaces GET 404 in the detail error region", async () => {
    getControlDetail.mockRejectedValue({
      status: 404,
      statusText: "Not Found",
      body: null,
    });
    render(<ControlDetailPanel name="missing" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ctl-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ctl-detail-error").textContent).toBe(
      `${DEV_MSG.CTL_NOT_FOUND} (404)`,
    );
  });

  it("saves user-control metadata and omits blank xslSource", async () => {
    getControlDetail.mockResolvedValue({
      ...sampleDetail,
      name: "qaCtl",
      displayName: "QA",
      scope: "user",
      xslSource: undefined,
    });
    updateControl.mockResolvedValue({
      name: "qaCtl",
      displayName: "QA updated",
      scope: "user",
      description: "Saved",
      dimension: "array",
      choiceSet: "required",
    });
    const onSaved = vi.fn();
    render(<ControlDetailPanel name="qaCtl" onBack={() => undefined} onSaved={onSaved} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ctl-edit-display")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-ctl-edit-display"), {
      target: { value: "QA updated" },
    });
    fireEvent.change(screen.getByTestId("developer-ctl-edit-description"), {
      target: { value: "Saved" },
    });
    fireEvent.change(screen.getByTestId("developer-ctl-edit-dimension"), {
      target: { value: "array" },
    });
    fireEvent.change(screen.getByTestId("developer-ctl-edit-choiceset"), {
      target: { value: "required" },
    });
    fireEvent.click(screen.getByTestId("developer-ctl-save"));
    await waitFor(() => {
      expect(onSaved).toHaveBeenCalled();
    });
    expect(updateControl).toHaveBeenCalledWith(
      "qaCtl",
      expect.objectContaining({
        name: "qaCtl",
        displayName: "QA updated",
        description: "Saved",
        dimension: "array",
        choiceSet: "required",
      }),
    );
    expect(updateControl.mock.calls[0][1].xslSource).toBeUndefined();
    const notice = screen.getByTestId("developer-ctl-detail-notice");
    expect(notice.textContent).toBe(DEV_MSG.CTL_SAVED);
    expect(notice.getAttribute("role")).toBe("status");
    expect(notice.getAttribute("aria-live")).toBe("polite");
  });

  it("sends trimmed description and server defaults when optional fields are cleared", async () => {
    getControlDetail.mockResolvedValue({
      ...sampleDetail,
      name: "qaCtl",
      displayName: "QA",
      scope: "user",
      description: "keep me",
      dimension: "array",
      choiceSet: "required",
    });
    updateControl.mockResolvedValue({
      name: "qaCtl",
      displayName: "qaCtl",
      scope: "user",
      description: "",
      dimension: "single",
      choiceSet: "none",
    });
    render(<ControlDetailPanel name="qaCtl" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ctl-edit-description")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-ctl-edit-display"), {
      target: { value: "  " },
    });
    fireEvent.change(screen.getByTestId("developer-ctl-edit-description"), {
      target: { value: "  " },
    });
    fireEvent.change(screen.getByTestId("developer-ctl-edit-dimension"), {
      target: { value: "" },
    });
    fireEvent.change(screen.getByTestId("developer-ctl-edit-choiceset"), {
      target: { value: "" },
    });
    fireEvent.click(screen.getByTestId("developer-ctl-save"));
    await waitFor(() => {
      expect(updateControl).toHaveBeenCalled();
    });
    expect(updateControl).toHaveBeenCalledWith("qaCtl", {
      name: "qaCtl",
      displayName: "qaCtl",
      description: "",
      dimension: "single",
      choiceSet: "none",
    });
  });

  it("surfaces PUT 403, 404, and system 409 on save", async () => {
    getControlDetail.mockResolvedValue({
      ...sampleDetail,
      name: "qaCtl",
      displayName: "QA",
      scope: "user",
    });
    const cases: Array<{ status: number; fallback: string }> = [
      { status: 403, fallback: DEV_MSG.CTL_FORBIDDEN },
      { status: 404, fallback: DEV_MSG.CTL_NOT_FOUND },
      { status: 409, fallback: DEV_MSG.CTL_SYSTEM_CONFLICT },
    ];
    for (const c of cases) {
      updateControl.mockRejectedValueOnce({
        status: c.status,
        statusText: "Error",
        body: null,
      });
      const { unmount } = render(<ControlDetailPanel name="qaCtl" onBack={() => undefined} />);
      await waitFor(() => {
        expect(screen.getByTestId("developer-ctl-save")).toBeTruthy();
      });
      fireEvent.click(screen.getByTestId("developer-ctl-save"));
      await waitFor(() => {
        expect(screen.getByTestId("developer-ctl-detail-error").textContent).toBe(
          `${c.fallback} (${c.status})`,
        );
      });
      unmount();
    }
  });

  it("deletes after CatalogConfirmDialog and does not use window.confirm", async () => {
    const confirmSpy = vi.spyOn(window, "confirm");
    getControlDetail.mockResolvedValue({
      ...sampleDetail,
      name: "qaCtl",
      displayName: "QA",
      scope: "user",
    });
    deleteControl.mockResolvedValue(undefined);
    const onDeleted = vi.fn();
    render(<ControlDetailPanel name="qaCtl" onBack={() => undefined} onDeleted={onDeleted} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ctl-delete")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-ctl-delete"));
    expect(screen.getByTestId("developer-catalog-confirm-dialog")).toBeTruthy();
    fireEvent.click(screen.getByTestId("developer-catalog-confirm-submit"));
    await waitFor(() => {
      expect(onDeleted).toHaveBeenCalled();
    });
    expect(deleteControl).toHaveBeenCalledWith("qaCtl");
    expect(confirmSpy).not.toHaveBeenCalled();
    confirmSpy.mockRestore();
  });

  it("surfaces DELETE 409 for a system-control conflict", async () => {
    getControlDetail.mockResolvedValue({
      ...sampleDetail,
      name: "qaCtl",
      scope: "user",
    });
    deleteControl.mockRejectedValue({
      status: 409,
      statusText: "Conflict",
      body: null,
    });
    render(<ControlDetailPanel name="qaCtl" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ctl-delete")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-ctl-delete"));
    fireEvent.click(screen.getByTestId("developer-catalog-confirm-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ctl-detail-error").textContent).toBe(
        `${DEV_MSG.CTL_SYSTEM_CONFLICT} (409)`,
      );
    });
  });
});
