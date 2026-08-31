/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import React from "react";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import {
  getSearchDetail,
  listSearches,
} from "../../../main/ts/api/developer/searchesApi";
import { DEV_MSG } from "../../../main/ts/developer/messages";
import { SearchesPanel } from "../../../main/ts/developer/SearchesPanel";

vi.mock("../../../main/ts/api/developer/searchesApi", async (importOriginal) => {
  const actual = await importOriginal<
    typeof import("../../../main/ts/api/developer/searchesApi")
  >();
  return {
    ...actual,
    listSearches: vi.fn(),
    getSearchDetail: vi.fn(),
    createSearch: vi.fn(),
    saveSearch: vi.fn(),
    deleteSearch: vi.fn(),
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

const listMock = vi.mocked(listSearches);
const detailMock = vi.mocked(getSearchDetail);

const sampleSearch = {
  name: "All Content",
  label: "All Content",
  standardSearch: true,
  fields: [{ fieldName: "sys_title", operator: "=", fieldValue: "*" }],
};

const sampleDetail = {
  name: "All Content",
  label: "All Content",
  fields: [{ fieldName: "sys_title", operator: "=", fieldValue: "*" }],
  designGaps: ["gap"],
};

describe("SearchesPanel", () => {
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

  it("lists searches and opens detail", async () => {
    listMock.mockResolvedValue([sampleSearch]);
    detailMock.mockResolvedValue(sampleDetail);
    render(<SearchesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sr-table")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sr-table").textContent).toContain("All Content");
    expect(screen.getByTestId("developer-sr-new")).toBeTruthy();
    fireEvent.click(screen.getByTestId("developer-sr-open"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-sr-detail")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sr-fields-table")).toBeTruthy();
    expect(screen.getByTestId("developer-sr-save")).toBeTruthy();
    expect(screen.getByTestId("developer-sr-delete")).toBeTruthy();
    fireEvent.click(screen.getByTestId("developer-sr-back"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-sr-table")).toBeTruthy();
    });
  });

  it("opens create chrome from New search", async () => {
    listMock.mockResolvedValue([]);
    render(<SearchesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sr-empty")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-sr-new"));
    expect(screen.getByTestId("developer-sr-detail")).toBeTruthy();
    expect(screen.getByTestId("developer-sr-save")).toBeDisabled();
    expect(detailMock).not.toHaveBeenCalled();
  });

  it("shows empty state when API returns no searches", async () => {
    listMock.mockResolvedValue([]);
    render(<SearchesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sr-empty")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sr-new")).toBeTruthy();
  });

  it("shows session-redirect message via panelErrMsg", async () => {
    listMock.mockRejectedValue(new SessionRedirectError());
    render(<SearchesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sr-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sr-error").textContent).toBe(DEV_MSG.SESSION_REDIRECT);
    expect(screen.queryByTestId("developer-sr-empty")).toBeNull();
  });

  it("shows ApiError status via panelErrMsg", async () => {
    listMock.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
      body: null,
    });
    render(<SearchesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sr-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sr-error").textContent).toBe(`${DEV_MSG.SR_ERROR} (500)`);
  });

  it("shows Error.message via panelErrMsg", async () => {
    listMock.mockRejectedValue(new Error("network down"));
    render(<SearchesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sr-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sr-error").textContent).toBe(
      `${DEV_MSG.SR_ERROR} network down`,
    );
    expect(screen.queryByTestId("developer-sr-table")).toBeNull();
  });

  it("shows fallback when rejection has no message", async () => {
    listMock.mockRejectedValue("boom");
    render(<SearchesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sr-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sr-error").textContent).toBe(DEV_MSG.SR_ERROR);
  });
});
