/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import * as itemFiltersApi from "../../../main/ts/api/developer/itemFiltersApi";
import { ItemFilterDetailPanel } from "../../../main/ts/developer/ItemFilterDetailPanel";
import { DEV_MSG } from "../../../main/ts/developer/messages";

vi.mock("../../../main/ts/api/developer/itemFiltersApi", () => ({
  listItemFilters: vi.fn(),
  getItemFilterDetail: vi.fn(),
}));

const getItemFilterDetail = itemFiltersApi.getItemFilterDetail as ReturnType<typeof vi.fn>;

const sampleDetail = {
  name: "publicItems",
  description: "Public content filter",
  filterId: { stringValue: "0-1-50" },
  legacyAuthtype: 1,
  parentFilter: { name: "allItems" },
  rules: [
    {
      name: "sys_filterByPublishable",
      ruleId: { stringValue: "0-1-51" },
      params: [{ name: "state", value: "public" }],
    },
  ],
};

describe("ItemFilterDetailPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    getItemFilterDetail.mockReset();
  });

  it("loads detail on success and supports back", async () => {
    getItemFilterDetail.mockResolvedValue(sampleDetail);
    const onBack = vi.fn();
    render(<ItemFilterDetailPanel idOrName="publicItems" onBack={onBack} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-if-detail-title")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-if-detail-title").textContent).toContain("publicItems");
    expect(screen.getByTestId("developer-if-rules-table")).toBeTruthy();
    expect(screen.getByTestId("developer-if-gaps")).toBeTruthy();
    expect(getItemFilterDetail).toHaveBeenCalledWith("publicItems");
    fireEvent.click(screen.getByTestId("developer-if-back"));
    expect(onBack).toHaveBeenCalled();
  });

  it("shows empty rules section when detail has none", async () => {
    getItemFilterDetail.mockResolvedValue({ ...sampleDetail, rules: [] });
    render(<ItemFilterDetailPanel idOrName="publicItems" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-if-rules-empty")).toBeTruthy();
    });
    expect(screen.queryByTestId("developer-if-rules-table")).toBeNull();
  });

  it("shows session-redirect message via panelErrMsg", async () => {
    getItemFilterDetail.mockRejectedValue(new SessionRedirectError());
    render(<ItemFilterDetailPanel idOrName="publicItems" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-if-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-if-detail-error").textContent).toBe(
      DEV_MSG.SESSION_REDIRECT,
    );
    expect(screen.queryByTestId("developer-if-detail-loading")).toBeNull();
    expect(screen.queryByTestId("developer-if-detail-title")).toBeNull();
  });

  it("shows ApiError status via panelErrMsg", async () => {
    getItemFilterDetail.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
      body: null,
    });
    render(<ItemFilterDetailPanel idOrName="publicItems" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-if-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-if-detail-error").textContent).toBe(
      `${DEV_MSG.IF_DETAIL_ERROR} (500)`,
    );
  });

  it("shows Error.message via panelErrMsg", async () => {
    getItemFilterDetail.mockRejectedValue(new Error("network down"));
    render(<ItemFilterDetailPanel idOrName="publicItems" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-if-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-if-detail-error").textContent).toBe(
      `${DEV_MSG.IF_DETAIL_ERROR} network down`,
    );
    expect(screen.queryByTestId("developer-if-detail-title")).toBeNull();
  });

  it("shows fallback when rejection has no message", async () => {
    getItemFilterDetail.mockRejectedValue("boom");
    render(<ItemFilterDetailPanel idOrName="publicItems" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-if-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-if-detail-error").textContent).toBe(
      DEV_MSG.IF_DETAIL_ERROR,
    );
  });
});
