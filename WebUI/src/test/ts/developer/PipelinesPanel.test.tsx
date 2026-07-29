/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import React from "react";
import { cleanup, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { listApplications } from "../../../main/ts/api/developer/pipelinesApi";
import { PipelinesPanel } from "../../../main/ts/developer/PipelinesPanel";

vi.mock("../../../main/ts/api/developer/pipelinesApi", () => ({
  listApplications: vi.fn(),
}));

const listApplicationsMock = vi.mocked(listApplications);

describe("PipelinesPanel", () => {
  beforeEach(() => {
    listApplicationsMock.mockReset();
  });

  afterEach(() => {
    cleanup();
  });

  it("renders catalog rows when applications load", async () => {
    listApplicationsMock.mockResolvedValue([
      {
        id: 1,
        name: "sys_cmpDocuments",
        description: "System content editor app",
        enabled: true,
        appType: "CONTENT_EDITOR",
        appRoot: "sys_cmpDocuments",
      },
    ]);
    render(<PipelinesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-table")).toBeTruthy();
    });
    expect(screen.getAllByText("sys_cmpDocuments").length).toBeGreaterThan(0);
    expect(screen.getByText("CONTENT_EDITOR")).toBeTruthy();
  });

  it("shows empty state when API returns no applications", async () => {
    listApplicationsMock.mockResolvedValue([]);
    render(<PipelinesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-empty")).toBeTruthy();
    });
  });

  it("shows error UI when pipelines fetch fails", async () => {
    listApplicationsMock.mockRejectedValue({ status: 500, statusText: "Error" });
    render(<PipelinesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-pipe-error").textContent).toMatch(/500/);
  });
});
