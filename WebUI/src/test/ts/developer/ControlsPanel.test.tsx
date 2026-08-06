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

vi.mock("../../../main/ts/api/developer/controlsApi", () => ({
  listControls: vi.fn(),
  getControlDetail: vi.fn(),
}));

const listControls = controlsApi.listControls as ReturnType<typeof vi.fn>;
const getControlDetail = controlsApi.getControlDetail as ReturnType<typeof vi.fn>;

describe("ControlsPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    listControls.mockReset();
    getControlDetail.mockReset();
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
});
