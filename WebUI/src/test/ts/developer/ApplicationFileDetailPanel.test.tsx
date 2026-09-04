/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { BootstrapProvider } from "../../../main/ts/app/bootstrap/BootstrapContext";
import { DEFAULT_SPA_BOOTSTRAP } from "../../../main/ts/app/bootstrap/types";
import * as appFilesApi from "../../../main/ts/api/developer/applicationFilesApi";
import { ApplicationFileDetailPanel } from "../../../main/ts/developer/ApplicationFileDetailPanel";
import { DEV_MSG } from "../../../main/ts/developer/messages";

vi.mock("../../../main/ts/api/developer/applicationFilesApi", () => ({
  getApplicationFileDetail: vi.fn(),
  updateApplicationFile: vi.fn(),
  APPLICATION_FILE_DESIGN_GAPS: ["gap-lock"],
}));

const getApplicationFileDetail = appFilesApi.getApplicationFileDetail as ReturnType<
  typeof vi.fn
>;
const updateApplicationFile = appFilesApi.updateApplicationFile as ReturnType<typeof vi.fn>;

function renderDetail(isAdmin: boolean, path = "ApplicationFiles/a.css") {
  return render(
    <BootstrapProvider value={{ ...DEFAULT_SPA_BOOTSTRAP, isAdmin }}>
      <ApplicationFileDetailPanel
        applicationName="sys_resources"
        path={path}
        onBack={() => undefined}
      />
    </BootstrapProvider>,
  );
}

describe("ApplicationFileDetailPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    vi.spyOn(window, "confirm").mockReturnValue(true);
    getApplicationFileDetail.mockReset();
    updateApplicationFile.mockReset();
  });

  it("shows detail load 404 via APPFILE_DETAIL_ERROR path", async () => {
    getApplicationFileDetail.mockRejectedValue({
      status: 404,
      statusText: "Not Found",
      body: null,
    });
    renderDetail(true);
    await waitFor(() => {
      expect(screen.getByTestId("developer-appfile-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-appfile-detail-error").textContent).toContain(
      DEV_MSG.APPFILE_DETAIL_ERROR,
    );
  });

  it("shows detail load 500", async () => {
    getApplicationFileDetail.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
      body: null,
    });
    renderDetail(true);
    await waitFor(() => {
      expect(screen.getByTestId("developer-appfile-detail-error").textContent).toContain(
        "(500)",
      );
    });
  });

  it("maps save 403 to APPFILE_FORBIDDEN", async () => {
    getApplicationFileDetail.mockResolvedValue({
      applicationName: "sys_resources",
      path: "ApplicationFiles/a.css",
      name: "a.css",
      content: "body{}",
      designGaps: ["gap-lock"],
    });
    updateApplicationFile.mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: null,
    });
    renderDetail(true);
    await waitFor(() => {
      expect(screen.getByTestId("developer-appfile-content-editor")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-appfile-content-editor"), {
      target: { value: "body{color:red}" },
    });
    fireEvent.click(screen.getByTestId("developer-appfile-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-appfile-detail-error").textContent).toContain(
        DEV_MSG.APPFILE_FORBIDDEN,
      );
    });
  });

  it("maps save 500 to APPFILE_SAVE_ERROR", async () => {
    getApplicationFileDetail.mockResolvedValue({
      applicationName: "sys_resources",
      path: "ApplicationFiles/a.css",
      name: "a.css",
      content: "body{}",
      designGaps: ["gap-lock"],
    });
    updateApplicationFile.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
      body: null,
    });
    renderDetail(true);
    await waitFor(() => {
      expect(screen.getByTestId("developer-appfile-content-editor")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-appfile-content-editor"), {
      target: { value: "body{color:red}" },
    });
    fireEvent.click(screen.getByTestId("developer-appfile-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-appfile-detail-error").textContent).toContain(
        DEV_MSG.APPFILE_SAVE_ERROR,
      );
    });
  });

  it("disables save for non-admin and shows admin hint", async () => {
    getApplicationFileDetail.mockResolvedValue({
      applicationName: "sys_resources",
      path: "ApplicationFiles/a.css",
      name: "a.css",
      content: "body{}",
      designGaps: ["gap-lock"],
    });
    renderDetail(false);
    await waitFor(() => {
      expect(screen.getByTestId("developer-appfile-save")).toBeTruthy();
    });
    expect(
      (screen.getByTestId("developer-appfile-save") as HTMLButtonElement).disabled,
    ).toBe(true);
    expect(screen.getByTestId("developer-appfile-admin-hint").textContent).toBe(
      DEV_MSG.APPFILE_SAVE_ADMIN_ONLY,
    );
    expect(updateApplicationFile).not.toHaveBeenCalled();
  });

  it("blocks editor when contentLength exceeds ceiling", async () => {
    getApplicationFileDetail.mockResolvedValue({
      applicationName: "sys_resources",
      path: "ApplicationFiles/big.bin",
      name: "big.bin",
      content: "x",
      contentLength: 3 * 1024 * 1024,
      designGaps: ["gap-lock"],
    });
    renderDetail(true, "ApplicationFiles/big.bin");
    await waitFor(() => {
      expect(screen.getByTestId("developer-appfile-detail-error").textContent).toBe(
        DEV_MSG.APPFILE_TOO_LARGE,
      );
    });
    expect(screen.queryByTestId("developer-appfile-content-editor")).toBeNull();
  });
});
