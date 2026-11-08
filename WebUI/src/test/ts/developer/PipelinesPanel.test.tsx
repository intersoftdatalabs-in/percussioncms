/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import React from "react";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { BootstrapProvider } from "../../../main/ts/app/bootstrap/BootstrapContext";
import { SessionRedirectError } from "../../../main/ts/api/client";
import {
  getApplicationDetail,
  getPipelineIr,
  listApplications,
} from "../../../main/ts/api/developer/pipelinesApi";
import * as pipeImportExport from "../../../main/ts/api/developer/pipelineImportExport";
import { DEV_MSG } from "../../../main/ts/developer/messages";
import {
  PipelinesPanel,
  retainCatalogOnListError,
} from "../../../main/ts/developer/PipelinesPanel";

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
  putWebhookHooks: vi.fn(),
  putFilterGroup: vi.fn(),
  putBinaryResource: vi.fn(),
  retrieveBinaryResource: vi.fn(),
  putResultPage: vi.fn(),
  putTracing: vi.fn(),
  getTracing: vi.fn(),
  getLastTrace: vi.fn(),
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

  it("retainCatalogOnListError keeps rows instead of wiping to empty (#4384)", () => {
    const prev = [{ id: 1, name: "sys_cmpDocuments" }];
    expect(retainCatalogOnListError(prev)).toEqual(prev);
    expect(retainCatalogOnListError(null)).toEqual([]);
  });

  it("shows fallback when rejection has no message", async () => {
    listApplicationsMock.mockRejectedValue("boom");
    render(<PipelinesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-pipe-error").textContent).toBe(DEV_MSG.PIPE_ERROR);
  });

  function renderAsAdmin() {
    return render(
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
        <PipelinesPanel />
      </BootstrapProvider>,
    );
  }

  it("Admin sees the create-only import section; non-Admin does not", async () => {
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
    renderAsAdmin();
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-table")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-pipe-import")).toBeTruthy();
    expect(screen.getByTestId("developer-pipe-import-submit")).toBeTruthy();
    // Honest empty state: no file chosen, no notice, no error.
    expect(screen.queryByTestId("developer-pipe-import-error")).toBeNull();
    expect(screen.queryByTestId("developer-pipe-import-notice")).toBeNull();
    cleanup();

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
    expect(screen.queryByTestId("developer-pipe-import")).toBeNull();
  });

  it("Admin import with no file shows the honest empty error", async () => {
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
    renderAsAdmin();
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-import-submit")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-pipe-import-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-import-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-pipe-import-error").textContent).toBe(
      DEV_MSG.PIPE_IMPORT_NO_FILE,
    );
  });

  it("Admin duplicate import surfaces 409 and keeps the catalog", async () => {
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
    const importMock = vi
      .spyOn(pipeImportExport, "importPipelineApplication")
      .mockRejectedValue({
        status: 409,
        statusText: "Conflict",
        body: { message: "Application already exists: sys_cmpDocuments" },
      });
    try {
      renderAsAdmin();
      await waitFor(() => {
        expect(screen.getByTestId("developer-pipe-import-file")).toBeTruthy();
      });
      const xml =
        '<PSXApplication id="9914" version="2.0"><name>sys_cmpDocuments</name>' +
        "<requestRoot>sys_cmpDocuments</requestRoot></PSXApplication>";
      const file = new File([xml], "sys_cmpDocuments.xml", { type: "application/xml" });
      fireEvent.change(screen.getByTestId("developer-pipe-import-file"), {
        target: { files: [file] },
      });
      await waitFor(() => {
        expect(screen.getByTestId("developer-pipe-import-submit")).toBeTruthy();
      });
      fireEvent.click(screen.getByTestId("developer-pipe-import-submit"));
      await waitFor(() => {
        expect(importMock).toHaveBeenCalled();
      });
      await waitFor(() => {
        expect(screen.getByTestId("developer-pipe-import-error")).toBeTruthy();
      });
      expect(screen.getByTestId("developer-pipe-import-error").textContent).toContain(
        DEV_MSG.PIPE_IMPORT_DUPLICATE,
      );
      // Catalog is retained (create-only 409 mutates nothing).
      expect(screen.getByTestId("developer-pipe-table")).toBeTruthy();
    } finally {
      importMock.mockRestore();
    }
  });
});
