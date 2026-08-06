/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import * as displayFormatsApi from "../../../main/ts/api/developer/displayFormatsApi";
import { DisplayFormatsPanel } from "../../../main/ts/developer/DisplayFormatsPanel";
import { DEV_MSG } from "../../../main/ts/developer/messages";

vi.mock("../../../main/ts/api/developer/displayFormatsApi", () => ({
  listDisplayFormats: vi.fn(),
  getDisplayFormatDetail: vi.fn(),
  normalizeColumns: (c: unknown) => (Array.isArray(c) ? c : []),
}));

const listDisplayFormats = displayFormatsApi.listDisplayFormats as ReturnType<typeof vi.fn>;
const getDisplayFormatDetail = displayFormatsApi.getDisplayFormatDetail as ReturnType<
  typeof vi.fn
>;

const sampleFormat = {
  name: "Default",
  label: "Default View",
  description: "System default",
  validForFolder: true,
  validForViewsAndSearches: true,
  columns: [{ source: "sys_title", displayName: "Title", position: 0 }],
};

const sampleDetail = {
  name: "Default",
  label: "Default View",
  columns: [{ source: "sys_title", displayName: "Title", position: 0, renderType: "text" }],
};

describe("DisplayFormatsPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    listDisplayFormats.mockReset();
    getDisplayFormatDetail.mockReset();
  });

  it("lists display formats and opens detail", async () => {
    listDisplayFormats.mockResolvedValue([sampleFormat]);
    getDisplayFormatDetail.mockResolvedValue(sampleDetail);
    render(<DisplayFormatsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-df-table")).toBeTruthy();
    });
    expect(screen.getByText("Default")).toBeTruthy();
    fireEvent.click(screen.getByTestId("developer-df-open"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-df-detail")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-df-columns-table")).toBeTruthy();
    fireEvent.click(screen.getByTestId("developer-df-back"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-df-table")).toBeTruthy();
    });
  });

  it("shows empty state when API returns no display formats", async () => {
    listDisplayFormats.mockResolvedValue([]);
    render(<DisplayFormatsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-df-empty")).toBeTruthy();
    });
  });

  it("shows session-redirect message via panelErrMsg", async () => {
    listDisplayFormats.mockRejectedValue(new SessionRedirectError());
    render(<DisplayFormatsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-df-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-df-error").textContent).toBe(DEV_MSG.SESSION_REDIRECT);
    expect(screen.queryByTestId("developer-df-empty")).toBeNull();
  });

  it("shows ApiError status via panelErrMsg", async () => {
    listDisplayFormats.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
      body: null,
    });
    render(<DisplayFormatsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-df-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-df-error").textContent).toBe(`${DEV_MSG.DF_ERROR} (500)`);
  });

  it("shows Error.message via panelErrMsg", async () => {
    listDisplayFormats.mockRejectedValue(new Error("network down"));
    render(<DisplayFormatsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-df-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-df-error").textContent).toBe(
      `${DEV_MSG.DF_ERROR} network down`,
    );
    expect(screen.queryByTestId("developer-df-table")).toBeNull();
  });

  it("shows fallback when rejection has no message", async () => {
    listDisplayFormats.mockRejectedValue("boom");
    render(<DisplayFormatsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-df-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-df-error").textContent).toBe(DEV_MSG.DF_ERROR);
  });
});
