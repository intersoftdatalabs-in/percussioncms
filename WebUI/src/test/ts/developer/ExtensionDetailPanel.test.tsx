/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import * as extensionsApi from "../../../main/ts/api/developer/extensionsApi";
import { DEV_MSG } from "../../../main/ts/developer/messages";
import { ExtensionDetailPanel } from "../../../main/ts/developer/ExtensionDetailPanel";

vi.mock("../../../main/ts/api/developer/extensionsApi", () => ({
  listExtensions: vi.fn(),
  getExtensionDetail: vi.fn(),
}));

const getExtensionDetail = extensionsApi.getExtensionDetail as ReturnType<typeof vi.fn>;

const sampleDetail = {
  extensionName: "sys_add",
  fqn: "Java/global/percussion/sys_add",
  handlerName: "Java",
  context: "global/percussion/",
  version: 1,
  supportedInterfaces: ["com.percussion.extension.IPSExtension"],
  runtimeParameters: [{ name: "htmlParams", dataType: "java.util.Map" }],
};

describe("ExtensionDetailPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    getExtensionDetail.mockReset();
  });

  it("loads detail on success and supports back", async () => {
    getExtensionDetail.mockResolvedValue(sampleDetail);
    const onBack = vi.fn();
    render(<ExtensionDetailPanel idOrName="sys_add" onBack={onBack} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ex-detail-title")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ex-detail-title").textContent).toContain("sys_add");
    expect(screen.getByTestId("developer-ex-params-table")).toBeTruthy();
    expect(screen.getByTestId("developer-ex-ifaces").textContent).toContain(
      "com.percussion.extension.IPSExtension",
    );
    expect(getExtensionDetail).toHaveBeenCalledWith("sys_add");
    fireEvent.click(screen.getByTestId("developer-ex-back"));
    expect(onBack).toHaveBeenCalled();
  });

  it("shows empty params section when detail has none", async () => {
    getExtensionDetail.mockResolvedValue({
      ...sampleDetail,
      runtimeParameters: [],
      supportedInterfaces: [],
    });
    render(<ExtensionDetailPanel idOrName="sys_add" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ex-params-empty")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ex-params-empty").textContent).toBe(DEV_MSG.EX_NONE);
    expect(screen.queryByTestId("developer-ex-params-table")).toBeNull();
    expect(screen.getByTestId("developer-ex-ifaces").textContent).toContain(DEV_MSG.EX_NONE);
  });

  it("shows session-redirect message via panelErrMsg", async () => {
    getExtensionDetail.mockRejectedValue(new SessionRedirectError());
    render(<ExtensionDetailPanel idOrName="sys_add" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ex-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ex-detail-error").textContent).toBe(
      DEV_MSG.SESSION_REDIRECT,
    );
    expect(screen.queryByTestId("developer-ex-detail-loading")).toBeNull();
    expect(screen.queryByTestId("developer-ex-detail-title")).toBeNull();
  });

  it("shows ApiError status via panelErrMsg", async () => {
    getExtensionDetail.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
      body: null,
    });
    render(<ExtensionDetailPanel idOrName="sys_add" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ex-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ex-detail-error").textContent).toBe(
      `${DEV_MSG.EX_DETAIL_ERROR} (500)`,
    );
  });

  it("shows Error.message via panelErrMsg", async () => {
    getExtensionDetail.mockRejectedValue(new Error("network down"));
    render(<ExtensionDetailPanel idOrName="sys_add" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ex-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ex-detail-error").textContent).toBe(
      `${DEV_MSG.EX_DETAIL_ERROR} network down`,
    );
    expect(screen.queryByTestId("developer-ex-detail-title")).toBeNull();
  });

  it("shows fallback when rejection has no message", async () => {
    getExtensionDetail.mockRejectedValue("boom");
    render(<ExtensionDetailPanel idOrName="sys_add" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ex-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ex-detail-error").textContent).toBe(
      DEV_MSG.EX_DETAIL_ERROR,
    );
  });
});
