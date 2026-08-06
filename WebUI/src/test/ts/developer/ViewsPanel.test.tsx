/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import * as viewsApi from "../../../main/ts/api/developer/viewsApi";
import { DEV_MSG } from "../../../main/ts/developer/messages";
import { ViewsPanel } from "../../../main/ts/developer/ViewsPanel";

vi.mock("../../../main/ts/api/developer/viewsApi", () => ({
  listViews: vi.fn(),
  getViewDetail: vi.fn(),
}));

const listViews = viewsApi.listViews as ReturnType<typeof vi.fn>;
const getViewDetail = viewsApi.getViewDetail as ReturnType<typeof vi.fn>;

const sampleView = {
  name: "My View",
  label: "My View",
  standardView: true,
  fields: [{ fieldName: "sys_title", operator: "=", fieldValue: "*" }],
};

const sampleDetail = {
  name: "My View",
  label: "My View",
  fields: [{ fieldName: "sys_title", operator: "=", fieldValue: "*" }],
  designGaps: ["gap"],
};

describe("ViewsPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    listViews.mockReset();
    getViewDetail.mockReset();
  });

  it("lists views and opens detail", async () => {
    listViews.mockResolvedValue([sampleView]);
    getViewDetail.mockResolvedValue(sampleDetail);
    render(<ViewsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-vw-table")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-vw-table").textContent).toContain("My View");
    fireEvent.click(screen.getByTestId("developer-vw-open"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-vw-detail")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-vw-fields-table")).toBeTruthy();
    fireEvent.click(screen.getByTestId("developer-vw-back"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-vw-table")).toBeTruthy();
    });
  });

  it("shows empty state when API returns no views", async () => {
    listViews.mockResolvedValue([]);
    render(<ViewsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-vw-empty")).toBeTruthy();
    });
  });

  it("shows session-redirect message via panelErrMsg", async () => {
    listViews.mockRejectedValue(new SessionRedirectError());
    render(<ViewsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-vw-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-vw-error").textContent).toBe(DEV_MSG.SESSION_REDIRECT);
    expect(screen.queryByTestId("developer-vw-empty")).toBeNull();
  });

  it("shows ApiError status via panelErrMsg", async () => {
    listViews.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
      body: null,
    });
    render(<ViewsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-vw-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-vw-error").textContent).toBe(`${DEV_MSG.VW_ERROR} (500)`);
  });

  it("shows Error.message via panelErrMsg", async () => {
    listViews.mockRejectedValue(new Error("network down"));
    render(<ViewsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-vw-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-vw-error").textContent).toBe(
      `${DEV_MSG.VW_ERROR} network down`,
    );
    expect(screen.queryByTestId("developer-vw-table")).toBeNull();
  });

  it("shows fallback when rejection has no message", async () => {
    listViews.mockRejectedValue("boom");
    render(<ViewsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-vw-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-vw-error").textContent).toBe(DEV_MSG.VW_ERROR);
  });
});
