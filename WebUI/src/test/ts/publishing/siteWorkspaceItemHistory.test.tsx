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
 * You may obtain a copy of the License at
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { DirtyFormProvider } from "@/publishing/dirtyFormContext";
import { SiteWorkspace } from "@/publishing/sections/SiteWorkspace";
import { fetchItemPublishingHistory } from "@/api/publishing/itemHistoryApi";

vi.mock("@/api/publishing/publishApi", () => ({
  publishSite: vi.fn(),
  incrementalPublishSite: vi.fn(),
  publishIncrementalWithApproval: vi.fn(),
  getIncrementalItems: vi.fn().mockResolvedValue({ items: [], totalCount: 0 }),
  getIncrementalRelatedItems: vi.fn().mockResolvedValue({ items: [], totalCount: 0 }),
  removeIncrementalQueueItem: vi.fn().mockResolvedValue(undefined),
  clearIncrementalQueue: vi.fn().mockResolvedValue(undefined),
  approveIncrementalQueueItem: vi.fn().mockResolvedValue(undefined),
  unapproveIncrementalQueueItem: vi.fn().mockResolvedValue(undefined),
}));

vi.mock("@/api/publishing/serversApi", () => ({
  listServers: vi.fn().mockResolvedValue([
    {
      serverId: 7,
      serverName: "FTP-Prod",
      name: "FTP-Prod",
      siteId: 42,
      serverType: "PRODUCTION",
    },
  ]),
  createServer: vi.fn(),
  deleteServer: vi.fn(),
  getServer: vi.fn(),
  updateServer: vi.fn(),
  isEC2Instance: vi.fn().mockResolvedValue(false),
  fetchAvailableRegions: vi.fn().mockResolvedValue([]),
  stopPublishing: vi.fn(),
}));

vi.mock("@/api/publishing/statusApi", () => ({
  fetchCurrentJobsForSite: vi.fn().mockResolvedValue([]),
}));

vi.mock("@/api/publishing/itemHistoryApi", () => ({
  fetchItemPublishingHistory: vi.fn(),
}));

const fetchHistory = vi.mocked(fetchItemPublishingHistory);

function renderWorkspace(itemId?: string): void {
  render(
    <DirtyFormProvider>
      <SiteWorkspace
        site={{ name: "MySite", siteId: 42 }}
        initialServerId="7"
        itemId={itemId}
        onItemIdChange={vi.fn()}
        onBack={vi.fn()}
      />
    </DirtyFormProvider>,
  );
}

describe("SiteWorkspace item publishing history (#5928)", () => {
  beforeEach(() => {
    fetchHistory.mockReset();
  });

  it("shows the read-only history panel idle until an item id is set", () => {
    renderWorkspace();
    expect(screen.getByTestId("item-publishing-history")).toBeTruthy();
    expect(screen.getByTestId("item-history-idle")).toBeTruthy();
    expect(fetchHistory).not.toHaveBeenCalled();
  });

  it("loads history rows for the workspace item via the existing history route", async () => {
    fetchHistory.mockResolvedValue([
      {
        server: "FTP-Prod",
        location: "/home",
        revisionId: 3,
        publishedDate: 200,
        operation: "publish",
        status: "SUCCESS",
        contentId: 42,
      },
    ]);
    renderWorkspace("42");
    await waitFor(() => {
      expect(screen.getByTestId("item-history-table")).toBeTruthy();
    });
    expect(fetchHistory).toHaveBeenCalledWith("42");
    expect(screen.getAllByTestId("item-history-row")[0].textContent).toContain(
      "FTP-Prod",
    );
  });

  it("looks up a typed item id without writing", async () => {
    fetchHistory.mockResolvedValue([]);
    renderWorkspace();
    fireEvent.change(screen.getByTestId("item-history-id"), {
      target: { value: "43" },
    });
    fireEvent.click(screen.getByTestId("item-history-lookup"));
    await waitFor(() => {
      expect(screen.getByTestId("item-history-empty")).toBeTruthy();
    });
    expect(fetchHistory).toHaveBeenCalledWith("43");
  });
});
