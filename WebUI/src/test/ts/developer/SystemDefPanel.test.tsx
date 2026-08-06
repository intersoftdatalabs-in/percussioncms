/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import React from "react";
import { cleanup, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import { getSystemDef } from "../../../main/ts/api/developer/systemDefApi";
import { DEV_MSG } from "../../../main/ts/developer/messages";
import { SystemDefPanel } from "../../../main/ts/developer/SystemDefPanel";

vi.mock("../../../main/ts/api/developer/systemDefApi", () => ({
  getSystemDef: vi.fn(),
}));

const getMock = vi.mocked(getSystemDef);

describe("SystemDefPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    getMock.mockReset();
  });

  afterEach(() => {
    cleanup();
  });

  it("renders system field catalog when load succeeds", async () => {
    getMock.mockResolvedValue({
      fieldCount: 1,
      cacheTimeoutMinutes: 15,
      fields: [
        {
          name: "sys_title",
          dataType: "text",
          required: true,
          searchable: true,
          readOnly: false,
          occurrence: "required",
        },
      ],
      designGaps: ["write not supported"],
    });
    render(<SystemDefPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sys-fields-table")).toBeTruthy();
    });
    expect(screen.getByText("sys_title")).toBeTruthy();
    expect(screen.getByTestId("developer-sys-gaps")).toBeTruthy();
  });

  it("shows empty state when no fields", async () => {
    getMock.mockResolvedValue({ fieldCount: 0, fields: [], designGaps: [] });
    render(<SystemDefPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sys-empty")).toBeTruthy();
    });
  });

  it("shows session-redirect message via panelErrMsg", async () => {
    getMock.mockRejectedValue(new SessionRedirectError());
    render(<SystemDefPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sys-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sys-error").textContent).toBe(DEV_MSG.SESSION_REDIRECT);
    expect(screen.queryByTestId("developer-sys-empty")).toBeNull();
  });

  it("shows ApiError status via panelErrMsg", async () => {
    getMock.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
      body: null,
    });
    render(<SystemDefPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sys-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sys-error").textContent).toBe(
      `${DEV_MSG.SYS_ERROR} (500)`,
    );
  });

  it("shows Error.message via panelErrMsg", async () => {
    getMock.mockRejectedValue(new Error("network down"));
    render(<SystemDefPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sys-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sys-error").textContent).toBe(
      `${DEV_MSG.SYS_ERROR} network down`,
    );
    expect(screen.queryByTestId("developer-sys-fields-table")).toBeNull();
  });

  it("shows fallback when rejection has no message", async () => {
    getMock.mockRejectedValue("boom");
    render(<SystemDefPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sys-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sys-error").textContent).toBe(DEV_MSG.SYS_ERROR);
  });
});
