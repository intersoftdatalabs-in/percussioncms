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
import { DirtyFormProvider } from "@/publishing/dirtyFormContext";
import { SiteWorkspace } from "@/publishing/sections/SiteWorkspace";
import {
  loadLinkedPagesForTakedown,
  takedownSelectedItem,
} from "@/contentExplorer/itemPublish";

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
  fetchItemPublishingHistory: vi.fn().mockResolvedValue([]),
}));

vi.mock("@/api/publishing/itemScheduleDatesApi", () => ({
  fetchItemScheduleDates: vi.fn().mockResolvedValue({
    itemId: "",
    startDate: "",
    endDate: "",
    comments: "",
  }),
  saveItemScheduleDates: vi.fn(),
}));

vi.mock("@/api/publishing/itemPublishingActionsApi", () => ({
  fetchItemPublishingActions: vi.fn().mockResolvedValue([]),
}));

vi.mock("@/contentExplorer/itemPublish", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("@/contentExplorer/itemPublish")>();
  return {
    ...actual,
    loadLinkedPagesForTakedown: vi.fn(),
    takedownSelectedItem: vi.fn(),
  };
});

const loadLinked = vi.mocked(loadLinkedPagesForTakedown);
const takeDown = vi.mocked(takedownSelectedItem);

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

describe("SiteWorkspace item takedown (#5930)", () => {
  beforeEach(() => {
    loadLinked.mockReset();
    takeDown.mockReset();
    loadLinked.mockResolvedValue([]);
    takeDown.mockResolvedValue(true);
  });

  it("renders the takedown panel on the site workspace", () => {
    renderWorkspace();
    expect(screen.getByTestId("item-takedown")).toBeTruthy();
    expect(loadLinked).not.toHaveBeenCalled();
    expect(takeDown).not.toHaveBeenCalled();
  });

  it("reviews linked pages for the selected workspace item", async () => {
    loadLinked.mockResolvedValue([
      { id: "7", pagePath: "/Sites/Demo/Home" },
    ]);
    renderWorkspace("42");
    await waitFor(() => {
      expect(loadLinked).toHaveBeenCalledWith("42");
    });
    expect(screen.getByTestId("item-takedown-linked").textContent).toContain(
      "/Sites/Demo/Home",
    );
  });

  it("takes down the selected item through the existing takedown route", async () => {
    renderWorkspace("42");
    await waitFor(() => expect(loadLinked).toHaveBeenCalledWith("42"));
    fireEvent.click(screen.getByTestId("item-takedown-submit"));
    await waitFor(() => {
      expect(takeDown).toHaveBeenCalled();
    });
    const item = takeDown.mock.calls[0]?.[0];
    expect(item?.id).toBe("42");
    expect(item?.path).toMatch(/\/Sites\//);
    expect(screen.getByTestId("item-takedown-success")).toBeTruthy();
  });

  it("keeps a failed takedown on the confirm as an error, not success", async () => {
    takeDown.mockRejectedValue(new Error("HTTP 409"));
    renderWorkspace("42");
    await waitFor(() => expect(loadLinked).toHaveBeenCalledWith("42"));
    fireEvent.click(screen.getByTestId("item-takedown-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("item-takedown-error")).toBeTruthy();
    });
    expect(screen.queryByTestId("item-takedown-success")).toBeNull();
    expect(screen.getByTestId("item-takedown-confirm")).toBeTruthy();
  });
});
