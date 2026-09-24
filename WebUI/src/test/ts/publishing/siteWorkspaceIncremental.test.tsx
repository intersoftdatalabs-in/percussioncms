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
  removeIncrementalQueueItem: vi.fn().mockResolvedValue(undefined),
  clearIncrementalQueue: vi.fn().mockResolvedValue(undefined),
  getIncrementalRelatedItems: vi.fn().mockResolvedValue({ items: [], totalCount: 0 }),
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
  fetchCurrentJobsForSite: vi.fn().mockResolvedValue([
    { jobId: 4242, status: "running", serverName: "FTP-Prod" },
  ]),
}));

const publishApi = await import("@/api/publishing/publishApi");
const statusApi = await import("@/api/publishing/statusApi");

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

describe("SiteWorkspace incremental site publish (#4614)", () => {
  afterEach(() => {
    vi.mocked(publishApi.incrementalPublishSite).mockReset();
    vi.mocked(publishApi.publishIncrementalWithApproval).mockReset();
    vi.mocked(window.confirm)?.mockRestore?.();
  });

  it("does not start incremental publish when confirm is cancelled", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(false);
    renderWorkspace();
    await waitFor(() => {
      expect(screen.getByText("FTP-Prod")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("publish-incremental-confirm"));
    expect(publishApi.incrementalPublishSite).not.toHaveBeenCalled();
  });

  it("starts incremental publish after confirm and shows job id plus status list", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    vi.mocked(publishApi.incrementalPublishSite).mockResolvedValue({
      SitePublishResponse: {
        status: "Queuing content",
        delivered: "0",
        failures: "0",
        jobid: 4242,
      },
    });
    renderWorkspace();
    await waitFor(() => {
      expect(screen.getByText("FTP-Prod")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("publish-incremental-confirm"));
    await waitFor(() => {
      expect(publishApi.incrementalPublishSite).toHaveBeenCalledWith(
        "MySite",
        "FTP-Prod",
      );
    });
    await waitFor(() => {
      const msg = screen.getByTestId("publish-action-message");
      expect(msg.textContent || "").toMatch(/4242/);
    });
    expect(statusApi.fetchCurrentJobsForSite).toHaveBeenCalled();
    expect(screen.getByTestId("publish-site-jobs")).toBeTruthy();
  });

  it("surfaces incremental publish errors after confirm", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    vi.mocked(publishApi.incrementalPublishSite).mockRejectedValue({
      status: 500,
      statusText: "Server Error",
      body: "queue failed",
    });
    renderWorkspace();
    await waitFor(() => {
      expect(screen.getByText("FTP-Prod")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("publish-incremental-confirm"));
    await waitFor(() => {
      expect(screen.getByRole("alert").textContent).toMatch(/queue failed/i);
    });
  });
});
