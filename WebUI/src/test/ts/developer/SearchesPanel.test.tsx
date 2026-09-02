/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import React from "react";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import {
  createSearch,
  getSearchDetail,
  listSearches,
} from "../../../main/ts/api/developer/searchesApi";
import { DEV_MSG } from "../../../main/ts/developer/messages";
import { SearchesPanel, upsertSearchRow } from "../../../main/ts/developer/SearchesPanel";

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
const createMock = vi.mocked(createSearch);

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
    createMock.mockReset();
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

  it("upsertSearchRow adds a created name and keeps existing rows", () => {
    const merged = upsertSearchRow([{ name: "Default_Search", label: "Default" }], {
      name: "QaSearch",
      label: "QA",
    });
    expect(merged.map((s) => s.name)).toEqual(["Default_Search", "QaSearch"]);
    const updated = upsertSearchRow(merged, { name: "QaSearch", label: "QA 2" });
    expect(updated).toHaveLength(2);
    expect(updated[1].label).toBe("QA 2");
  });

  it("keeps a created search in the catalog when GET list omits the name", async () => {
    listMock.mockResolvedValue([{ name: "Default_Search", label: "Default", standardSearch: true }]);
    createMock.mockResolvedValue({
      name: "QaSearch",
      label: "QA",
      type: "StandardSearch",
      standardSearch: true,
      fields: [],
    });
    render(<SearchesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sr-new")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-sr-new"));
    fireEvent.change(screen.getByTestId("developer-sr-name"), {
      target: { value: "QaSearch" },
    });
    fireEvent.click(screen.getByTestId("developer-sr-save"));
    await waitFor(() => {
      expect(createMock).toHaveBeenCalled();
    });
    fireEvent.click(screen.getByTestId("developer-sr-back"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-sr-panel")).toBeTruthy();
    });
    expect(document.querySelector('[data-sr-name="QaSearch"]')).toBeTruthy();
    expect(document.querySelector('[data-sr-name="Default_Search"]')).toBeTruthy();
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
