/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import React from "react";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import {
  getSharedFieldGroupDetail,
  listSharedFieldGroups,
} from "../../../main/ts/api/developer/sharedFieldsApi";
import { DEV_MSG } from "../../../main/ts/developer/messages";
import { SharedFieldsPanel } from "../../../main/ts/developer/SharedFieldsPanel";

vi.mock("../../../main/ts/api/developer/sharedFieldsApi", async (importOriginal) => {
  const actual = await importOriginal<
    typeof import("../../../main/ts/api/developer/sharedFieldsApi")
  >();
  return {
    ...actual,
    listSharedFieldGroups: vi.fn(),
    getSharedFieldGroupDetail: vi.fn(),
    createSharedFieldGroup: vi.fn(),
    updateSharedFieldGroup: vi.fn(),
    deleteSharedFieldGroup: vi.fn(),
  };
});

const listMock = vi.mocked(listSharedFieldGroups);
const detailMock = vi.mocked(getSharedFieldGroupDetail);

describe("SharedFieldsPanel", () => {
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
    expect(screen.getByTestId("developer-sf-new")).toBeTruthy();
  });

  it("opens group detail from catalog row", async () => {
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
      designGaps: ["control properties not in this chrome"],
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
    expect(screen.getByTestId("developer-sf-save")).toBeTruthy();
    expect(screen.getByTestId("developer-sf-delete")).toBeTruthy();

    fireEvent.click(screen.getByTestId("developer-sf-back"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-sf-table")).toBeTruthy();
    });
  });

  it("opens create chrome from New shared field group", async () => {
    listMock.mockResolvedValue([]);
    render(<SharedFieldsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sf-empty")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-sf-new"));
    expect(screen.getByTestId("developer-sf-detail")).toBeTruthy();
    expect(screen.getByTestId("developer-sf-save")).toBeDisabled();
    expect(detailMock).not.toHaveBeenCalled();
  });

  it("shows empty state", async () => {
    listMock.mockResolvedValue([]);
    render(<SharedFieldsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sf-empty")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sf-new")).toBeTruthy();
  });

  it("shows session-redirect message via panelErrMsg", async () => {
    listMock.mockRejectedValue(new SessionRedirectError());
    render(<SharedFieldsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sf-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sf-error").textContent).toBe(DEV_MSG.SESSION_REDIRECT);
    expect(screen.queryByTestId("developer-sf-empty")).toBeNull();
  });

  it("shows ApiError status via panelErrMsg", async () => {
    listMock.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
      body: null,
    });
    render(<SharedFieldsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sf-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sf-error").textContent).toBe(
      `${DEV_MSG.SF_ERROR} (500)`,
    );
  });

  it("shows Error.message via panelErrMsg", async () => {
    listMock.mockRejectedValue(new Error("network down"));
    render(<SharedFieldsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sf-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sf-error").textContent).toBe(
      `${DEV_MSG.SF_ERROR} network down`,
    );
    expect(screen.queryByTestId("developer-sf-table")).toBeNull();
  });

  it("shows fallback when rejection has no message", async () => {
    listMock.mockRejectedValue("boom");
    render(<SharedFieldsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sf-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sf-error").textContent).toBe(DEV_MSG.SF_ERROR);
  });
});
