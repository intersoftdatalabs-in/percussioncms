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
import { afterEach, describe, expect, it, vi } from "vitest";
import { DirtyFormProvider } from "@/publishing/dirtyFormContext";
import { SiteWorkspace } from "@/publishing/sections/SiteWorkspace";

vi.mock("@/api/publishing/publishApi", () => ({
  publishSite: vi.fn(),
  incrementalPublishSite: vi.fn(),
  publishIncrementalWithApproval: vi.fn(),
  getIncrementalItems: vi.fn().mockResolvedValue({ items: [], totalCount: 0 }),
  getIncrementalRelatedItems: vi.fn().mockResolvedValue({ items: [], totalCount: 0 }),
  removeIncrementalQueueItem: vi.fn().mockResolvedValue(undefined),
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

const publishApi = await import("@/api/publishing/publishApi");

function renderWorkspace(): void {
  render(
    <DirtyFormProvider>
      <SiteWorkspace
        site={{ name: "MySite", siteId: 42 }}
        initialServerId="7"
        onBack={vi.fn()}
      />
    </DirtyFormProvider>,
  );
}

describe("SiteWorkspace incremental queue list (#4787)", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    vi.mocked(publishApi.getIncrementalItems).mockReset();
    vi.mocked(publishApi.getIncrementalRelatedItems).mockReset();
    vi.mocked(publishApi.removeIncrementalQueueItem).mockReset();
    vi.mocked(publishApi.getIncrementalRelatedItems).mockResolvedValue({
      items: [],
      totalCount: 0,
    });
    vi.mocked(publishApi.removeIncrementalQueueItem).mockResolvedValue(undefined);
  });

  it("renders queued items as id and label rows", async () => {
    vi.mocked(publishApi.getIncrementalItems).mockResolvedValue({
      PagedItemList: {
        childrenInPage: [
          { id: "301", name: "Home Page" },
          { contentid: 88, title: "About" },
        ],
      },
    });
    renderWorkspace();
    await waitFor(() => {
      expect(screen.getByText("FTP-Prod")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("publish-incremental-preview-btn"));
    await waitFor(() => {
      expect(screen.getByTestId("publish-incremental-queue-list")).toBeTruthy();
    });
    const rows = screen.getAllByTestId("publish-incremental-queue-row");
    expect(rows).toHaveLength(2);
    expect(rows[0].textContent).toContain("301");
    expect(rows[0].textContent).toContain("Home Page");
    expect(rows[1].textContent).toContain("88");
    expect(rows[1].textContent).toContain("About");
    expect(screen.queryByTestId("publish-incremental-queue-empty")).toBeNull();
  });

  it("shows an empty state and no fake rows when the queue is empty", async () => {
    vi.mocked(publishApi.getIncrementalItems).mockResolvedValue({
      items: [],
      totalCount: 0,
    });
    renderWorkspace();
    await waitFor(() => {
      expect(screen.getByText("FTP-Prod")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("publish-incremental-preview-btn"));
    await waitFor(() => {
      expect(screen.getByTestId("publish-incremental-queue-empty")).toBeTruthy();
    });
    expect(screen.queryAllByTestId("publish-incremental-queue-row")).toHaveLength(0);
  });

  it("shows a visible error when the queue list fails to load", async () => {
    vi.mocked(publishApi.getIncrementalItems).mockRejectedValue({
      status: 500,
      statusText: "Server Error",
      body: "queue list failed",
    });
    renderWorkspace();
    await waitFor(() => {
      expect(screen.getByText("FTP-Prod")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("publish-incremental-preview-btn"));
    await waitFor(() => {
      expect(
        screen.getByTestId("publish-incremental-queue-error").textContent,
      ).toMatch(/queue list failed/i);
    });
    expect(screen.queryByTestId("publish-incremental-queue-list")).toBeNull();
    expect(screen.queryAllByTestId("publish-incremental-queue-row")).toHaveLength(0);
  });

  it("confirm removes that content id from the queue", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    vi.mocked(publishApi.getIncrementalItems).mockResolvedValue({
      items: [
        { id: "301", name: "Home" },
        { id: "88", title: "About" },
      ],
    });
    renderWorkspace();
    await waitFor(() => {
      expect(screen.getByText("FTP-Prod")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("publish-incremental-preview-btn"));
    await waitFor(() => {
      expect(screen.getAllByTestId("publish-incremental-queue-row")).toHaveLength(2);
    });
    const removes = screen.getAllByTestId("publish-incremental-queue-remove");
    fireEvent.click(removes[0]);
    await waitFor(() => {
      expect(publishApi.removeIncrementalQueueItem).toHaveBeenCalledWith(
        "MySite",
        "FTP-Prod",
        "301",
      );
    });
    await waitFor(() => {
      expect(screen.getAllByTestId("publish-incremental-queue-row")).toHaveLength(1);
    });
    expect(screen.getByTestId("publish-incremental-queue-row").textContent).toContain(
      "88",
    );
  });

  it("cancel leaves the queue unchanged", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(false);
    vi.mocked(publishApi.getIncrementalItems).mockResolvedValue({
      items: [{ id: "301", name: "Home" }],
    });
    renderWorkspace();
    await waitFor(() => {
      expect(screen.getByText("FTP-Prod")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("publish-incremental-preview-btn"));
    await waitFor(() => {
      expect(screen.getByTestId("publish-incremental-queue-remove")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("publish-incremental-queue-remove"));
    expect(publishApi.removeIncrementalQueueItem).not.toHaveBeenCalled();
    expect(screen.getAllByTestId("publish-incremental-queue-row")).toHaveLength(1);
  });

  it("shows 403 and 404 without removing the row", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    vi.mocked(publishApi.getIncrementalItems).mockResolvedValue({
      items: [{ id: "301", name: "Home" }],
    });
    vi.mocked(publishApi.removeIncrementalQueueItem).mockRejectedValueOnce({
      status: 403,
      statusText: "Forbidden",
      body: "no",
    });
    renderWorkspace();
    await waitFor(() => {
      expect(screen.getByText("FTP-Prod")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("publish-incremental-preview-btn"));
    await waitFor(() => {
      expect(screen.getByTestId("publish-incremental-queue-remove")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("publish-incremental-queue-remove"));
    await waitFor(() => {
      expect(
        screen.getByTestId("publish-incremental-queue-remove-error").textContent,
      ).toMatch(/not allowed/i);
    });
    expect(screen.getAllByTestId("publish-incremental-queue-row")).toHaveLength(1);

    vi.mocked(publishApi.removeIncrementalQueueItem).mockRejectedValueOnce({
      status: 404,
      statusText: "Not Found",
      body: "missing",
    });
    fireEvent.click(screen.getByTestId("publish-incremental-queue-remove"));
    await waitFor(() => {
      expect(
        screen.getByTestId("publish-incremental-queue-remove-error").textContent,
      ).toMatch(/not on the incremental queue/i);
    });
    expect(screen.getAllByTestId("publish-incremental-queue-row")).toHaveLength(1);
  });
});
