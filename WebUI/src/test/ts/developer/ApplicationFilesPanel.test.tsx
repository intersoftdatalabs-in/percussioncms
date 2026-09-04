/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import * as appFilesApi from "../../../main/ts/api/developer/applicationFilesApi";
import * as pipelinesApi from "../../../main/ts/api/developer/pipelinesApi";
import { DEV_MSG } from "../../../main/ts/developer/messages";
import { ApplicationFilesPanel } from "../../../main/ts/developer/ApplicationFilesPanel";

vi.mock("../../../main/ts/api/developer/pipelinesApi", () => ({
  listApplications: vi.fn(),
}));

vi.mock("../../../main/ts/api/developer/applicationFilesApi", () => ({
  listApplicationFiles: vi.fn(),
  getApplicationFileDetail: vi.fn(),
  updateApplicationFile: vi.fn(),
  APPLICATION_FILE_DESIGN_GAPS: ["gap-lock"],
}));

const listApplications = pipelinesApi.listApplications as ReturnType<typeof vi.fn>;
const listApplicationFiles = appFilesApi.listApplicationFiles as ReturnType<typeof vi.fn>;
const getApplicationFileDetail = appFilesApi.getApplicationFileDetail as ReturnType<
  typeof vi.fn
>;
const updateApplicationFile = appFilesApi.updateApplicationFile as ReturnType<typeof vi.fn>;

describe("ApplicationFilesPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    listApplications.mockReset();
    listApplicationFiles.mockReset();
    getApplicationFileDetail.mockReset();
    updateApplicationFile.mockReset();
  });

  it("lists apps, files, opens editor, and saves content", async () => {
    listApplications.mockResolvedValue([
      { name: "sys_resources", description: "Resources", appRoot: "sys_resources" },
    ]);
    listApplicationFiles.mockResolvedValue([
      {
        path: "ApplicationFiles/style.css",
        name: "style.css",
        directory: false,
      },
      {
        path: "ApplicationFiles",
        name: "ApplicationFiles",
        directory: true,
      },
    ]);
    getApplicationFileDetail.mockResolvedValue({
      applicationName: "sys_resources",
      path: "ApplicationFiles/style.css",
      name: "style.css",
      content: "body{}",
      designGaps: ["gap-lock"],
    });
    updateApplicationFile.mockResolvedValue({
      applicationName: "sys_resources",
      path: "ApplicationFiles/style.css",
      name: "style.css",
      content: "body{color:red}",
      designGaps: ["gap-lock"],
    });

    render(<ApplicationFilesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-appfile-apps-table")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-appfile-app-open"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-appfile-table")).toBeTruthy();
    });
    expect(listApplicationFiles).toHaveBeenCalledWith("sys_resources");
    fireEvent.click(screen.getByTestId("developer-appfile-open"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-appfile-detail")).toBeTruthy();
    });
    const editor = screen.getByTestId(
      "developer-appfile-content-editor",
    ) as HTMLTextAreaElement;
    expect(editor.value).toContain("body{}");
    fireEvent.change(editor, { target: { value: "body{color:red}" } });
    fireEvent.click(screen.getByTestId("developer-appfile-save"));
    await waitFor(() => {
      expect(updateApplicationFile).toHaveBeenCalledWith(
        "sys_resources",
        "ApplicationFiles/style.css",
        { content: "body{color:red}" },
      );
    });
    await waitFor(() => {
      expect(screen.getByTestId("developer-appfile-editor-notice").textContent).toBe(
        DEV_MSG.APPFILE_SAVED,
      );
    });
  });

  it("shows empty apps state", async () => {
    listApplications.mockResolvedValue([]);
    render(<ApplicationFilesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-appfile-apps-empty")).toBeTruthy();
    });
  });

  it("shows session-redirect message via panelErrMsg", async () => {
    listApplications.mockRejectedValue(new SessionRedirectError());
    render(<ApplicationFilesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-appfile-apps-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-appfile-apps-error").textContent).toBe(
      DEV_MSG.SESSION_REDIRECT,
    );
  });

  it("shows ApiError status via panelErrMsg", async () => {
    listApplications.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
      body: null,
    });
    render(<ApplicationFilesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-appfile-apps-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-appfile-apps-error").textContent).toBe(
      `${DEV_MSG.APPFILE_APPS_ERROR} (500)`,
    );
  });
});
