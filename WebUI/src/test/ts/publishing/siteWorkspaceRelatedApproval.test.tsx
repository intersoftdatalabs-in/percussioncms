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
  fetchPublishingLogs: vi.fn().mockResolvedValue([]),
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

async function openPreviewWithRelated(): Promise<void> {
  vi.mocked(publishApi.getIncrementalRelatedItems).mockResolvedValue({
    items: [
      { id: "301", name: "Home Page" },
      { id: "88", title: "About Us" },
    ],
  });
  renderWorkspace();
  await waitFor(() => {
    expect(screen.getByText("FTP-Prod")).toBeTruthy();
  });
  fireEvent.click(screen.getByTestId("publish-incremental-preview-btn"));
  await waitFor(() => {
    expect(screen.getByTestId("publish-related-approval")).toBeTruthy();
  });
}

describe("SiteWorkspace incremental approval i18n chrome (#7005)", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    vi.mocked(publishApi.getIncrementalItems).mockReset();
    vi.mocked(publishApi.getIncrementalItems).mockResolvedValue({
      items: [],
      totalCount: 0,
    });
    vi.mocked(publishApi.getIncrementalRelatedItems).mockReset();
    vi.mocked(publishApi.getIncrementalRelatedItems).mockResolvedValue({
      items: [],
      totalCount: 0,
    });
  });

  it("renders the preview button through the keyed Incremental preview label", async () => {
    await openPreviewWithRelated();
    expect(screen.getByTestId("publish-incremental-preview-btn").textContent).toBe(
      "Incremental preview",
    );
  });

  it("renders the related-approval heading through its key, not concatenation", async () => {
    await openPreviewWithRelated();
    const panel = screen.getByTestId("publish-related-approval");
    expect(panel.textContent).toContain("Publish Related Items for approval");
  });

  it("renders the keyed selected-count and updates it when a row is checked", async () => {
    await openPreviewWithRelated();
    const count = screen.getByTestId("publish-related-approval-count");
    expect(count.textContent).toBe("Selected for approval: 0 of 2");
    fireEvent.click(screen.getByLabelText("Approve related 301"));
    await waitFor(() => {
      expect(
        screen.getByTestId("publish-related-approval-count").textContent,
      ).toBe("Selected for approval: 1 of 2");
    });
  });

  it("keys the related table headings and screen-reader names", async () => {
    await openPreviewWithRelated();
    const panel = screen.getByTestId("publish-related-approval");
    expect(panel.textContent).toContain("Item");
    expect(panel.textContent).toContain("Id");
    expect(screen.getByLabelText("Select all related items")).toBeTruthy();
    expect(screen.getByLabelText("Approve related 88")).toBeTruthy();
  });

  it("labels the confirm button Incremental (with approval) when related items exist", async () => {
    await openPreviewWithRelated();
    expect(screen.getByTestId("publish-incremental-confirm").textContent).toBe(
      "Incremental (with approval)",
    );
  });
});
