/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import React from "react";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import {
  getApplicationDetail,
  getPipelineIr,
  listApplications,
} from "../../../main/ts/api/developer/pipelinesApi";
import { DEV_MSG } from "../../../main/ts/developer/messages";
import { PipelinesPanel } from "../../../main/ts/developer/PipelinesPanel";

vi.mock("../../../main/ts/api/developer/pipelinesApi", () => ({
  listApplications: vi.fn(),
  getApplicationDetail: vi.fn(),
  startApplication: vi.fn(),
  stopApplication: vi.fn(),
  getPipelineIr: vi.fn(),
  getPipelineOpenApi: vi.fn().mockResolvedValue('openapi: "3.0.3"\npaths: {}\n'),
  openApiDownloadFilename: (app: string) => `${app}.openapi.yaml`,
  executeResource: vi.fn(),
  putHttpBackendTank: vi.fn(),
  getApplicationValidation: vi.fn().mockRejectedValue({
    status: 404,
    statusText: "Not Found",
    body: null,
  }),
}));

const listApplicationsMock = vi.mocked(listApplications);
const getApplicationDetailMock = vi.mocked(getApplicationDetail);
const getPipelineIrMock = vi.mocked(getPipelineIr);

describe("PipelinesPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    listApplicationsMock.mockReset();
    getApplicationDetailMock.mockReset();
    getPipelineIrMock.mockReset();
    getPipelineIrMock.mockResolvedValue({
      irVersion: "1.0",
      source: "NATIVE",
      resources: [],
    });
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
    expect(screen.getByTestId("developer-pipe-open").getAttribute("data-pipe-name")).toBe(
      "sys_cmpDocuments",
    );
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
      designGaps: ["IR write / graph editor not exposed"],
    });

    render(<PipelinesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-table")).toBeTruthy();
    });

    const openBtn = screen.getByTestId("developer-pipe-open");
    expect(openBtn.getAttribute("data-pipe-name")).toBe("sys_cmpDocuments");
    fireEvent.click(openBtn);
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-detail")).toBeTruthy();
    });
    expect(getApplicationDetailMock).toHaveBeenCalledWith("sys_cmpDocuments");
    expect(getPipelineIrMock).toHaveBeenCalledWith("sys_cmpDocuments");
    expect(screen.getByTestId("developer-pipe-detail-title").textContent).toBe(
      "sys_cmpDocuments",
    );
    expect(screen.getByTestId("developer-pipe-datasets-table")).toBeTruthy();
    expect(screen.getByText("contenteditor.html")).toBeTruthy();
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-ir")).toBeTruthy();
    });
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

  it("shows session-redirect message via panelErrMsg", async () => {
    listApplicationsMock.mockRejectedValue(new SessionRedirectError());
    render(<PipelinesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-pipe-error").textContent).toBe(DEV_MSG.SESSION_REDIRECT);
    expect(screen.queryByTestId("developer-pipe-empty")).toBeNull();
  });

  it("shows ApiError status via panelErrMsg", async () => {
    listApplicationsMock.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
      body: null,
    });
    render(<PipelinesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-pipe-error").textContent).toBe(
      `${DEV_MSG.PIPE_ERROR} (500)`,
    );
  });

  it("shows Error.message via panelErrMsg", async () => {
    listApplicationsMock.mockRejectedValue(new Error("network down"));
    render(<PipelinesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-pipe-error").textContent).toBe(
      `${DEV_MSG.PIPE_ERROR} network down`,
    );
    expect(screen.queryByTestId("developer-pipe-table")).toBeNull();
  });

  it("shows fallback when rejection has no message", async () => {
    listApplicationsMock.mockRejectedValue("boom");
    render(<PipelinesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-pipe-error").textContent).toBe(DEV_MSG.PIPE_ERROR);
  });
});
