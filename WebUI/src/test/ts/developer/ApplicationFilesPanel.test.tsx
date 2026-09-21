/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { BootstrapProvider } from "../../../main/ts/app/bootstrap/BootstrapContext";
import { DEFAULT_SPA_BOOTSTRAP } from "../../../main/ts/app/bootstrap/types";
import { SessionRedirectError } from "../../../main/ts/api/client";
import * as appFilesApi from "../../../main/ts/api/developer/applicationFilesApi";
import * as pipelinesApi from "../../../main/ts/api/developer/pipelinesApi";
import { DEV_MSG } from "../../../main/ts/developer/messages";
import {
  ApplicationFilesPanel,
  isSafeApplicationFileApiPath,
} from "../../../main/ts/developer/ApplicationFilesPanel";

function renderAdmin(ui: React.ReactElement) {
  return render(
    <BootstrapProvider value={{ ...DEFAULT_SPA_BOOTSTRAP, isAdmin: true }}>
      {ui}
    </BootstrapProvider>,
  );
}

vi.mock("../../../main/ts/api/developer/pipelinesApi", () => ({
  listApplications: vi.fn(),
}));

vi.mock("../../../main/ts/api/developer/applicationFilesApi", () => ({
  listApplicationFiles: vi.fn(),
  getApplicationFileDetail: vi.fn(),
  updateApplicationFile: vi.fn(),
  createApplicationFolder: vi.fn(),
  deleteApplicationPath: vi.fn(),
  moveApplicationPath: vi.fn(),
  lockApplicationFile: vi.fn(),
  unlockApplicationFile: vi.fn(),
  APPLICATION_FILE_DESIGN_GAPS: ["gap-binary"],
}));

const listApplications = pipelinesApi.listApplications as ReturnType<typeof vi.fn>;
const listApplicationFiles = appFilesApi.listApplicationFiles as ReturnType<typeof vi.fn>;
const getApplicationFileDetail = appFilesApi.getApplicationFileDetail as ReturnType<
  typeof vi.fn
>;
const updateApplicationFile = appFilesApi.updateApplicationFile as ReturnType<typeof vi.fn>;
const createApplicationFolder = appFilesApi.createApplicationFolder as ReturnType<typeof vi.fn>;
const deleteApplicationPath = appFilesApi.deleteApplicationPath as ReturnType<typeof vi.fn>;
const moveApplicationPath = appFilesApi.moveApplicationPath as ReturnType<typeof vi.fn>;
const lockApplicationFile = appFilesApi.lockApplicationFile as ReturnType<typeof vi.fn>;
const unlockApplicationFile = appFilesApi.unlockApplicationFile as ReturnType<typeof vi.fn>;

describe("isSafeApplicationFileApiPath", () => {
  it("accepts relative API paths and rejects traversal", () => {
    expect(isSafeApplicationFileApiPath("ApplicationFiles/qa-dir")).toBe(true);
    expect(isSafeApplicationFileApiPath("../escape")).toBe(false);
    expect(isSafeApplicationFileApiPath("/abs")).toBe(false);
    expect(isSafeApplicationFileApiPath("C:/Windows")).toBe(false);
    expect(isSafeApplicationFileApiPath("")).toBe(false);
  });
});

