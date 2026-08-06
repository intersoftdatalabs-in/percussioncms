/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import * as serverConfigsApi from "../../../main/ts/api/developer/serverConfigsApi";
import { DEV_MSG } from "../../../main/ts/developer/messages";
import { ServerConfigDetailPanel } from "../../../main/ts/developer/ServerConfigDetailPanel";

vi.mock("../../../main/ts/api/developer/serverConfigsApi", () => ({
  listServerConfigs: vi.fn(),
  getServerConfigDetail: vi.fn(),
}));

const getServerConfigDetail = serverConfigsApi.getServerConfigDetail as ReturnType<typeof vi.fn>;

const sampleDetail = {
  name: "LOG_CONFIG",
  displayName: "Logging configuration",
  fileName: "log4j.xml",
  mimeType: "application/xml",
  characterEncoding: "UTF-8",
  content: "<Configuration/>",
  designGaps: ["gap-save"],
};

describe("ServerConfigDetailPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    getServerConfigDetail.mockReset();
  });

  it("loads detail on success and supports back", async () => {
    getServerConfigDetail.mockResolvedValue(sampleDetail);
    const onBack = vi.fn();
    render(<ServerConfigDetailPanel name="LOG_CONFIG" onBack={onBack} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-cfg-detail-title")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-cfg-detail-title").textContent).toContain(
      "Logging configuration",
    );
    expect(screen.getByTestId("developer-cfg-content-pre").textContent).toContain("Configuration");
    expect(screen.getByTestId("developer-cfg-gaps").textContent).toContain("gap-save");
    expect(getServerConfigDetail).toHaveBeenCalledWith("LOG_CONFIG");
    fireEvent.click(screen.getByTestId("developer-cfg-back"));
    expect(onBack).toHaveBeenCalled();
  });

  it("shows empty content when detail has none", async () => {
    getServerConfigDetail.mockResolvedValue({
      ...sampleDetail,
      content: "",
    });
    render(<ServerConfigDetailPanel name="LOG_CONFIG" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-cfg-content-empty")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-cfg-content-empty").textContent).toBe(
      DEV_MSG.CFG_CONTENT_EMPTY,
    );
    expect(screen.queryByTestId("developer-cfg-content-pre")).toBeNull();
  });

  it("shows session-redirect message via panelErrMsg", async () => {
    getServerConfigDetail.mockRejectedValue(new SessionRedirectError());
    render(<ServerConfigDetailPanel name="LOG_CONFIG" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-cfg-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-cfg-detail-error").textContent).toBe(
      DEV_MSG.SESSION_REDIRECT,
    );
    expect(screen.queryByTestId("developer-cfg-detail-loading")).toBeNull();
    expect(screen.queryByTestId("developer-cfg-detail-title")).toBeNull();
  });

  it("shows ApiError status via panelErrMsg", async () => {
    getServerConfigDetail.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
      body: null,
    });
    render(<ServerConfigDetailPanel name="LOG_CONFIG" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-cfg-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-cfg-detail-error").textContent).toBe(
      `${DEV_MSG.CFG_DETAIL_ERROR} (500)`,
    );
  });

  it("shows Error.message via panelErrMsg", async () => {
    getServerConfigDetail.mockRejectedValue(new Error("network down"));
    render(<ServerConfigDetailPanel name="LOG_CONFIG" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-cfg-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-cfg-detail-error").textContent).toBe(
      `${DEV_MSG.CFG_DETAIL_ERROR} network down`,
    );
    expect(screen.queryByTestId("developer-cfg-detail-title")).toBeNull();
  });

  it("shows fallback when rejection has no message", async () => {
    getServerConfigDetail.mockRejectedValue("boom");
    render(<ServerConfigDetailPanel name="LOG_CONFIG" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-cfg-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-cfg-detail-error").textContent).toBe(
      DEV_MSG.CFG_DETAIL_ERROR,
    );
  });
});
