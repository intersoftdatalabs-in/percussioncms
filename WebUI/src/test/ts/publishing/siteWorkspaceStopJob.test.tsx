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
  stopPublishing: vi.fn().mockResolvedValue({}),
}));

vi.mock("@/api/publishing/statusApi", () => ({
  fetchCurrentJobsForSite: vi.fn().mockResolvedValue([
    { jobId: 4615, status: "running", serverName: "FTP-Prod" },
    { jobId: 8, status: "Completed", serverName: "FTP-Prod" },
  ]),
}));

const serversApi = await import("@/api/publishing/serversApi");

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

describe("SiteWorkspace stop publish job (#4615)", () => {
  afterEach(() => {
    vi.mocked(serversApi.stopPublishing).mockClear();
    vi.mocked(window.confirm)?.mockRestore?.();
  });

  it("offers Stop only on running jobs", async () => {
    renderWorkspace();
    await waitFor(() => {
      expect(screen.getByTestId("publish-stop-job-4615")).toBeTruthy();
    });
    expect(screen.queryByTestId("publish-stop-job-8")).toBeNull();
  });

  it("does not stop when confirm is cancelled", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(false);
    renderWorkspace();
    await waitFor(() => {
      expect(screen.getByTestId("publish-stop-job-4615")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("publish-stop-job-4615"));
    expect(serversApi.stopPublishing).not.toHaveBeenCalled();
  });

  it("stops after confirm", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    renderWorkspace();
    await waitFor(() => {
      expect(screen.getByTestId("publish-stop-job-4615")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("publish-stop-job-4615"));
    await waitFor(() => {
      expect(serversApi.stopPublishing).toHaveBeenCalledWith(4615);
    });
  });

  it("maps HTTP 409 as error, not success", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    vi.mocked(serversApi.stopPublishing).mockRejectedValueOnce({
      status: 409,
      statusText: "Conflict",
      body: {},
    });
    renderWorkspace();
    await waitFor(() => {
      expect(screen.getByTestId("publish-stop-job-4615")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("publish-stop-job-4615"));
    await waitFor(() => {
      expect(screen.getByRole("alert").textContent).toMatch(
        /cannot be stopped|409|Conflict/i,
      );
    });
  });
});
