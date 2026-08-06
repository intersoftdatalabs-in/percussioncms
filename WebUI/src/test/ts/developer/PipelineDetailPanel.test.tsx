/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import * as pipelinesApi from "../../../main/ts/api/developer/pipelinesApi";
import { DEV_MSG } from "../../../main/ts/developer/messages";
import { PipelineDetailPanel } from "../../../main/ts/developer/PipelineDetailPanel";

vi.mock("../../../main/ts/api/developer/pipelinesApi", () => ({
  getApplicationDetail: vi.fn(),
  listApplications: vi.fn(),
}));

const getApplicationDetail = pipelinesApi.getApplicationDetail as ReturnType<typeof vi.fn>;

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
  designGaps: ["Pipe IR not exposed"],
};

describe("PipelineDetailPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    getApplicationDetail.mockReset();
  });

  it("loads detail on success and supports back", async () => {
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
      "Pipe IR not exposed",
    );
    expect(getApplicationDetail).toHaveBeenCalledWith("sys_cmpDocuments");
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
