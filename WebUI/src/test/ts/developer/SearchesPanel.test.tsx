/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import * as searchesApi from "../../../main/ts/api/developer/searchesApi";
import { DEV_MSG } from "../../../main/ts/developer/messages";
import { SearchesPanel } from "../../../main/ts/developer/SearchesPanel";

vi.mock("../../../main/ts/api/developer/searchesApi", () => ({
  listSearches: vi.fn(),
  getSearchDetail: vi.fn(),
}));

const listSearches = searchesApi.listSearches as ReturnType<typeof vi.fn>;
const getSearchDetail = searchesApi.getSearchDetail as ReturnType<typeof vi.fn>;

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
    listSearches.mockReset();
    getSearchDetail.mockReset();
  });

  it("lists searches and opens detail", async () => {
    listSearches.mockResolvedValue([sampleSearch]);
    getSearchDetail.mockResolvedValue(sampleDetail);
    render(<SearchesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sr-table")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sr-table").textContent).toContain("All Content");
    fireEvent.click(screen.getByTestId("developer-sr-open"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-sr-detail")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sr-fields-table")).toBeTruthy();
    fireEvent.click(screen.getByTestId("developer-sr-back"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-sr-table")).toBeTruthy();
    });
  });

  it("shows empty state when API returns no searches", async () => {
    listSearches.mockResolvedValue([]);
    render(<SearchesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sr-empty")).toBeTruthy();
    });
  });

  it("shows session-redirect message via panelErrMsg", async () => {
    listSearches.mockRejectedValue(new SessionRedirectError());
    render(<SearchesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sr-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sr-error").textContent).toBe(DEV_MSG.SESSION_REDIRECT);
    expect(screen.queryByTestId("developer-sr-empty")).toBeNull();
  });

  it("shows ApiError status via panelErrMsg", async () => {
    listSearches.mockRejectedValue({
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
    listSearches.mockRejectedValue(new Error("network down"));
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
    listSearches.mockRejectedValue("boom");
    render(<SearchesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sr-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sr-error").textContent).toBe(DEV_MSG.SR_ERROR);
  });
});
