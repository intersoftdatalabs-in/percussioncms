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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { PublishingShell } from "@/publishing/PublishingShell";

vi.mock("@/api/home/homeApi", () => ({
  fetchSites: vi.fn().mockResolvedValue([{ name: "SiteA", siteId: "1" }]),
}));

vi.mock("@/api/publishing/serversApi", () => ({
  listServers: vi.fn().mockResolvedValue([
    { serverId: "7", serverName: "LocalFS" },
  ]),
  stopPublishing: vi.fn(),
}));

vi.mock("@/api/publishing/runtimeApi", () => ({
  listRuntimeEditions: vi.fn().mockResolvedValue([
    { editionId: "10", name: "Full", runningJobId: 0, pubServerId: "7" },
    { editionId: "11", name: "Demand", runningJobId: 99, jobStatus: "Running" },
  ]),
  startEditionJob: vi.fn(),
  stopRuntimeJob: vi.fn(),
  demandPublish: vi.fn(),
  clearSiteItems: vi.fn(),
  purgeRuntimeJobLog: vi.fn(),
}));

vi.mock("@/api/publishing/statusApi", () => ({
  fetchCurrentJobs: vi.fn().mockResolvedValue([
    {
      jobId: 99,
      siteName: "SiteA",
      editionName: "Demand",
      status: "Running",
      completedItems: 0,
      totalItems: 1,
    },
  ]),
}));

vi.mock("@/api/publishing/itemHistoryApi", () => ({
  fetchItemPublishingHistory: vi.fn().mockResolvedValue([]),
}));

describe("PublishingShell runtime open job (#4838)", () => {
  it("navigates from a running edition to Status job detail", async () => {
    render(<PublishingShell section="runtime" />);
    await waitFor(() => {
      expect(screen.getByTestId("runtime-open-job-11")).toBeTruthy();
    });
    expect(screen.queryByTestId("runtime-open-job-10")).toBeNull();
    fireEvent.click(screen.getByTestId("runtime-open-job-11"));
    await waitFor(() => {
      expect(screen.getByTestId("publish-section-status")).toBeTruthy();
    });
    await waitFor(() => {
      expect(screen.getByTestId("publish-status-detail-job-id").textContent).toBe(
        "99",
      );
    });
    expect(screen.getByTestId("publish-status-detail-edition").textContent).toBe(
      "Demand",
    );
  });
});
