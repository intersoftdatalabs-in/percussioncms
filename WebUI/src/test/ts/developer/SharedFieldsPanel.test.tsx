/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import React from "react";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  getSharedFieldGroupDetail,
  listSharedFieldGroups,
} from "../../../main/ts/api/developer/sharedFieldsApi";
import { SharedFieldsPanel } from "../../../main/ts/developer/SharedFieldsPanel";

vi.mock("../../../main/ts/api/developer/sharedFieldsApi", () => ({
  listSharedFieldGroups: vi.fn(),
  getSharedFieldGroupDetail: vi.fn(),
}));

const listMock = vi.mocked(listSharedFieldGroups);
const detailMock = vi.mocked(getSharedFieldGroupDetail);

describe("SharedFieldsPanel", () => {
  beforeEach(() => {
    listMock.mockReset();
    detailMock.mockReset();
  });

  afterEach(() => {
    cleanup();
  });

  it("renders catalog rows when groups load", async () => {
    listMock.mockResolvedValue([
      { name: "shared", filename: "shared.xml", fieldCount: 2 },
    ]);
    render(<SharedFieldsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sf-table")).toBeTruthy();
    });
    expect(screen.getByText("shared")).toBeTruthy();
    expect(screen.getByText("shared.xml")).toBeTruthy();
  });

  it("opens read-only group detail from catalog row", async () => {
    listMock.mockResolvedValue([
      { name: "shared", filename: "shared.xml", fieldCount: 1 },
    ]);
    detailMock.mockResolvedValue({
      name: "shared",
      filename: "shared.xml",
      fields: [
        {
          name: "rx_title",
          dataType: "text",
          required: true,
          searchable: true,
          readOnly: false,
          occurrence: "required",
        },
      ],
      designGaps: ["write not supported"],
    });

    render(<SharedFieldsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sf-table")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-sf-open"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-sf-detail")).toBeTruthy();
    });
    expect(detailMock).toHaveBeenCalledWith("shared");
    expect(screen.getByTestId("developer-sf-detail-title").textContent).toBe("shared");
    expect(screen.getByText("rx_title")).toBeTruthy();
    expect(screen.getByTestId("developer-sf-gaps")).toBeTruthy();

    fireEvent.click(screen.getByTestId("developer-sf-back"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-sf-table")).toBeTruthy();
    });
  });

  it("shows empty state", async () => {
    listMock.mockResolvedValue([]);
    render(<SharedFieldsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sf-empty")).toBeTruthy();
    });
  });

  it("shows error UI when list fails", async () => {
    listMock.mockRejectedValue({ status: 500, statusText: "Error" });
    render(<SharedFieldsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sf-error")).toBeTruthy();
    });
  });
});
