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

vi.mock("../../../main/ts/api/developer/controlsApi", () => ({
  listControls: vi.fn(),
  getControlDetail: vi.fn(),
}));

const getControlDetail = controlsApi.getControlDetail as ReturnType<typeof vi.fn>;

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
});
