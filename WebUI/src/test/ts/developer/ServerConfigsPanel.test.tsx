/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ServerConfigsPanel } from "../../../main/ts/developer/ServerConfigsPanel";
import * as api from "../../../main/ts/api/developer/serverConfigsApi";

vi.mock("../../../main/ts/api/developer/serverConfigsApi", () => ({
  listServerConfigs: vi.fn(),
  getServerConfigDetail: vi.fn(),
}));

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
      designGaps: ["gap-save"],
    });
    render(<ServerConfigsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-cfg-table")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-cfg-open"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-cfg-detail")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-cfg-content-pre").textContent).toContain(
      "Configuration",
    );
  });

  it("shows empty and error states", async () => {
    listServerConfigs.mockResolvedValueOnce([]);
    const { unmount } = render(<ServerConfigsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-cfg-empty")).toBeTruthy();
    });
    unmount();
    listServerConfigs.mockRejectedValueOnce(new Error("down"));
    render(<ServerConfigsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-cfg-error")).toBeTruthy();
    });
  });
});