describe("ApplicationFilesPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    listApplications.mockReset();
    listApplicationFiles.mockReset();
    getApplicationFileDetail.mockReset();
    updateApplicationFile.mockReset();
    createApplicationFolder.mockReset();
    deleteApplicationPath.mockReset();
    moveApplicationPath.mockReset();
    lockApplicationFile.mockReset();
    unlockApplicationFile.mockReset();
    lockApplicationFile.mockResolvedValue({ locker: "Admin" });
    unlockApplicationFile.mockResolvedValue(undefined);
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

    renderAdmin(<ApplicationFilesPanel />);
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
    fireEvent.click(screen.getByTestId("developer-appfile-lock"));
    await waitFor(() => {
      expect(lockApplicationFile).toHaveBeenCalled();
    });
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
    renderAdmin(<ApplicationFilesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-appfile-apps-empty")).toBeTruthy();
    });
  });

  it("shows session-redirect message via panelErrMsg", async () => {
    listApplications.mockRejectedValue(new SessionRedirectError());
    renderAdmin(<ApplicationFilesPanel />);
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
    renderAdmin(<ApplicationFilesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-appfile-apps-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-appfile-apps-error").textContent).toBe(
      `${DEV_MSG.APPFILE_APPS_ERROR} (500)`,
    );
  });

  it("Admin can create, rename, and delete a folder then refresh", async () => {
    listApplications.mockResolvedValue([
      { name: "sys_resources", description: "Resources", appRoot: "sys_resources" },
    ]);
    listApplicationFiles
      .mockResolvedValueOnce([
        { path: "ApplicationFiles", name: "ApplicationFiles", directory: true },
      ])
      .mockResolvedValueOnce([
        { path: "ApplicationFiles", name: "ApplicationFiles", directory: true },
        { path: "ApplicationFiles/qa-dir", name: "qa-dir", directory: true },
      ])
      .mockResolvedValueOnce([
        { path: "ApplicationFiles", name: "ApplicationFiles", directory: true },
        { path: "ApplicationFiles/qa-renamed", name: "qa-renamed", directory: true },
      ])
      .mockResolvedValueOnce([
        { path: "ApplicationFiles", name: "ApplicationFiles", directory: true },
      ]);
    createApplicationFolder.mockResolvedValue({
      path: "ApplicationFiles/qa-dir",
      directory: true,
    });
    moveApplicationPath.mockResolvedValue({
      path: "ApplicationFiles/qa-renamed",
      directory: true,
    });
    deleteApplicationPath.mockResolvedValue(undefined);

    renderAdmin(<ApplicationFilesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-appfile-apps-table")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-appfile-app-open"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-appfile-create-folder")).toBeTruthy();
    });

    fireEvent.change(screen.getByTestId("developer-appfile-folder-path"), {
      target: { value: "ApplicationFiles/qa-dir" },
    });
    fireEvent.click(screen.getByTestId("developer-appfile-create-folder"));
    await waitFor(() => {
      expect(createApplicationFolder).toHaveBeenCalledWith(
        "sys_resources",
        "ApplicationFiles/qa-dir",
      );
    });
    await waitFor(() => {
      expect(screen.getByTestId("developer-appfile-files-notice").textContent).toBe(
        DEV_MSG.APPFILE_FOLDER_CREATED,
      );
    });

    const renameBtn = screen.getByLabelText("Rename ApplicationFiles/qa-dir");
    fireEvent.click(renameBtn);
    fireEvent.change(screen.getByTestId("developer-appfile-rename-path"), {
      target: { value: "ApplicationFiles/qa-renamed" },
    });
    fireEvent.click(screen.getByTestId("developer-appfile-rename-save"));
    await waitFor(() => {
      expect(moveApplicationPath).toHaveBeenCalledWith(
        "sys_resources",
        "ApplicationFiles/qa-dir",
        "ApplicationFiles/qa-renamed",
      );
    });

    fireEvent.click(screen.getByLabelText("Delete ApplicationFiles/qa-renamed"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-catalog-confirm-dialog")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-catalog-confirm-submit"));
    await waitFor(() => {
      expect(deleteApplicationPath).toHaveBeenCalledWith(
        "sys_resources",
        "ApplicationFiles/qa-renamed",
      );
    });
  });

  it("creates a missing file via lock then PUT and opens the editor", async () => {
    listApplications.mockResolvedValue([
      { name: "sys_resources", description: "Resources", appRoot: "sys_resources" },
    ]);
    listApplicationFiles.mockResolvedValue([]);
    updateApplicationFile.mockResolvedValue({
      path: "ApplicationFiles/qa-new.txt",
      name: "qa-new.txt",
      content: "",
    });
    getApplicationFileDetail.mockResolvedValue({
      path: "ApplicationFiles/qa-new.txt",
      name: "qa-new.txt",
      content: "",
    });

    renderAdmin(<ApplicationFilesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-appfile-apps-table")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-appfile-app-open"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-appfile-create-file")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-appfile-file-path"), {
      target: { value: "ApplicationFiles/qa-new.txt" },
    });
    fireEvent.click(screen.getByTestId("developer-appfile-create-file"));
    await waitFor(() => {
      expect(lockApplicationFile).toHaveBeenCalledWith(
        "sys_resources",
        "ApplicationFiles/qa-new.txt",
      );
      expect(updateApplicationFile).toHaveBeenCalledWith(
        "sys_resources",
        "ApplicationFiles/qa-new.txt",
        { content: "" },
      );
    });
    await waitFor(() => {
      expect(getApplicationFileDetail).toHaveBeenCalled();
    });
  });
});
