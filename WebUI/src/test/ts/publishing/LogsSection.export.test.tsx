/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { LogsSection } from "@/publishing/sections/LogsSection";

const fetchPublishingLogs = vi.fn();
const downloadPublishingLogsCsv = vi.fn();

vi.mock("@/api/home/homeApi", () => ({
  fetchSites: vi.fn().mockResolvedValue([]),
}));

vi.mock("@/api/publishing/statusApi", () => ({
  fetchPublishingLogs: (...args: unknown[]) => fetchPublishingLogs(...args),
  fetchLogDetails: vi.fn(),
  purgePublishingLogs: vi.fn(),
}));

vi.mock("@/api/publishing/itemHistoryApi", () => ({
  fetchItemPublishingHistory: vi.fn().mockResolvedValue([]),
}));

vi.mock("@/publishing/logsExport", async () => {
  const actual = await vi.importActual<typeof import("@/publishing/logsExport")>(
    "@/publishing/logsExport",
  );
  return {
    ...actual,
    downloadPublishingLogsCsv: (...args: unknown[]) =>
      downloadPublishingLogsCsv(...args),
  };
});

describe("LogsSection export filtered logs", () => {
  beforeEach(() => {
    fetchPublishingLogs.mockReset();
    downloadPublishingLogsCsv.mockReset();
    fetchPublishingLogs.mockResolvedValue([
      {
        jobId: 10,
        siteName: "FastForward",
        serverName: "prod",
        status: "Completed",
      },
      {
        jobId: 11,
        siteName: "Marketing",
        serverName: "stage",
        status: "Failed",
      },
    ]);
  });

  it("exports only the rows that match the on-screen filter", async () => {
    render(<LogsSection />);
    fireEvent.click(screen.getByTestId("logs-filter-apply"));
    await waitFor(() => {
      expect(screen.getByTestId("publish-log-row-10")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("logs-filter-query"), {
      target: { value: "market" },
    });
    fireEvent.click(screen.getByTestId("logs-export-filtered"));
    expect(downloadPublishingLogsCsv).toHaveBeenCalledTimes(1);
    const csv = String(downloadPublishingLogsCsv.mock.calls[0][0]).replace(
      /\r\n/g,
      "\n",
    );
    expect(csv).toContain("11,Marketing,stage,Failed");
    expect(csv).not.toContain("FastForward");
    expect(screen.queryByTestId("publish-logs-export-error")).toBeNull();
  });

  it("still exports headers when the filter is empty", async () => {
    render(<LogsSection />);
    fireEvent.click(screen.getByTestId("logs-filter-apply"));
    await waitFor(() => {
      expect(screen.getByTestId("publish-log-row-10")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("logs-filter-query"), {
      target: { value: "no-such-site" },
    });
    expect(screen.getByTestId("publish-logs-empty")).toBeTruthy();
    fireEvent.click(screen.getByTestId("logs-export-filtered"));
    const csv = String(downloadPublishingLogsCsv.mock.calls[0][0]).replace(
      /\r\n/g,
      "\n",
    );
    expect(csv).toBe("Job,Site,Server,Status\n");
  });

  it("shows an error when the file cannot be built", async () => {
    downloadPublishingLogsCsv.mockImplementation(() => {
      throw new Error("blob failed");
    });
    render(<LogsSection />);
    fireEvent.click(screen.getByTestId("logs-export-filtered"));
    await waitFor(() => {
      expect(
        screen.getByTestId("publish-logs-export-error").textContent,
      ).toContain("Could not export the filtered publish logs.");
    });
  });
});
