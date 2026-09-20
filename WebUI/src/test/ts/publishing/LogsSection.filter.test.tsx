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

vi.mock("@/api/home/homeApi", () => ({
  fetchSites: vi.fn().mockResolvedValue([{ name: "FastForward", id: "1" }]),
}));

vi.mock("@/api/publishing/statusApi", () => ({
  fetchPublishingLogs: (...args: unknown[]) => fetchPublishingLogs(...args),
  fetchLogDetails: vi.fn(),
  purgePublishingLogs: vi.fn(),
}));

vi.mock("@/api/publishing/itemHistoryApi", () => ({
  fetchItemPublishingHistory: vi.fn().mockResolvedValue([]),
}));

describe("LogsSection search and filter", () => {
  beforeEach(() => {
    fetchPublishingLogs.mockReset();
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

  it("filters loaded rows by search text and status", async () => {
    render(<LogsSection />);
    fireEvent.click(screen.getByTestId("logs-filter-apply"));
    await waitFor(() => {
      expect(screen.getByTestId("publish-log-row-10")).toBeTruthy();
    });
    expect(screen.getByTestId("publish-log-row-11")).toBeTruthy();

    fireEvent.change(screen.getByTestId("logs-filter-query"), {
      target: { value: "market" },
    });
    expect(screen.queryByTestId("publish-log-row-10")).toBeNull();
    expect(screen.getByTestId("publish-log-row-11")).toBeTruthy();

    fireEvent.change(screen.getByTestId("logs-filter-query"), {
      target: { value: "" },
    });
    fireEvent.change(screen.getByTestId("logs-filter-status"), {
      target: { value: "failed" },
    });
    expect(screen.queryByTestId("publish-log-row-10")).toBeNull();
    expect(screen.getByTestId("publish-log-row-11")).toBeTruthy();
  });

  it("sends showOnlyFailures when the server checkbox is checked", async () => {
    render(<LogsSection />);
    fireEvent.click(screen.getByTestId("logs-filter-failures"));
    fireEvent.click(screen.getByTestId("logs-filter-apply"));
    await waitFor(() => {
      expect(fetchPublishingLogs).toHaveBeenCalled();
    });
    const req = fetchPublishingLogs.mock.calls[0][0] as {
      showOnlyFailures?: boolean;
    };
    expect(req.showOnlyFailures).toBe(true);
  });
});
