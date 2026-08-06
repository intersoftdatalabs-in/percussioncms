/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import * as viewsApi from "../../../main/ts/api/developer/viewsApi";
import { DEV_MSG } from "../../../main/ts/developer/messages";
import { ViewDetailPanel } from "../../../main/ts/developer/ViewDetailPanel";

vi.mock("../../../main/ts/api/developer/viewsApi", () => ({
  listViews: vi.fn(),
  getViewDetail: vi.fn(),
}));

const getViewDetail = viewsApi.getViewDetail as ReturnType<typeof vi.fn>;

const sampleDetail = {
  name: "My View",
  label: "My View",
  description: "Custom view",
  displayFormatId: "Default",
  maximumResultSize: 50,
  caseSensitive: true,
  fields: [{ fieldName: "sys_contentid", operator: "=", fieldValue: "1", fieldType: "number" }],
  designGaps: ["gap-a"],
};

describe("ViewDetailPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    getViewDetail.mockReset();
  });

  it("loads detail on success and supports back", async () => {
    getViewDetail.mockResolvedValue(sampleDetail);
    const onBack = vi.fn();
    render(<ViewDetailPanel idOrName="My View" onBack={onBack} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-vw-detail-title")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-vw-detail-title").textContent).toContain("My View");
    expect(screen.getByTestId("developer-vw-fields-table")).toBeTruthy();
    expect(screen.getByTestId("developer-vw-gaps").textContent).toContain("gap-a");
    expect(getViewDetail).toHaveBeenCalledWith("My View");
    fireEvent.click(screen.getByTestId("developer-vw-back"));
    expect(onBack).toHaveBeenCalled();
  });

  it("shows empty fields section when detail has none", async () => {
    getViewDetail.mockResolvedValue({ ...sampleDetail, fields: [] });
    render(<ViewDetailPanel idOrName="My View" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-vw-fields-empty")).toBeTruthy();
    });
    expect(screen.queryByTestId("developer-vw-fields-table")).toBeNull();
  });

  it("shows session-redirect message via panelErrMsg", async () => {
    getViewDetail.mockRejectedValue(new SessionRedirectError());
    render(<ViewDetailPanel idOrName="My View" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-vw-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-vw-detail-error").textContent).toBe(
      DEV_MSG.SESSION_REDIRECT,
    );
    expect(screen.queryByTestId("developer-vw-detail-loading")).toBeNull();
    expect(screen.queryByTestId("developer-vw-detail-title")).toBeNull();
  });

  it("shows ApiError status via panelErrMsg", async () => {
    getViewDetail.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
      body: null,
    });
    render(<ViewDetailPanel idOrName="My View" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-vw-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-vw-detail-error").textContent).toBe(
      `${DEV_MSG.VW_DETAIL_ERROR} (500)`,
    );
  });

  it("shows Error.message via panelErrMsg", async () => {
    getViewDetail.mockRejectedValue(new Error("network down"));
    render(<ViewDetailPanel idOrName="My View" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-vw-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-vw-detail-error").textContent).toBe(
      `${DEV_MSG.VW_DETAIL_ERROR} network down`,
    );
    expect(screen.queryByTestId("developer-vw-detail-title")).toBeNull();
  });

  it("shows fallback when rejection has no message", async () => {
    getViewDetail.mockRejectedValue("boom");
    render(<ViewDetailPanel idOrName="My View" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-vw-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-vw-detail-error").textContent).toBe(
      DEV_MSG.VW_DETAIL_ERROR,
    );
  });
});
