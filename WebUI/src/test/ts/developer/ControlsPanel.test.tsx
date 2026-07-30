/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ControlsPanel } from "../../../main/ts/developer/ControlsPanel";
import * as controlsApi from "../../../main/ts/api/developer/controlsApi";

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

  it("shows loading empty and error states", async () => {
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

    listControls.mockRejectedValueOnce(new Error("down"));
    render(<ControlsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ctl-error")).toBeTruthy();
    });
  });
});
