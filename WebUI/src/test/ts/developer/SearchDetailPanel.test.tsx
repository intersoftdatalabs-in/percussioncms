/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import * as searchesApi from "../../../main/ts/api/developer/searchesApi";
import { DEV_MSG } from "../../../main/ts/developer/messages";
import { SearchDetailPanel } from "../../../main/ts/developer/SearchDetailPanel";

vi.mock("../../../main/ts/api/developer/searchesApi", () => ({
  listSearches: vi.fn(),
  getSearchDetail: vi.fn(),
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

const getSearchDetail = searchesApi.getSearchDetail as ReturnType<typeof vi.fn>;

const sampleDetail = {
  name: "All Content",
  label: "All Content",
  description: "System search",
  displayFormatId: "Default",
  maximumResultSize: 100,
  caseSensitive: false,
  guid: { stringValue: "0-26-7" },
  fields: [{ fieldName: "sys_title", operator: "=", fieldValue: "*", fieldType: "text" }],
  designGaps: ["gap-a"],
};

describe("SearchDetailPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    getSearchDetail.mockReset();
  });

  it("loads detail on success and supports back", async () => {
    getSearchDetail.mockResolvedValue(sampleDetail);
    const onBack = vi.fn();
    render(<SearchDetailPanel idOrName="All Content" onBack={onBack} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sr-detail-title")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sr-detail-title").textContent).toContain("All Content");
    expect(screen.getByTestId("developer-sr-fields-table")).toBeTruthy();
    expect(screen.getByTestId("developer-sr-gaps").textContent).toContain("gap-a");
    expect(getSearchDetail).toHaveBeenCalledWith("All Content");
    const acl = screen.getByTestId("developer-sr-acl-stub");
    expect(acl.getAttribute("data-object-kind")).toBe("search");
    expect(acl.getAttribute("data-object-guid")).toBe("0-26-7");
    fireEvent.click(screen.getByTestId("developer-sr-back"));
    expect(onBack).toHaveBeenCalled();
  });

  it("shows empty fields section when detail has none", async () => {
    getSearchDetail.mockResolvedValue({ ...sampleDetail, fields: [] });
    render(<SearchDetailPanel idOrName="All Content" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sr-fields-empty")).toBeTruthy();
    });
    expect(screen.queryByTestId("developer-sr-fields-table")).toBeNull();
  });

  it("shows session-redirect message via panelErrMsg", async () => {
    getSearchDetail.mockRejectedValue(new SessionRedirectError());
    render(<SearchDetailPanel idOrName="All Content" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sr-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sr-detail-error").textContent).toBe(
      DEV_MSG.SESSION_REDIRECT,
    );
    expect(screen.queryByTestId("developer-sr-detail-loading")).toBeNull();
    expect(screen.queryByTestId("developer-sr-detail-title")).toBeNull();
  });

  it("shows ApiError status via panelErrMsg", async () => {
    getSearchDetail.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
      body: null,
    });
    render(<SearchDetailPanel idOrName="All Content" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sr-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sr-detail-error").textContent).toBe(
      `${DEV_MSG.SR_DETAIL_ERROR} (500)`,
    );
  });

  it("shows Error.message via panelErrMsg", async () => {
    getSearchDetail.mockRejectedValue(new Error("network down"));
    render(<SearchDetailPanel idOrName="All Content" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sr-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sr-detail-error").textContent).toBe(
      `${DEV_MSG.SR_DETAIL_ERROR} network down`,
    );
    expect(screen.queryByTestId("developer-sr-detail-title")).toBeNull();
  });

  it("shows fallback when rejection has no message", async () => {
    getSearchDetail.mockRejectedValue("boom");
    render(<SearchDetailPanel idOrName="All Content" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sr-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sr-detail-error").textContent).toBe(
      DEV_MSG.SR_DETAIL_ERROR,
    );
  });
});
