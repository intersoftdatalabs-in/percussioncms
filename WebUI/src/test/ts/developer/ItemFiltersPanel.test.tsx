/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import React from "react";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import {
  getItemFilterDetail,
  listItemFilters,
} from "../../../main/ts/api/developer/itemFiltersApi";
import { ItemFiltersPanel } from "../../../main/ts/developer/ItemFiltersPanel";
import { DEV_MSG } from "../../../main/ts/developer/messages";

vi.mock("../../../main/ts/api/developer/itemFiltersApi", async (importOriginal) => {
  const actual = await importOriginal<
    typeof import("../../../main/ts/api/developer/itemFiltersApi")
  >();
  return {
    ...actual,
    listItemFilters: vi.fn(),
    getItemFilterDetail: vi.fn(),
    createItemFilter: vi.fn(),
    updateItemFilter: vi.fn(),
    deleteItemFilter: vi.fn(),
  };
});

const listMock = vi.mocked(listItemFilters);
const detailMock = vi.mocked(getItemFilterDetail);

describe("ItemFiltersPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    listMock.mockReset();
    detailMock.mockReset();
  });

  afterEach(() => {
    cleanup();
  });

  it("renders catalog rows", async () => {
    listMock.mockResolvedValue([
      {
        name: "public",
        description: "Public content",
        rules: [{ name: "sys_IsPublic" }],
        parentFilter: null,
      },
    ]);
    render(<ItemFiltersPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-if-table")).toBeTruthy();
    });
    expect(screen.getByText("public")).toBeTruthy();
    expect(screen.getByTestId("developer-if-new")).toBeTruthy();
  });

  it("opens detail with rules", async () => {
    listMock.mockResolvedValue([{ name: "public", rules: [] }]);
    detailMock.mockResolvedValue({
      name: "public",
      description: "Public content",
      legacyAuthtype: 1,
      filterId: { stringValue: "0-11-1" },
      rules: [{ name: "sys_IsPublic", params: [{ name: "x", value: "1" }] }],
      parentFilter: null,
    });

    render(<ItemFiltersPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-if-table")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-if-open"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-if-detail")).toBeTruthy();
    });
    expect(detailMock).toHaveBeenCalledWith("public");
    expect(screen.getByText("sys_IsPublic")).toBeTruthy();
    expect(screen.getByTestId("developer-if-save")).toBeTruthy();
    expect(screen.getByTestId("developer-if-delete")).toBeTruthy();
  });

  it("opens create chrome from New item filter", async () => {
    listMock.mockResolvedValue([]);
    render(<ItemFiltersPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-if-empty")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-if-new"));
    expect(screen.getByTestId("developer-if-detail")).toBeTruthy();
    expect(screen.getByTestId("developer-if-save")).toBeDisabled();
    expect(detailMock).not.toHaveBeenCalled();
  });

  it("shows empty state when API returns no item filters", async () => {
    listMock.mockResolvedValue([]);
    render(<ItemFiltersPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-if-empty")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-if-new")).toBeTruthy();
  });

  it("shows session-redirect message via panelErrMsg", async () => {
    listMock.mockRejectedValue(new SessionRedirectError());
    render(<ItemFiltersPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-if-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-if-error").textContent).toBe(DEV_MSG.SESSION_REDIRECT);
    expect(screen.queryByTestId("developer-if-empty")).toBeNull();
  });

  it("shows ApiError status via panelErrMsg", async () => {
    listMock.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
      body: null,
    });
    render(<ItemFiltersPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-if-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-if-error").textContent).toBe(`${DEV_MSG.IF_ERROR} (500)`);
  });

  it("shows Error.message via panelErrMsg", async () => {
    listMock.mockRejectedValue(new Error("network down"));
    render(<ItemFiltersPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-if-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-if-error").textContent).toBe(
      `${DEV_MSG.IF_ERROR} network down`,
    );
    expect(screen.queryByTestId("developer-if-table")).toBeNull();
  });

  it("shows fallback when rejection has no message", async () => {
    listMock.mockRejectedValue("boom");
    render(<ItemFiltersPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-if-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-if-error").textContent).toBe(DEV_MSG.IF_ERROR);
  });
});
