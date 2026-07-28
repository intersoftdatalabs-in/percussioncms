/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { ProcessMonitorWidget } from "@/dashboard/ProcessMonitorWidget";
import * as gadgetApi from "@/api/dashboard/gadgetApi";

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
