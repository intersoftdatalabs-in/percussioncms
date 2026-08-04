/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { ProcessMonitorWidget } from "@/dashboard/ProcessMonitorWidget";
import * as gadgetApi from "@/api/dashboard/gadgetApi";
import { MSG } from "@/i18n/message";

vi.mock("@/api/dashboard/gadgetApi", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("@/api/dashboard/gadgetApi")>();
  return {
    ...actual,
    fetchProcessMonitors: vi.fn(),
  };
});

describe("ProcessMonitorWidget", () => {
  beforeEach(() => {
    vi.mocked(gadgetApi.fetchProcessMonitors).mockReset();
    delete (window as { I18N?: unknown }).I18N;
  });

  afterEach(() => {
    delete (window as { I18N?: unknown }).I18N;
  });

  it("localizes default title via I18N when key resolves (GH-1830)", async () => {
    (window as unknown as { I18N: { message: (k: string) => string } }).I18N = {
      message: (k: string) =>
        k === MSG.GADGET_PROCESS_MONITOR ? "प्रक्रिया मॉनिटर" : k,
    };
    vi.mocked(gadgetApi.fetchProcessMonitors).mockResolvedValue([]);
    render(<ProcessMonitorWidget refreshInterval={0} />);
    await waitFor(() => {
      expect(screen.getByTestId("process-monitor-empty")).toBeDefined();
    });
    const root = screen.getByTestId("process-monitor-widget");
    const titleEl = root.querySelector("div");
    expect(titleEl?.textContent).toBe("प्रक्रिया मॉनिटर");
    expect(titleEl?.textContent).not.toBe("Process Monitor");
  });

  it("lists monitors", async () => {
    vi.mocked(gadgetApi.fetchProcessMonitors).mockResolvedValue([
      {
        designator: "pub",
        name: "Publishing",
        status: "idle",
        message: "Ready",
      },
    ]);
    render(<ProcessMonitorWidget refreshInterval={0} />);
    await waitFor(() => {
      expect(screen.getByTestId("process-monitor-list")).toBeDefined();
    });
    expect(screen.getByText("Publishing")).toBeDefined();
    expect(screen.getByText("Ready")).toBeDefined();
  });

  it("shows error", async () => {
    vi.mocked(gadgetApi.fetchProcessMonitors).mockRejectedValue(
      new Error("monitor down"),
    );
    render(<ProcessMonitorWidget refreshInterval={0} />);
    await waitFor(() => {
      expect(screen.getByTestId("process-monitor-error")).toBeDefined();
    });
  });
});
