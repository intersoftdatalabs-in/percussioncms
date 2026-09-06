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
  executeResource: vi.fn(),
  getApplicationValidation: vi.fn(),
  getPipelineIr: vi.fn(),
  getPipelineOpenApi: vi.fn(),
  openApiDownloadFilename: (app: string, format = "yaml") =>
    `${app}.openapi.${format === "json" ? "json" : "yaml"}`,
  putHttpBackendTank: vi.fn(),
}));

const getApplicationDetail = pipelinesApi.getApplicationDetail as ReturnType<typeof vi.fn>;
const startApplication = pipelinesApi.startApplication as ReturnType<typeof vi.fn>;
const stopApplication = pipelinesApi.stopApplication as ReturnType<typeof vi.fn>;
const executeResource = pipelinesApi.executeResource as ReturnType<typeof vi.fn>;
const getApplicationValidation = pipelinesApi.getApplicationValidation as ReturnType<
  typeof vi.fn
>;
const getPipelineIr = pipelinesApi.getPipelineIr as ReturnType<typeof vi.fn>;
const getPipelineOpenApi = pipelinesApi.getPipelineOpenApi as ReturnType<typeof vi.fn>;
const putHttpBackendTank = pipelinesApi.putHttpBackendTank as ReturnType<typeof vi.fn>;

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
    executeResource.mockReset();
    getApplicationValidation.mockReset();
    getPipelineIr.mockReset();
    getPipelineOpenApi.mockReset();
    putHttpBackendTank.mockReset();
    getPipelineIr.mockResolvedValue({ irVersion: "1.0", source: "NATIVE", resources: [] });
    getPipelineOpenApi.mockResolvedValue(
      'openapi: "3.0.3"\npaths:\n  /pipelines/sys_cmpDocuments/resources/contenteditor/execute:\n',
    );
    putHttpBackendTank.mockResolvedValue({
      adapterType: "HTTP",
      url: "http://127.0.0.1/pipeline-http-fixture",
      httpMethod: "GET",
    });
    // Soft-empty when validation tip is not merged (default for most tests).
    getApplicationValidation.mockRejectedValue({
      status: 404,
      statusText: "Not Found",
      body: null,
    });
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

  it("hides Start/Stop, Test invoke, and Problems chrome for non-Admin", async () => {
    getApplicationDetail.mockResolvedValue(sampleDetail);
    renderDetail(false);
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-detail-title")).toBeTruthy();
    });
    expect(screen.queryByTestId("developer-pipe-lifecycle")).toBeNull();
    expect(screen.queryByTestId("developer-pipe-start")).toBeNull();
    expect(screen.queryByTestId("developer-pipe-stop")).toBeNull();
    expect(screen.queryByTestId("developer-pipe-invoke")).toBeNull();
    expect(screen.queryByTestId("developer-pipe-http")).toBeNull();
    expect(screen.queryByTestId("developer-pipe-problems")).toBeNull();
    expect(getApplicationValidation).not.toHaveBeenCalled();
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

  it("Admin Test invoke POSTs sample JSON and shows execute result", async () => {
    getApplicationDetail.mockResolvedValue(sampleDetail);
    executeResource.mockResolvedValue({
      appName: "sys_cmpDocuments",
      resourceName: "contenteditor",
      kind: "query",
      rowCount: 1,
      rows: [{ TYPE: "workflow" }],
    });
    renderDetail(true);
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-invoke")).toBeTruthy();
    });
    const resourceInput = screen.getByTestId(
      "developer-pipe-invoke-resource",
    ) as HTMLInputElement;
    expect(resourceInput.value).toBe("contenteditor");
    fireEvent.change(screen.getByTestId("developer-pipe-invoke-body"), {
      target: { value: '{"params":{"TYPE":"workflow"}}' },
    });
    fireEvent.click(screen.getByTestId("developer-pipe-invoke-run"));
    await waitFor(() => {
      expect(executeResource).toHaveBeenCalledWith(
        "sys_cmpDocuments",
        "contenteditor",
        { params: { TYPE: "workflow" } },
      );
    });
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-invoke-result").textContent).toContain(
        "contenteditor",
      );
    });
    expect(screen.getByTestId("developer-pipe-invoke-result").textContent).toContain(
      "workflow",
    );
  });

  it("shows clear error for invalid invoke JSON and missing resource", async () => {
    getApplicationDetail.mockResolvedValue({ ...sampleDetail, dataSets: [] });
    renderDetail(true);
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-invoke")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-pipe-invoke-run"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-invoke-error").textContent).toBe(
        DEV_MSG.PIPE_INVOKE_RESOURCE_REQUIRED,
      );
    });
    expect(executeResource).not.toHaveBeenCalled();

    fireEvent.change(screen.getByTestId("developer-pipe-invoke-resource"), {
      target: { value: "DatasetQ" },
    });
    fireEvent.change(screen.getByTestId("developer-pipe-invoke-body"), {
      target: { value: "not-json" },
    });
    fireEvent.click(screen.getByTestId("developer-pipe-invoke-run"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-invoke-error").textContent).toBe(
        DEV_MSG.PIPE_INVOKE_BODY_INVALID,
      );
    });
    expect(executeResource).not.toHaveBeenCalled();
  });

  it("Admin saves HTTP backend tank for the selected resource", async () => {
    getApplicationDetail.mockResolvedValue(sampleDetail);
    renderDetail(true);
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-http")).toBeTruthy();
    });
    const url = screen.getByTestId("developer-pipe-http-url") as HTMLInputElement;
    expect(url.value).toContain("pipeline-http-fixture");
    fireEvent.click(screen.getByTestId("developer-pipe-http-save"));
    await waitFor(() => {
      expect(putHttpBackendTank).toHaveBeenCalledWith(
        "sys_cmpDocuments",
        "contenteditor",
        {
          adapterType: "HTTP",
          url: "http://127.0.0.1/pipeline-http-fixture",
          httpMethod: "GET",
        },
      );
    });
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-http-notice").textContent).toBe(
        DEV_MSG.PIPE_HTTP_SAVED,
      );
    });
  });

  it("HTTP save fail-closes on blank URL and cloud 400", async () => {
    getApplicationDetail.mockResolvedValue(sampleDetail);
    renderDetail(true);
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-http-url")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-pipe-http-url"), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("developer-pipe-http-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-http-error").textContent).toBe(
        DEV_MSG.PIPE_HTTP_URL_REQUIRED,
      );
    });
    expect(putHttpBackendTank).not.toHaveBeenCalled();

    fireEvent.change(screen.getByTestId("developer-pipe-http-url"), {
      target: { value: "https://erp.example/api/items" },
    });
    putHttpBackendTank.mockRejectedValue({
      status: 400,
      statusText: "Bad Request",
      body: { message: "HTTP datasource URL must be loopback" },
    });
    fireEvent.click(screen.getByTestId("developer-pipe-http-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-http-error").textContent).toMatch(
        /loopback/i,
      );
    });
  });

  it("surfaces execute API errors in Test invoke", async () => {
    getApplicationDetail.mockResolvedValue(sampleDetail);
    executeResource.mockRejectedValue({
      status: 400,
      statusText: "Bad Request",
      body: { message: "unsupported resource kind" },
    });
    renderDetail(true);
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-invoke-run")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-pipe-invoke-run"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-invoke-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-pipe-invoke-error").textContent).toMatch(
      /unsupported resource kind|400/,
    );
  });

  it("soft-empty Problems when validation REST returns 404", async () => {
    getApplicationDetail.mockResolvedValue(sampleDetail);
    renderDetail(true);
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-problems-unavailable")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-pipe-problems-unavailable").textContent).toBe(
      DEV_MSG.PIPE_PROBLEMS_UNAVAILABLE,
    );
    expect(getApplicationValidation).toHaveBeenCalledWith("sys_cmpDocuments");
    expect(screen.queryByTestId("developer-pipe-problems-table")).toBeNull();
  });

  it("renders Problems table when validation REST is present", async () => {
    getApplicationDetail.mockResolvedValue(sampleDetail);
    getApplicationValidation.mockResolvedValue({
      id: 1,
      name: "sys_cmpDocuments",
      valid: false,
      errorCount: 1,
      warningCount: 1,
      problems: [
        {
          severity: "ERROR",
          code: "1301",
          message: "Missing mapper",
          resource: "contenteditor",
          path: "PSDataMapper",
        },
        {
          severity: "WARNING",
          code: "1400",
          message: "Deprecated exit",
        },
      ],
    });
    renderDetail(true);
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-problems-table")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-pipe-problem-row-0").textContent).toContain(
      "Missing mapper",
    );
    expect(screen.getByTestId("developer-pipe-problem-row-1").textContent).toContain(
      "WARNING",
    );
  });

  it("shows empty Problems when validation returns no items", async () => {
    getApplicationDetail.mockResolvedValue(sampleDetail);
    getApplicationValidation.mockResolvedValue({
      name: "sys_cmpDocuments",
      valid: true,
      errorCount: 0,
      warningCount: 0,
      problems: [],
    });
    renderDetail(true);
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-problems-empty")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-pipe-problems-empty").textContent).toBe(
      DEV_MSG.PIPE_PROBLEMS_EMPTY,
    );
  });

  it("views OpenAPI YAML documenting a resource path", async () => {
    getApplicationDetail.mockResolvedValue(sampleDetail);
    renderDetail(true);
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-openapi-doc")).toBeTruthy();
    });
    expect(getPipelineOpenApi).toHaveBeenCalledWith("sys_cmpDocuments", "yaml");
    expect(screen.getByTestId("developer-pipe-openapi-doc").textContent).toContain(
      "openapi:",
    );
    expect(screen.getByTestId("developer-pipe-openapi-doc").textContent).toContain(
      "/pipelines/sys_cmpDocuments/resources/contenteditor/execute",
    );
    fireEvent.click(screen.getByTestId("developer-pipe-openapi-view"));
    await waitFor(() => {
      expect(getPipelineOpenApi.mock.calls.length).toBeGreaterThanOrEqual(2);
    });
  });

  it("downloads OpenAPI with a safe filename", async () => {
    getApplicationDetail.mockResolvedValue(sampleDetail);
    renderDetail(true);
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-openapi-download")).toBeTruthy();
    });
    const createObjectURL = vi.fn(() => "blob:openapi");
    const revoke = vi.fn();
    URL.createObjectURL = createObjectURL;
    URL.revokeObjectURL = revoke;
    const click = vi.fn();
    const realCreate = document.createElement.bind(document);
    const createSpy = vi.spyOn(document, "createElement").mockImplementation((tag: string) => {
      const el = realCreate(tag);
      if (tag === "a") {
        el.click = click;
      }
      return el;
    });
    fireEvent.click(screen.getByTestId("developer-pipe-openapi-download"));
    expect(createObjectURL).toHaveBeenCalled();
    expect(click).toHaveBeenCalled();
    expect(revoke).toHaveBeenCalled();
    createSpy.mockRestore();
  });

  it("shows OpenAPI error without echoing the path id", async () => {
    getApplicationDetail.mockResolvedValue(sampleDetail);
    getPipelineOpenApi.mockRejectedValue({
      status: 404,
      statusText: "Not Found",
      body: { message: "Application not found" },
    });
    renderDetail(true);
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-openapi-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-pipe-openapi-error").textContent).toContain(
      "Application not found",
    );
    expect(screen.getByTestId("developer-pipe-openapi-error").textContent).not.toContain(
      "../",
    );
  });
});
