/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import React from "react";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  getItemFilterDetail,
  listItemFilters,
} from "../../../main/ts/api/developer/itemFiltersApi";
import { ItemFiltersPanel } from "../../../main/ts/developer/ItemFiltersPanel";

vi.mock("../../../main/ts/api/developer/itemFiltersApi", () => ({
  listItemFilters: vi.fn(),
  getItemFilterDetail: vi.fn(),
}));

const listMock = vi.mocked(listItemFilters);
const detailMock = vi.mocked(getItemFilterDetail);

describe("ItemFiltersPanel", () => {
  beforeEach(() => {
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
  });

  it("shows error UI when list fails", async () => {
    listMock.mockRejectedValue({ status: 500, statusText: "Error" });
    render(<ItemFiltersPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-if-error")).toBeTruthy();
    });
  });
});
