/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import * as pipelinesApi from "../../../main/ts/api/developer/pipelinesApi";
import { DEV_MSG } from "../../../main/ts/developer/messages";
import {
  PipelineDetailPanel,
  presentStageLabels,
} from "../../../main/ts/developer/PipelineDetailPanel";

vi.mock("../../../main/ts/api/developer/pipelinesApi", () => ({
  getApplicationDetail: vi.fn(),
  getPipelineIr: vi.fn(),
  listApplications: vi.fn(),
}));

const getApplicationDetail = pipelinesApi.getApplicationDetail as ReturnType<typeof vi.fn>;
const getPipelineIr = pipelinesApi.getPipelineIr as ReturnType<typeof vi.fn>;

const sampleDetail = {
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
};

const sampleIr = {
  irVersion: "1.0",
  source: "CLASSIC_IMPORT",
  app: {
    id: 1,
    name: "sys_cmpDocuments",
    requestRoot: "sys_cmpDocuments",
    enabled: true,
  },
  resources: [
    {
      name: "Dataset34",
      kind: "QUERY",
      requestPage: "sys_rxlookup",
      pipeName: "QueryPipe",
      transactionMode: "none",
      stages: {
        pageTank: { present: true, schemaSource: "file:Properties.dtd" },
        backendTank: {
          present: true,
          joinCount: 0,
          tables: [
            { alias: "PSX_ADMINLOOKUP", table: "PSX_ADMINLOOKUP", datasource: "" },
          ],
        },
        mapper: {
          present: true,
          mappings: [
            {
              documentField: "Properties/@Type",
              backend: "PSX_ADMINLOOKUP.TYPE",
              backendKind: "COLUMN",
            },
          ],
        },
        selector: { present: true, method: "whereClause", whereClauseCount: 1 },
        pager: { present: false },
        updater: { present: false },
      },
    },
  ],
};

describe("presentStageLabels", () => {
  it("lists present stage keys in order", () => {
    expect(
      presentStageLabels({
        pageTank: { present: true },
        backendTank: { present: true },
        mapper: { present: false },
        selector: { present: true },
        pager: { present: false },
        updater: { present: false },
      }),
    ).toEqual(["pageTank", "backendTank", "selector"]);
  });

  it("returns empty for missing stages", () => {
    expect(presentStageLabels(undefined)).toEqual([]);
  });
});

describe("PipelineDetailPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    getApplicationDetail.mockReset();
    getPipelineIr.mockReset();
    getPipelineIr.mockResolvedValue(sampleIr);
  });

  it("loads detail and IR on success and supports back", async () => {
    getApplicationDetail.mockResolvedValue(sampleDetail);
    const onBack = vi.fn();
    render(<PipelineDetailPanel idOrName="sys_cmpDocuments" onBack={onBack} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-detail-title")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-pipe-detail-title").textContent).toBe(
      "sys_cmpDocuments",
    );
    expect(screen.getByTestId("developer-pipe-datasets-table")).toBeTruthy();
    expect(screen.getByText("contenteditor.html")).toBeTruthy();
    expect(screen.getByTestId("developer-pipe-gaps").textContent).toContain(
      "IR write / graph editor not exposed",
    );
    expect(getApplicationDetail).toHaveBeenCalledWith("sys_cmpDocuments");
    expect(getPipelineIr).toHaveBeenCalledWith("sys_cmpDocuments");

    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-ir-meta")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-pipe-ir-meta").textContent).toContain(
      "CLASSIC_IMPORT",
    );
    expect(screen.getByTestId("developer-pipe-ir-resource-0")).toBeTruthy();
    expect(screen.getByTestId("developer-pipe-ir-tanks-0").textContent).toContain(
      "PSX_ADMINLOOKUP",
    );
    expect(screen.getByTestId("developer-pipe-ir-mapper-0").textContent).toContain(
      "Properties/@Type",
    );
    fireEvent.click(screen.getByTestId("developer-pipe-back"));
    expect(onBack).toHaveBeenCalled();
  });

  it("shows empty datasets section when detail has none", async () => {
    getApplicationDetail.mockResolvedValue({
      ...sampleDetail,
      dataSets: [],
      designGaps: undefined,
    });
    render(<PipelineDetailPanel idOrName="sys_cmpDocuments" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-datasets-empty")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-pipe-datasets-empty").textContent).toBe(
      DEV_MSG.PIPE_NONE,
    );
    expect(screen.queryByTestId("developer-pipe-datasets-table")).toBeNull();
    expect(screen.queryByTestId("developer-pipe-gaps")).toBeNull();
  });

  it("shows IR empty state when resources array is empty", async () => {
    getApplicationDetail.mockResolvedValue({ ...sampleDetail, designGaps: undefined });
    getPipelineIr.mockResolvedValue({
      irVersion: "1.0",
      source: "NATIVE",
      resources: [],
    });
    render(<PipelineDetailPanel idOrName="sys_cmpDocuments" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-ir-empty")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-pipe-ir-empty").textContent).toBe(
      DEV_MSG.PIPE_IR_EMPTY,
    );
  });

  it("keeps catalog detail when IR load fails", async () => {
    getApplicationDetail.mockResolvedValue(sampleDetail);
    getPipelineIr.mockRejectedValue({
      status: 404,
      statusText: "Not Found",
      body: null,
    });
    render(<PipelineDetailPanel idOrName="sys_cmpDocuments" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-detail-title")).toBeTruthy();
    });
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-ir-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-pipe-ir-error").textContent).toBe(
      `${DEV_MSG.PIPE_IR_ERROR} (404)`,
    );
    expect(screen.getByTestId("developer-pipe-datasets-table")).toBeTruthy();
    expect(screen.queryByTestId("developer-pipe-ir-resources")).toBeNull();
  });

  it("shows session-redirect message via panelErrMsg", async () => {
    getApplicationDetail.mockRejectedValue(new SessionRedirectError());
    render(<PipelineDetailPanel idOrName="sys_cmpDocuments" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-pipe-detail-error").textContent).toBe(
      DEV_MSG.SESSION_REDIRECT,
    );
    expect(screen.queryByTestId("developer-pipe-detail-loading")).toBeNull();
    expect(screen.queryByTestId("developer-pipe-detail-title")).toBeNull();
  });

  it("shows ApiError status via panelErrMsg", async () => {
    getApplicationDetail.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
      body: null,
    });
    render(<PipelineDetailPanel idOrName="sys_cmpDocuments" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-pipe-detail-error").textContent).toBe(
      `${DEV_MSG.PIPE_DETAIL_ERROR} (500)`,
    );
  });

  it("shows Error.message via panelErrMsg", async () => {
    getApplicationDetail.mockRejectedValue(new Error("network down"));
    render(<PipelineDetailPanel idOrName="sys_cmpDocuments" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-pipe-detail-error").textContent).toBe(
      `${DEV_MSG.PIPE_DETAIL_ERROR} network down`,
    );
    expect(screen.queryByTestId("developer-pipe-detail-title")).toBeNull();
  });

  it("shows fallback when rejection has no message", async () => {
    getApplicationDetail.mockRejectedValue("boom");
    render(<PipelineDetailPanel idOrName="sys_cmpDocuments" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-pipe-detail-error").textContent).toBe(
      DEV_MSG.PIPE_DETAIL_ERROR,
    );
  });
});
