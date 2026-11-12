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

import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { PublishingShell } from "@/publishing/PublishingShell";

vi.mock("@/api/home/homeApi", () => ({
  fetchSites: vi.fn().mockResolvedValue([]),
}));

vi.mock("@/api/publishing/statusApi", () => ({
  fetchCurrentJobs: vi.fn().mockResolvedValue([
    {
      jobId: 42,
      siteName: "FastForward",
      editionName: "Full",
      status: "Running",
      completedItems: 1,
      totalItems: 2,
    },
  ]),
  fetchCurrentJobsForSite: vi.fn().mockResolvedValue([]),
  fetchPublishingLogs: vi.fn().mockResolvedValue([
    {
      jobId: 77,
      siteName: "FastForward",
      serverName: "prod",
      status: "Completed",
    },
  ]),
  fetchLogDetails: vi.fn().mockResolvedValue({ SitePublishItem: [] }),
  purgePublishingLogs: vi.fn().mockResolvedValue({}),
}));

vi.mock("@/api/publishing/itemHistoryApi", () => ({
  fetchItemPublishingHistory: vi.fn().mockResolvedValue([]),
}));

vi.mock("@/api/publishing/itemScheduleDatesApi", () => ({
  fetchItemScheduleDates: vi.fn().mockResolvedValue({
    itemId: "",
    startDate: "",
    endDate: "",
    comments: "",
  }),
  saveItemScheduleDates: vi.fn().mockResolvedValue(undefined),
}));

describe("PublishingShell job/request deep links (#6215)", () => {
  it("opens the job detail for a logs jobId deep link", async () => {
    render(<PublishingShell section="logs" jobId="77" />);
    await waitFor(() => {
      expect(screen.getByTestId("publish-log-details")).toBeTruthy();
    });
  });

  it("degrades unknown logs jobIds to the plain list", async () => {
    render(<PublishingShell section="logs" jobId="999" />);
    await waitFor(() => {
      expect(screen.getByTestId("publish-logs-table")).toBeTruthy();
    });
    expect(screen.queryByTestId("publish-log-details")).toBeNull();
    expect(screen.queryByRole("alert")).toBeNull();
  });

  it("surfaces the request for a status requestId deep link", async () => {
    render(<PublishingShell section="status" requestId="42" />);
    await waitFor(() => {
      expect(
        screen.getByTestId("publish-status-detail-job-id").textContent,
      ).toBe("42");
    });
  });

  it("degrades unknown status requestIds to the plain list", async () => {
    render(<PublishingShell section="status" requestId="999" />);
    await waitFor(() => {
      expect(screen.getByTestId("publish-section-status")).toBeTruthy();
    });
    // Jobs load but no detail panel opens for the unknown id.
    await waitFor(() => {
      expect(screen.getByTestId("publish-status-job-42")).toBeTruthy();
    });
    expect(
      screen.queryByTestId("publish-status-detail-job-id"),
    ).toBeNull();
    expect(screen.queryByRole("alert")).toBeNull();
  });

  it("drops unsafe deep-link ids before they reach the sections", async () => {
    render(
      <PublishingShell
        section="status"
        requestId="<script>"
        jobId="1/2"
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("publish-section-status")).toBeTruthy();
    });
    expect(
      screen.queryByTestId("publish-status-detail-job-id"),
    ).toBeNull();
  });
});
