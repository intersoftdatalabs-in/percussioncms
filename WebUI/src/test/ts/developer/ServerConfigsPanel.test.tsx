/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import * as api from "../../../main/ts/api/developer/serverConfigsApi";
import { DEV_MSG } from "../../../main/ts/developer/messages";
import { ServerConfigsPanel } from "../../../main/ts/developer/ServerConfigsPanel";

vi.mock("../../../main/ts/api/developer/serverConfigsApi", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("../../../main/ts/api/developer/serverConfigsApi")>();
  return {
    ...actual,
    listServerConfigs: vi.fn(),
    getServerConfigDetail: vi.fn(),
    updateServerConfig: vi.fn(),
  };
});

const listServerConfigs = api.listServerConfigs as ReturnType<typeof vi.fn>;
const getServerConfigDetail = api.getServerConfigDetail as ReturnType<typeof vi.fn>;

describe("ServerConfigsPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    listServerConfigs.mockReset();
    getServerConfigDetail.mockReset();
  });

  it("lists configs and opens detail with content", async () => {
    listServerConfigs.mockResolvedValue([
      {
        name: "LOG_CONFIG",
        displayName: "Logging configuration",
        fileName: "log4j.xml",
      },
    ]);
    getServerConfigDetail.mockResolvedValue({
      name: "LOG_CONFIG",
      displayName: "Logging configuration",
      fileName: "log4j.xml",
      content: "<Configuration/>",
      designGaps: ["Locking and concurrent edit are not exposed on this Developer surface"],
    });
    render(<ServerConfigsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-cfg-table")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-cfg-open"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-cfg-detail")).toBeTruthy();
    });
    expect(
      (screen.getByTestId("developer-cfg-content-editor") as HTMLTextAreaElement).value,
    ).toContain("Configuration");
    expect(screen.getByTestId("developer-cfg-save")).toBeTruthy();
  });

  it("shows empty state when API returns no configs", async () => {
    listServerConfigs.mockResolvedValue([]);
    render(<ServerConfigsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-cfg-empty")).toBeTruthy();
    });
  });

  it("shows session-redirect message via panelErrMsg", async () => {
    listServerConfigs.mockRejectedValue(new SessionRedirectError());
    render(<ServerConfigsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-cfg-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-cfg-error").textContent).toBe(DEV_MSG.SESSION_REDIRECT);
    expect(screen.queryByTestId("developer-cfg-empty")).toBeNull();
  });

  it("shows ApiError status via panelErrMsg", async () => {
    listServerConfigs.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
      body: null,
    });
    render(<ServerConfigsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-cfg-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-cfg-error").textContent).toBe(
      `${DEV_MSG.CFG_ERROR} (500)`,
    );
  });

  it("shows Error.message via panelErrMsg", async () => {
    listServerConfigs.mockRejectedValue(new Error("network down"));
    render(<ServerConfigsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-cfg-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-cfg-error").textContent).toBe(
      `${DEV_MSG.CFG_ERROR} network down`,
    );
    expect(screen.queryByTestId("developer-cfg-table")).toBeNull();
  });

  it("shows fallback when rejection has no message", async () => {
    listServerConfigs.mockRejectedValue("boom");
    render(<ServerConfigsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-cfg-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-cfg-error").textContent).toBe(DEV_MSG.CFG_ERROR);
  });
});
