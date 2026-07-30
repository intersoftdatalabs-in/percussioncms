/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import React from "react";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  getApplicationDetail,
  listApplications,
} from "../../../main/ts/api/developer/pipelinesApi";
import { PipelinesPanel } from "../../../main/ts/developer/PipelinesPanel";

vi.mock("../../../main/ts/api/developer/pipelinesApi", () => ({
  listApplications: vi.fn(),
  getApplicationDetail: vi.fn(),
}));

const listApplicationsMock = vi.mocked(listApplications);
const getApplicationDetailMock = vi.mocked(getApplicationDetail);

describe("PipelinesPanel", () => {
  beforeEach(() => {
    listApplicationsMock.mockReset();
    getApplicationDetailMock.mockReset();
  });

  afterEach(() => {
    cleanup();
  });

  it("renders catalog rows when applications load", async () => {
    listApplicationsMock.mockResolvedValue([
      {
        id: 1,
        name: "sys_cmpDocuments",
        description: "System content editor app",
        enabled: true,
        appType: "CONTENT_EDITOR",
        appRoot: "sys_cmpDocuments",
      },
    ]);
    render(<PipelinesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-table")).toBeTruthy();
    });
    expect(screen.getAllByText("sys_cmpDocuments").length).toBeGreaterThan(0);
    expect(screen.getByText("CONTENT_EDITOR")).toBeTruthy();
  });

  it("opens read-only application detail from catalog row", async () => {
    listApplicationsMock.mockResolvedValue([
      {
        id: 1,
        name: "sys_cmpDocuments",
        description: "System content editor app",
        enabled: true,
        appType: "CONTENT_EDITOR",
        appRoot: "sys_cmpDocuments",
      },
    ]);
    getApplicationDetailMock.mockResolvedValue({
      id: 1,
      name: "sys_cmpDocuments",
      description: "System content editor app",
      enabled: true,
      hidden: false,
      appType: "CONTENT_EDITOR",
      appRoot: "sys_cmpDocuments",
      version: "8.2",
      dataSets: [
        {
          name: "contenteditor",
          kind: "DATASET",
          requestPage: "contenteditor.html",
          description: "CE",
        },
      ],
      designGaps: ["Pipe IR not exposed"],
    });

    render(<PipelinesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-table")).toBeTruthy();
    });

    fireEvent.click(screen.getByTestId("developer-pipe-open"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-detail")).toBeTruthy();
    });
    expect(getApplicationDetailMock).toHaveBeenCalledWith("sys_cmpDocuments");
    expect(screen.getByTestId("developer-pipe-detail-title").textContent).toBe(
      "sys_cmpDocuments",
    );
    expect(screen.getByTestId("developer-pipe-datasets-table")).toBeTruthy();
    expect(screen.getByText("contenteditor.html")).toBeTruthy();
    expect(screen.getByTestId("developer-pipe-gaps")).toBeTruthy();

    fireEvent.click(screen.getByTestId("developer-pipe-back"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-table")).toBeTruthy();
    });
  });

  it("shows empty state when API returns no applications", async () => {
    listApplicationsMock.mockResolvedValue([]);
    render(<PipelinesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-empty")).toBeTruthy();
    });
  });

  it("shows error UI when pipelines fetch fails", async () => {
    listApplicationsMock.mockRejectedValue({ status: 500, statusText: "Error" });
    render(<PipelinesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-pipe-error").textContent).toMatch(/500/);
  });
});
