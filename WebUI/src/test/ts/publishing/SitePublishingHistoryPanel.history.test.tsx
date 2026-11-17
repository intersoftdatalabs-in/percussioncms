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
import { renderA11yGate } from "../contentExplorer/a11y";
import { SitePublishingHistoryPanel } from "@/publishing/components/SitePublishingHistoryPanel";

const fetchPublishingLogs = vi.fn();
const downloadPublishingLogsCsv = vi.fn();

vi.mock("@/api/publishing/statusApi", () => ({
  fetchPublishingLogs: (...args: unknown[]) => fetchPublishingLogs(...args),
  fetchLogDetails: vi.fn(),
  purgePublishingLogs: vi.fn(),
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

const ROWS = [
  { jobId: 10, siteName: "FastForward", serverName: "prod", status: "Completed" },
  { jobId: 11, siteName: "FastForward", serverName: "stage", status: "Failed" },
];

describe("SitePublishingHistoryPanel (#6473)", () => {
  beforeEach(() => {
    fetchPublishingLogs.mockReset();
    downloadPublishingLogsCsv.mockReset();
    fetchPublishingLogs.mockResolvedValue(ROWS);
  });

  it("loads history scoped to the workspace site", async () => {
    render(<SitePublishingHistoryPanel siteId={42} siteName="FastForward" />);
    await waitFor(() => {
      expect(screen.getAllByTestId("publish-site-history-row")).toHaveLength(2);
    });
    expect(fetchPublishingLogs).toHaveBeenCalledTimes(1);
    expect(fetchPublishingLogs.mock.calls[0][0]).toMatchObject({ siteId: "42" });
    expect(screen.getByTestId("publish-site-history-count")).toHaveTextContent(
      "Showing 2 of 2 history rows",
    );
  });

  it("narrows rows with the status filter and search", async () => {
    render(<SitePublishingHistoryPanel siteId={42} siteName="FastForward" />);
    await waitFor(() => {
      expect(screen.getAllByTestId("publish-site-history-row")).toHaveLength(2);
    });
    fireEvent.change(screen.getByTestId("publish-site-history-status"), {
      target: { value: "failed" },
    });
    expect(screen.getAllByTestId("publish-site-history-row")).toHaveLength(1);
    expect(screen.getByTestId("publish-site-history-count")).toHaveTextContent(
      "Showing 1 of 2 history rows",
    );
    fireEvent.change(screen.getByTestId("publish-site-history-status"), {
      target: { value: "all" },
    });
    fireEvent.change(screen.getByTestId("publish-site-history-query"), {
      target: { value: "stage" },
    });
    const rows = screen.getAllByTestId("publish-site-history-row");
    expect(rows).toHaveLength(1);
    expect(rows[0].textContent).toContain("11");
  });

  it("exports the visible rows to a per-site CSV file", async () => {
    render(<SitePublishingHistoryPanel siteId={42} siteName="FastForward" />);
    await waitFor(() => {
      expect(screen.getAllByTestId("publish-site-history-row")).toHaveLength(2);
    });
    fireEvent.change(screen.getByTestId("publish-site-history-status"), {
      target: { value: "success" },
    });
    fireEvent.click(screen.getByTestId("publish-site-history-export"));
    expect(downloadPublishingLogsCsv).toHaveBeenCalledTimes(1);
    const [csv, filename] = downloadPublishingLogsCsv.mock.calls[0] as [
      string,
      string,
    ];
    expect(filename).toBe("publish-history-fastforward.csv");
    expect(csv.replace(/\r\n/g, "\n")).toContain("10,FastForward,prod,Completed");
    expect(csv).not.toContain("stage");
    expect(screen.queryByTestId("publish-site-history-export-error")).toBeNull();
  });

  it("shows the empty state when the site has no history", async () => {
    fetchPublishingLogs.mockResolvedValue([]);
    render(<SitePublishingHistoryPanel siteId={42} siteName="FastForward" />);
    await waitFor(() => {
      expect(screen.getByTestId("publish-site-history-empty")).toBeTruthy();
    });
    expect(screen.queryByTestId("publish-site-history-table")).toBeNull();
  });

  it("shows an error when the history request fails", async () => {
    fetchPublishingLogs.mockRejectedValue(new Error("boom"));
    render(<SitePublishingHistoryPanel siteId={42} siteName="FastForward" />);
    await waitFor(() => {
      expect(screen.getByTestId("publish-site-history-error")).toBeTruthy();
    });
    expect(screen.queryByTestId("publish-site-history-table")).toBeNull();
  });

  it("reloads history on refresh", async () => {
    render(<SitePublishingHistoryPanel siteId={42} siteName="FastForward" />);
    await waitFor(() => {
      expect(fetchPublishingLogs).toHaveBeenCalledTimes(1);
    });
    fireEvent.click(screen.getByTestId("publish-site-history-refresh"));
    await waitFor(() => {
      expect(fetchPublishingLogs).toHaveBeenCalledTimes(2);
    });
  });

  it("shows a reload CTA on the truly-empty state (#6595)", async () => {
    fetchPublishingLogs.mockResolvedValue([]);
    render(<SitePublishingHistoryPanel siteId={42} siteName="FastForward" />);
    await waitFor(() => {
      expect(screen.getByTestId("publish-site-history-empty")).toBeTruthy();
    });
    const cta = screen.getByTestId("publish-site-history-empty-reload");
    expect(cta).toHaveTextContent("Reload history");
    fireEvent.click(cta);
    await waitFor(() => {
      expect(fetchPublishingLogs).toHaveBeenCalledTimes(2);
    });
  });

  it("shows filtered-empty copy when filters hide every loaded row (#6595)", async () => {
    render(<SitePublishingHistoryPanel siteId={42} siteName="FastForward" />);
    await waitFor(() => {
      expect(screen.getAllByTestId("publish-site-history-row")).toHaveLength(2);
    });
    fireEvent.change(screen.getByTestId("publish-site-history-query"), {
      target: { value: "no-such-server-zzz" },
    });
    await waitFor(() => {
      expect(
        screen.getByTestId("publish-site-history-empty-filtered"),
      ).toBeTruthy();
    });
    expect(screen.queryByTestId("publish-site-history-empty")).toBeNull();
    expect(screen.queryByTestId("publish-site-history-table")).toBeNull();
    expect(
      screen.getByTestId("publish-site-history-empty-filtered"),
    ).toHaveTextContent("No history rows match the current filters");
  });

  it("names the export-empty error instead of downloading zero rows (#6595)", async () => {
    render(<SitePublishingHistoryPanel siteId={42} siteName="FastForward" />);
    await waitFor(() => {
      expect(screen.getAllByTestId("publish-site-history-row")).toHaveLength(2);
    });
    fireEvent.change(screen.getByTestId("publish-site-history-status"), {
      target: { value: "success" },
    });
    fireEvent.change(screen.getByTestId("publish-site-history-query"), {
      target: { value: "no-such-server-zzz" },
    });
    fireEvent.click(screen.getByTestId("publish-site-history-export"));
    await waitFor(() => {
      expect(screen.getByTestId("publish-site-history-export-error")).toHaveTextContent(
        "No visible history rows to export for this site",
      );
    });
    expect(downloadPublishingLogsCsv).not.toHaveBeenCalled();
  });

  it("renders with zero serious/critical a11y violations (#6595)", async () => {
    const { container } = render(
      <SitePublishingHistoryPanel siteId={42} siteName="FastForward" />,
    );
    await waitFor(() => {
      expect(screen.getAllByTestId("publish-site-history-row")).toHaveLength(2);
    });
    await renderA11yGate(container);
  });

  it("renders the empty state with zero serious/critical a11y violations (#6595)", async () => {
    fetchPublishingLogs.mockResolvedValue([]);
    const { container } = render(
      <SitePublishingHistoryPanel siteId={42} siteName="FastForward" />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("publish-site-history-empty")).toBeTruthy();
    });
    await renderA11yGate(container);
  });
});
