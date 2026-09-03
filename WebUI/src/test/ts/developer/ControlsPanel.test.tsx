/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import * as controlsApi from "../../../main/ts/api/developer/controlsApi";
import { ControlsPanel } from "../../../main/ts/developer/ControlsPanel";
import { DEV_MSG } from "../../../main/ts/developer/messages";

vi.mock("../../../main/ts/api/developer/controlsApi", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("../../../main/ts/api/developer/controlsApi")>();
  return {
    ...actual,
    listControls: vi.fn(),
    getControlDetail: vi.fn(),
    createControl: vi.fn(),
    updateControl: vi.fn(),
    deleteControl: vi.fn(),
  };
});

const listControls = controlsApi.listControls as ReturnType<typeof vi.fn>;
const getControlDetail = controlsApi.getControlDetail as ReturnType<typeof vi.fn>;
const createControl = controlsApi.createControl as ReturnType<typeof vi.fn>;
const deleteControl = controlsApi.deleteControl as ReturnType<typeof vi.fn>;

describe("ControlsPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    listControls.mockReset();
    getControlDetail.mockReset();
    createControl.mockReset();
    deleteControl.mockReset();
  });

  it("lists controls and opens detail", async () => {
    listControls.mockResolvedValue([
      {
        name: "sys_EditBox",
        displayName: "Edit Box",
        scope: "system",
        dimension: "single",
      },
    ]);
    getControlDetail.mockResolvedValue({
      name: "sys_EditBox",
      displayName: "Edit Box",
      scope: "system",
      parameters: [{ name: "maxlength", dataType: "number", required: false }],
      designGaps: ["gap-a"],
    });
    render(<ControlsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ctl-table")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-ctl-open"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ctl-detail")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ctl-params-table")).toBeTruthy();
    expect(screen.getByTestId("developer-ctl-gaps").textContent).toContain("gap-a");
  });

  it("shows loading empty and empty state", async () => {
    let resolveList!: (v: unknown) => void;
    listControls.mockReturnValue(
      new Promise((resolve) => {
        resolveList = resolve;
      }),
    );
    const { unmount } = render(<ControlsPanel />);
    expect(screen.getByTestId("developer-ctl-loading")).toBeTruthy();
    resolveList([]);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ctl-empty")).toBeTruthy();
    });
    unmount();
  });

  it("shows session-redirect message via panelErrMsg", async () => {
    listControls.mockRejectedValue(new SessionRedirectError());
    render(<ControlsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ctl-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ctl-error").textContent).toBe(DEV_MSG.SESSION_REDIRECT);
    expect(screen.queryByTestId("developer-ctl-empty")).toBeNull();
  });

  it("shows ApiError status via panelErrMsg", async () => {
    listControls.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
      body: null,
    });
    render(<ControlsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ctl-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ctl-error").textContent).toBe(
      `${DEV_MSG.CTL_ERROR} (500)`,
    );
  });

  it("shows Error.message via panelErrMsg", async () => {
    listControls.mockRejectedValue(new Error("network down"));
    render(<ControlsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ctl-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ctl-error").textContent).toBe(
      `${DEV_MSG.CTL_ERROR} network down`,
    );
    expect(screen.queryByTestId("developer-ctl-table")).toBeNull();
  });

  it("shows fallback when rejection has no message", async () => {
    listControls.mockRejectedValue("boom");
    render(<ControlsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ctl-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ctl-error").textContent).toBe(DEV_MSG.CTL_ERROR);
  });

  it("shows New user control on empty catalog", async () => {
    listControls.mockResolvedValue([]);
    render(<ControlsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ctl-empty")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ctl-new")).toBeTruthy();
  });

  it("opens create chrome from New user control", async () => {
    listControls.mockResolvedValue([]);
    render(<ControlsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ctl-new")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-ctl-new"));
    expect(screen.getByTestId("developer-ctl-create")).toBeTruthy();
    expect(screen.getByTestId("developer-ctl-create-save")).toBeDisabled();
  });

  it("create opens the new control detail", async () => {
    listControls
      .mockResolvedValueOnce([])
      .mockResolvedValue([
        { name: "qaCtl", displayName: "QA", scope: "user", dimension: "single" },
      ]);
    createControl.mockResolvedValue({
      name: "qaCtl",
      displayName: "QA",
      scope: "user",
    });
    getControlDetail.mockResolvedValue({
      name: "qaCtl",
      displayName: "QA",
      scope: "user",
      parameters: [],
      designGaps: [],
    });
    render(<ControlsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ctl-new")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-ctl-new"));
    fireEvent.change(screen.getByTestId("developer-ctl-create-name"), {
      target: { value: "qaCtl" },
    });
    fireEvent.click(screen.getByTestId("developer-ctl-create-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ctl-detail")).toBeTruthy();
    });
    expect(createControl).toHaveBeenCalledWith(expect.objectContaining({ name: "qaCtl" }));
    await waitFor(() => {
      expect(getControlDetail).toHaveBeenCalledWith("qaCtl");
    });
    expect(screen.getByTestId("developer-ctl-detail-name").textContent).toBe("qaCtl");
    expect(screen.queryByTestId("developer-ctl-create-name")).toBeNull();
    fireEvent.click(screen.getByTestId("developer-ctl-back"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ctl-table").textContent).toContain("qaCtl");
    });
  });

  it("delete removes the row from the catalog", async () => {
    listControls
      .mockResolvedValueOnce([
        { name: "qaCtl", displayName: "QA", scope: "user", dimension: "single" },
        { name: "sys_EditBox", displayName: "Edit Box", scope: "system", dimension: "single" },
      ])
      .mockResolvedValue([
        { name: "sys_EditBox", displayName: "Edit Box", scope: "system", dimension: "single" },
      ]);
    getControlDetail.mockResolvedValue({
      name: "qaCtl",
      displayName: "QA",
      scope: "user",
      parameters: [],
      designGaps: [],
    });
    deleteControl.mockResolvedValue(undefined);
    render(<ControlsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ctl-table").textContent).toContain("qaCtl");
    });
    fireEvent.click(screen.getAllByTestId("developer-ctl-open")[0]);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ctl-delete")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-ctl-delete"));
    fireEvent.click(screen.getByTestId("developer-catalog-confirm-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ctl-table")).toBeTruthy();
    });
    expect(deleteControl).toHaveBeenCalledWith("qaCtl");
    expect(screen.getByTestId("developer-ctl-table").textContent).not.toContain("qaCtl");
    expect(screen.getByTestId("developer-ctl-table").textContent).toContain("sys_EditBox");
  });
});
