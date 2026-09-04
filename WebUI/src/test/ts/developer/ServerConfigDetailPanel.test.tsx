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

const getServerConfigDetail = serverConfigsApi.getServerConfigDetail as ReturnType<typeof vi.fn>;
const updateServerConfig = serverConfigsApi.updateServerConfig as ReturnType<typeof vi.fn>;

const sampleDetail = {
  name: "LOG_CONFIG",
  displayName: "Logging configuration",
  fileName: "log4j.xml",
  mimeType: "application/xml",
  characterEncoding: "UTF-8",
  content: "<Configuration/>",
  designGaps: [
    "Configuration create is not supported via this API (fixed allow-listed set only)",
    "Locking and concurrent edit are not exposed on this Developer surface",
  ],
};

describe("ServerConfigDetailPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    getServerConfigDetail.mockReset();
    updateServerConfig.mockReset();
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
    expect(
      (screen.getByTestId("developer-cfg-content-editor") as HTMLTextAreaElement).value,
    ).toContain("Configuration");
    expect(screen.getByTestId("developer-cfg-gaps").textContent).toContain("Locking");
    expect(screen.getByTestId("developer-cfg-gaps").textContent).not.toMatch(
      /create\s*\/\s*update\s*\/\s*save/i,
    );
    expect(getServerConfigDetail).toHaveBeenCalledWith("LOG_CONFIG");
    fireEvent.click(screen.getByTestId("developer-cfg-back"));
    expect(onBack).toHaveBeenCalled();
  });

  it("shows empty editor when detail has no content", async () => {
    getServerConfigDetail.mockResolvedValue({
      ...sampleDetail,
      content: "",
    });
    render(<ServerConfigDetailPanel name="LOG_CONFIG" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-cfg-content-editor")).toBeTruthy();
    });
    expect(
      (screen.getByTestId("developer-cfg-content-editor") as HTMLTextAreaElement).value,
    ).toBe("");
  });

  it("saves edited content and refreshes detail", async () => {
    getServerConfigDetail.mockResolvedValue(sampleDetail);
    updateServerConfig.mockResolvedValue({
      ...sampleDetail,
      content: "<Configuration updated/>",
    });
    const onSaved = vi.fn();
    render(
      <ServerConfigDetailPanel name="LOG_CONFIG" onBack={() => undefined} onSaved={onSaved} />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("developer-cfg-content-editor")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-cfg-content-editor"), {
      target: { value: "<Configuration updated/>" },
    });
    fireEvent.click(screen.getByTestId("developer-cfg-save"));
    await waitFor(() => {
      expect(updateServerConfig).toHaveBeenCalledWith("LOG_CONFIG", {
        content: "<Configuration updated/>",
      });
    });
    await waitFor(() => {
      expect(screen.getByTestId("developer-cfg-editor-notice").textContent).toBe(
        DEV_MSG.CFG_SAVED,
      );
    });
    expect(screen.getByTestId("developer-cfg-editor-notice").getAttribute("role")).toBe(
      "status",
    );
    expect(
      (screen.getByTestId("developer-cfg-content-editor") as HTMLTextAreaElement).value,
    ).toBe("<Configuration updated/>");
    expect(onSaved).toHaveBeenCalled();
  });

  it("disables save when content is unchanged", async () => {
    getServerConfigDetail.mockResolvedValue(sampleDetail);
    render(<ServerConfigDetailPanel name="LOG_CONFIG" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-cfg-save")).toBeTruthy();
    });
    expect((screen.getByTestId("developer-cfg-save") as HTMLButtonElement).disabled).toBe(true);
    fireEvent.click(screen.getByTestId("developer-cfg-save"));
    expect(updateServerConfig).not.toHaveBeenCalled();
  });

  it("shows save error via panelErrMsg", async () => {
    getServerConfigDetail.mockResolvedValue(sampleDetail);
    updateServerConfig.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
      body: null,
    });
    render(<ServerConfigDetailPanel name="LOG_CONFIG" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-cfg-content-editor")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-cfg-content-editor"), {
      target: { value: "<Configuration boom/>" },
    });
    fireEvent.click(screen.getByTestId("developer-cfg-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-cfg-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-cfg-detail-error").textContent).toBe(
      `${DEV_MSG.CFG_SAVE_ERROR} (500)`,
    );
  });

  it("shows forbidden message on 403 save", async () => {
    getServerConfigDetail.mockResolvedValue(sampleDetail);
    updateServerConfig.mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: null,
    });
    render(<ServerConfigDetailPanel name="LOG_CONFIG" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-cfg-content-editor")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-cfg-content-editor"), {
      target: { value: "<Configuration forbidden/>" },
    });
    fireEvent.click(screen.getByTestId("developer-cfg-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-cfg-detail-error").textContent).toContain(
        DEV_MSG.CFG_FORBIDDEN,
      );
    });
  });

  it("strips stale CFG_GAP_SAVE strings from design gaps", async () => {
    getServerConfigDetail.mockResolvedValue({
      ...sampleDetail,
      designGaps: [
        "Configuration create / update / save not supported via this API",
        "Locking and concurrent edit are not exposed on this Developer surface",
      ],
    });
    render(<ServerConfigDetailPanel name="LOG_CONFIG" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-cfg-gaps")).toBeTruthy();
    });
    const gaps = screen.getByTestId("developer-cfg-gaps").textContent || "";
    expect(gaps).toContain("Locking");
    expect(gaps).not.toMatch(/create\s*\/\s*update\s*\/\s*save/i);
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
