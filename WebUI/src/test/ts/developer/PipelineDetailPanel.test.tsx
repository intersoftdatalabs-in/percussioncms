/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { BootstrapProvider } from "../../../main/ts/app/bootstrap/BootstrapContext";
import { SessionRedirectError } from "../../../main/ts/api/client";
import * as pipelinesApi from "../../../main/ts/api/developer/pipelinesApi";
import { DEV_MSG } from "../../../main/ts/developer/messages";
import { PipelineDetailPanel } from "../../../main/ts/developer/PipelineDetailPanel";

vi.mock("../../../main/ts/api/developer/pipelinesApi", () => ({
  getApplicationDetail: vi.fn(),
  listApplications: vi.fn(),
  startApplication: vi.fn(),
  stopApplication: vi.fn(),
  getPipelineIr: vi.fn(),
}));

const getApplicationDetail = pipelinesApi.getApplicationDetail as ReturnType<typeof vi.fn>;
const startApplication = pipelinesApi.startApplication as ReturnType<typeof vi.fn>;
const stopApplication = pipelinesApi.stopApplication as ReturnType<typeof vi.fn>;
const getPipelineIr = pipelinesApi.getPipelineIr as ReturnType<typeof vi.fn>;

const sampleDetail = {
  id: 1,
  name: "sys_cmpDocuments",
  description: "System content editor app",
  enabled: true,
  active: false,
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

function renderDetail(isAdmin = true) {
  return render(
    <BootstrapProvider
      value={{
        userName: isAdmin ? "admin" : "editor",
        locale: "en-us",
        entry: "developer",
        isAdmin,
        isDesigner: true,
        isWidgetBuilderActive: false,
        allowExternalAvatarFetch: true,
      }}
    >
      <PipelineDetailPanel idOrName="sys_cmpDocuments" onBack={() => undefined} />
    </BootstrapProvider>,
  );
}

describe("PipelineDetailPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    getApplicationDetail.mockReset();
    startApplication.mockReset();
    stopApplication.mockReset();
    getPipelineIr.mockReset();
    getPipelineIr.mockResolvedValue({ irVersion: "1.0", source: "NATIVE", resources: [] });
  });

  afterEach(() => {
    cleanup();
  });

  it("loads detail on success and supports back", async () => {
    getApplicationDetail.mockResolvedValue(sampleDetail);
    const onBack = vi.fn();
    render(
      <BootstrapProvider
        value={{
          userName: "admin",
          locale: "en-us",
          entry: "developer",
          isAdmin: true,
          isDesigner: true,
          isWidgetBuilderActive: false,
          allowExternalAvatarFetch: true,
        }}
      >
        <PipelineDetailPanel idOrName="sys_cmpDocuments" onBack={onBack} />
      </BootstrapProvider>,
    );
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
    expect(screen.getByTestId("developer-pipe-meta-running").textContent).toBe(
      DEV_MSG.NO,
    );
    expect(getApplicationDetail).toHaveBeenCalledWith("sys_cmpDocuments");
    fireEvent.click(screen.getByTestId("developer-pipe-back"));
    expect(onBack).toHaveBeenCalled();
  });

  it("shows Admin Start/Stop chrome and starts when stopped", async () => {
    getApplicationDetail.mockResolvedValue(sampleDetail);
    startApplication.mockResolvedValue({
      ...sampleDetail,
      active: true,
      designGaps: ["Pipe IR not exposed"],
    });
    renderDetail(true);
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-lifecycle")).toBeTruthy();
    });
    const startBtn = screen.getByTestId("developer-pipe-start") as HTMLButtonElement;
    const stopBtn = screen.getByTestId("developer-pipe-stop") as HTMLButtonElement;
    expect(startBtn.disabled).toBe(false);
    expect(stopBtn.disabled).toBe(true);
    fireEvent.click(startBtn);
    await waitFor(() => {
      expect(startApplication).toHaveBeenCalledWith("sys_cmpDocuments");
    });
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-meta-running").textContent).toBe(
        DEV_MSG.YES,
      );
    });
    expect(screen.getByTestId("developer-pipe-lifecycle-notice").textContent).toBe(
      DEV_MSG.PIPE_STARTED,
    );
    expect((screen.getByTestId("developer-pipe-start") as HTMLButtonElement).disabled).toBe(
      true,
    );
    expect((screen.getByTestId("developer-pipe-stop") as HTMLButtonElement).disabled).toBe(
      false,
    );
  });

  it("stops a running application and refreshes active", async () => {
    getApplicationDetail.mockResolvedValue({ ...sampleDetail, active: true });
    stopApplication.mockResolvedValue({ ...sampleDetail, active: false });
    renderDetail(true);
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-stop")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-pipe-stop"));
    await waitFor(() => {
      expect(stopApplication).toHaveBeenCalledWith("sys_cmpDocuments");
    });
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-meta-running").textContent).toBe(
        DEV_MSG.NO,
      );
    });
    expect(screen.getByTestId("developer-pipe-lifecycle-notice").textContent).toBe(
      DEV_MSG.PIPE_STOPPED,
    );
  });

  it("hides Start/Stop chrome for non-Admin", async () => {
    getApplicationDetail.mockResolvedValue(sampleDetail);
    renderDetail(false);
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-detail-title")).toBeTruthy();
    });
    expect(screen.queryByTestId("developer-pipe-lifecycle")).toBeNull();
    expect(screen.queryByTestId("developer-pipe-start")).toBeNull();
    expect(screen.queryByTestId("developer-pipe-stop")).toBeNull();
  });

  it("disables Start when application is disabled", async () => {
    getApplicationDetail.mockResolvedValue({
      ...sampleDetail,
      enabled: false,
      active: false,
    });
    renderDetail(true);
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-start")).toBeTruthy();
    });
    expect((screen.getByTestId("developer-pipe-start") as HTMLButtonElement).disabled).toBe(
      true,
    );
    expect((screen.getByTestId("developer-pipe-stop") as HTMLButtonElement).disabled).toBe(
      true,
    );
  });

  it("shows PIPE_FORBIDDEN on 403 start", async () => {
    getApplicationDetail.mockResolvedValue(sampleDetail);
    startApplication.mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: null,
    });
    renderDetail(true);
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-start")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-pipe-start"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-lifecycle-error").textContent).toBe(
        DEV_MSG.PIPE_FORBIDDEN,
      );
    });
  });

  it("clears busy when idOrName changes during an in-flight start", async () => {
    let resolveStart: ((value: typeof sampleDetail) => void) | undefined;
    getApplicationDetail.mockImplementation(async (id: string) => ({
      ...sampleDetail,
      name: id,
      active: false,
    }));
    startApplication.mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveStart = resolve;
        }),
    );

    const { rerender } = render(
      <BootstrapProvider
        value={{
          userName: "admin",
          locale: "en-us",
          entry: "developer",
          isAdmin: true,
          isDesigner: true,
          isWidgetBuilderActive: false,
          allowExternalAvatarFetch: true,
        }}
      >
        <PipelineDetailPanel idOrName="sys_cmpDocuments" onBack={() => undefined} />
      </BootstrapProvider>,
    );

    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-start")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-pipe-start"));
    await waitFor(() => {
      expect(startApplication).toHaveBeenCalledWith("sys_cmpDocuments");
    });
    expect((screen.getByTestId("developer-pipe-start") as HTMLButtonElement).disabled).toBe(
      true,
    );

    rerender(
      <BootstrapProvider
        value={{
          userName: "admin",
          locale: "en-us",
          entry: "developer",
          isAdmin: true,
          isDesigner: true,
          isWidgetBuilderActive: false,
          allowExternalAvatarFetch: true,
        }}
      >
        <PipelineDetailPanel idOrName="sys_otherApp" onBack={() => undefined} />
      </BootstrapProvider>,
    );

    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-detail-title").textContent).toBe("sys_otherApp");
    });
    expect((screen.getByTestId("developer-pipe-start") as HTMLButtonElement).disabled).toBe(
      false,
    );

    resolveStart?.({ ...sampleDetail, name: "sys_cmpDocuments", active: true });
  });

  it("shows empty datasets section when detail has none", async () => {
    getApplicationDetail.mockResolvedValue({
      ...sampleDetail,
      dataSets: [],
      designGaps: undefined,
    });
    renderDetail(true);
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
    renderDetail(true);
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
    renderDetail(true);
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-pipe-detail-error").textContent).toBe(
      `${DEV_MSG.PIPE_DETAIL_ERROR} (500)`,
    );
  });

  it("shows Error.message via panelErrMsg", async () => {
    getApplicationDetail.mockRejectedValue(new Error("network down"));
    renderDetail(true);
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
    renderDetail(true);
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-pipe-detail-error").textContent).toBe(
      DEV_MSG.PIPE_DETAIL_ERROR,
    );
  });
});
