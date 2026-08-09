/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import * as displayFormatsApi from "../../../main/ts/api/developer/displayFormatsApi";
import { DisplayFormatDetailPanel } from "../../../main/ts/developer/DisplayFormatDetailPanel";
import { DEV_MSG } from "../../../main/ts/developer/messages";

vi.mock("../../../main/ts/api/developer/displayFormatsApi", () => ({
  listDisplayFormats: vi.fn(),
  getDisplayFormatDetail: vi.fn(),
  normalizeColumns: (c: unknown) => (Array.isArray(c) ? c : []),
}));

// ObjectAclSection loads ACL via separate API; stub to isolate detail load + assert wiring.
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

const getDisplayFormatDetail = displayFormatsApi.getDisplayFormatDetail as ReturnType<
  typeof vi.fn
>;

const sampleDetail = {
  name: "Default",
  label: "Default View",
  description: "System default",
  guid: { stringValue: "0-1-100" },
  validForFolder: true,
  validForViewsAndSearches: true,
  validForRelatedContent: false,
  columns: [
    { source: "sys_title", displayName: "Title", position: 0, renderType: "text", width: 200 },
  ],
};

describe("DisplayFormatDetailPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    getDisplayFormatDetail.mockReset();
  });

  it("loads detail on success and supports back", async () => {
    getDisplayFormatDetail.mockResolvedValue(sampleDetail);
    const onBack = vi.fn();
    render(<DisplayFormatDetailPanel idOrName="Default" onBack={onBack} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-df-detail-title")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-df-detail-title").textContent).toContain("Default View");
    expect(screen.getByTestId("developer-df-columns-table")).toBeTruthy();
    expect(screen.getByTestId("developer-df-gaps")).toBeTruthy();
    expect(getDisplayFormatDetail).toHaveBeenCalledWith("Default");
    fireEvent.click(screen.getByTestId("developer-df-back"));
    expect(onBack).toHaveBeenCalled();
  });

  it("mounts ObjectAclSection with display-format kind and object guid", async () => {
    getDisplayFormatDetail.mockResolvedValue(sampleDetail);
    render(<DisplayFormatDetailPanel idOrName="Default" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-df-acl-stub")).toBeTruthy();
    });
    const acl = screen.getByTestId("developer-df-acl-stub");
    expect(acl.getAttribute("data-object-kind")).toBe("display-format");
    expect(acl.getAttribute("data-object-guid")).toBe("0-1-100");
  });

  it("shows empty columns section when detail has none", async () => {
    getDisplayFormatDetail.mockResolvedValue({ ...sampleDetail, columns: [] });
    render(<DisplayFormatDetailPanel idOrName="Default" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-df-columns-empty")).toBeTruthy();
    });
    expect(screen.queryByTestId("developer-df-columns-table")).toBeNull();
  });

  it("shows session-redirect message via panelErrMsg", async () => {
    getDisplayFormatDetail.mockRejectedValue(new SessionRedirectError());
    render(<DisplayFormatDetailPanel idOrName="Default" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-df-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-df-detail-error").textContent).toBe(
      DEV_MSG.SESSION_REDIRECT,
    );
    expect(screen.queryByTestId("developer-df-detail-loading")).toBeNull();
    expect(screen.queryByTestId("developer-df-detail-title")).toBeNull();
  });

  it("shows ApiError status via panelErrMsg", async () => {
    getDisplayFormatDetail.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
      body: null,
    });
    render(<DisplayFormatDetailPanel idOrName="Default" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-df-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-df-detail-error").textContent).toBe(
      `${DEV_MSG.DF_DETAIL_ERROR} (500)`,
    );
  });

  it("shows Error.message via panelErrMsg", async () => {
    getDisplayFormatDetail.mockRejectedValue(new Error("network down"));
    render(<DisplayFormatDetailPanel idOrName="Default" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-df-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-df-detail-error").textContent).toBe(
      `${DEV_MSG.DF_DETAIL_ERROR} network down`,
    );
    expect(screen.queryByTestId("developer-df-detail-title")).toBeNull();
  });

  it("shows fallback when rejection has no message", async () => {
    getDisplayFormatDetail.mockRejectedValue("boom");
    render(<DisplayFormatDetailPanel idOrName="Default" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-df-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-df-detail-error").textContent).toBe(
      DEV_MSG.DF_DETAIL_ERROR,
    );
  });
});
