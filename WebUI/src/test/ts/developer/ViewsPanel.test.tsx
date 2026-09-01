/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import React from "react";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import { getViewDetail, listViews } from "../../../main/ts/api/developer/viewsApi";
import { DEV_MSG } from "../../../main/ts/developer/messages";
import { ViewsPanel } from "../../../main/ts/developer/ViewsPanel";

vi.mock("../../../main/ts/api/developer/viewsApi", async (importOriginal) => {
  const actual = await importOriginal<typeof import("../../../main/ts/api/developer/viewsApi")>();
  return {
    ...actual,
    listViews: vi.fn(),
    getViewDetail: vi.fn(),
    createView: vi.fn(),
    saveView: vi.fn(),
    deleteView: vi.fn(),
  };
});

vi.mock("../../../main/ts/developer/ObjectAclSection", () => ({
  ObjectAclSection: (props: {
    objectGuid?: string | null;
    objectKind?: string | null;
    testIdPrefix?: string;
  }) => (
    <div
      data-testid={`${props.testIdPrefix ?? "developer-acl"}-stub`}
      data-object-guid={props.objectGuid ?? ""}
      data-object-kind={props.objectKind ?? ""}
    />
  ),
}));

const listMock = vi.mocked(listViews);
const detailMock = vi.mocked(getViewDetail);

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
    listMock.mockReset();
    detailMock.mockReset();
  });

  afterEach(() => {
    cleanup();
  });

  it("lists views and opens detail", async () => {
    listMock.mockResolvedValue([sampleView]);
    detailMock.mockResolvedValue(sampleDetail);
    render(<ViewsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-vw-table")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-vw-table").textContent).toContain("My View");
    expect(screen.getByTestId("developer-vw-new")).toBeTruthy();
    fireEvent.click(screen.getByTestId("developer-vw-open"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-vw-detail")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-vw-fields-table")).toBeTruthy();
    expect(screen.getByTestId("developer-vw-save")).toBeTruthy();
    expect(screen.getByTestId("developer-vw-delete")).toBeTruthy();
    fireEvent.click(screen.getByTestId("developer-vw-back"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-vw-table")).toBeTruthy();
    });
  });

  it("opens create chrome from New view", async () => {
    listMock.mockResolvedValue([]);
    render(<ViewsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-vw-empty")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-vw-new"));
    expect(screen.getByTestId("developer-vw-detail")).toBeTruthy();
    expect(screen.getByTestId("developer-vw-save")).toBeDisabled();
    expect(detailMock).not.toHaveBeenCalled();
  });

  it("shows empty state when API returns no views", async () => {
    listMock.mockResolvedValue([]);
    render(<ViewsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-vw-empty")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-vw-new")).toBeTruthy();
  });

  it("shows session-redirect message via panelErrMsg", async () => {
    listMock.mockRejectedValue(new SessionRedirectError());
    render(<ViewsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-vw-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-vw-error").textContent).toBe(DEV_MSG.SESSION_REDIRECT);
    expect(screen.queryByTestId("developer-vw-empty")).toBeNull();
  });

  it("shows ApiError status via panelErrMsg", async () => {
    listMock.mockRejectedValue({
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
    listMock.mockRejectedValue(new Error("network down"));
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
    listMock.mockRejectedValue("boom");
    render(<ViewsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-vw-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-vw-error").textContent).toBe(DEV_MSG.VW_ERROR);
  });
});
