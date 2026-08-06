/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import * as extensionsApi from "../../../main/ts/api/developer/extensionsApi";
import { DEV_MSG } from "../../../main/ts/developer/messages";
import { ExtensionsPanel } from "../../../main/ts/developer/ExtensionsPanel";

vi.mock("../../../main/ts/api/developer/extensionsApi", () => ({
  listExtensions: vi.fn(),
  getExtensionDetail: vi.fn(),
}));

const listExtensions = extensionsApi.listExtensions as ReturnType<typeof vi.fn>;
const getExtensionDetail = extensionsApi.getExtensionDetail as ReturnType<typeof vi.fn>;

const sampleExtension = {
  extensionName: "sys_add",
  handlerName: "Java",
  context: "global/percussion/",
  fqn: "Java/global/percussion/sys_add",
  category: "sys",
};

const sampleDetail = {
  extensionName: "sys_add",
  fqn: "Java/global/percussion/sys_add",
  supportedInterfaces: ["com.percussion.extension.IPSExtension"],
  runtimeParameters: [{ name: "htmlParams", dataType: "java.util.Map" }],
};

describe("ExtensionsPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    listExtensions.mockReset();
    getExtensionDetail.mockReset();
  });

  it("lists extensions and opens detail", async () => {
    listExtensions.mockResolvedValue([sampleExtension]);
    getExtensionDetail.mockResolvedValue(sampleDetail);
    render(<ExtensionsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ex-table")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ex-table").textContent).toContain("sys_add");
    fireEvent.click(screen.getByTestId("developer-ex-open"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ex-detail")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ex-params-table")).toBeTruthy();
    fireEvent.click(screen.getByTestId("developer-ex-back"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ex-table")).toBeTruthy();
    });
  });

  it("shows empty state when API returns no extensions", async () => {
    listExtensions.mockResolvedValue([]);
    render(<ExtensionsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ex-empty")).toBeTruthy();
    });
  });

  it("shows session-redirect message via panelErrMsg", async () => {
    listExtensions.mockRejectedValue(new SessionRedirectError());
    render(<ExtensionsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ex-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ex-error").textContent).toBe(DEV_MSG.SESSION_REDIRECT);
    expect(screen.queryByTestId("developer-ex-empty")).toBeNull();
  });

  it("shows ApiError status via panelErrMsg", async () => {
    listExtensions.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
      body: null,
    });
    render(<ExtensionsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ex-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ex-error").textContent).toBe(`${DEV_MSG.EX_ERROR} (500)`);
  });

  it("shows Error.message via panelErrMsg", async () => {
    listExtensions.mockRejectedValue(new Error("network down"));
    render(<ExtensionsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ex-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ex-error").textContent).toBe(
      `${DEV_MSG.EX_ERROR} network down`,
    );
    expect(screen.queryByTestId("developer-ex-table")).toBeNull();
  });

  it("shows fallback when rejection has no message", async () => {
    listExtensions.mockRejectedValue("boom");
    render(<ExtensionsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ex-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ex-error").textContent).toBe(DEV_MSG.EX_ERROR);
  });
});
