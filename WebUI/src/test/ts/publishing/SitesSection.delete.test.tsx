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
import { deleteSite } from "@/api/developer/sitesApi";
import { fetchSites } from "@/api/home/homeApi";
import { SitesSection } from "@/publishing/sections/SitesSection";

vi.mock("@/api/home/homeApi", () => ({
  fetchSites: vi.fn(),
}));

vi.mock("@/api/developer/sitesApi", async () => {
  const actual = await vi.importActual<typeof import("@/api/developer/sitesApi")>(
    "@/api/developer/sitesApi",
  );
  return {
    ...actual,
    deleteSite: vi.fn(),
  };
});

vi.mock("@/api/publishing/publishApi", () => ({
  publishSite: vi.fn(),
  incrementalPublishSite: vi.fn(),
  publishIncrementalWithApproval: vi.fn(),
  getIncrementalItems: vi.fn().mockResolvedValue({ items: [], totalCount: 0 }),
  getIncrementalRelatedItems: vi.fn().mockResolvedValue({ items: [], totalCount: 0 }),
  removeIncrementalQueueItem: vi.fn(),
  clearIncrementalQueue: vi.fn(),
  approveIncrementalQueueItem: vi.fn(),
  unapproveIncrementalQueueItem: vi.fn(),
}));

vi.mock("@/api/publishing/serversApi", () => ({
  listServers: vi.fn().mockResolvedValue([]),
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

const fetchSitesMock = fetchSites as ReturnType<typeof vi.fn>;
const deleteSiteMock = deleteSite as ReturnType<typeof vi.fn>;

async function openNightly(): Promise<void> {
  render(<SitesSection />);
  fireEvent.click(await screen.findByTestId("publish-site-card-NightlySite"));
  await screen.findByTestId("publish-site-workspace");
  fireEvent.click(screen.getByTestId("publish-site-delete-open"));
}

describe("SitesSection delete the open site", () => {
  beforeEach(() => {
    fetchSitesMock.mockReset();
    deleteSiteMock.mockReset();
    fetchSitesMock.mockResolvedValue([
      { name: "NightlySite", id: "1", siteId: "1" },
      { name: "OtherSite", id: "2", siteId: "2" },
    ]);
  });

  it("deletes on confirm, leaves the workspace, and refreshes the list", async () => {
    deleteSiteMock.mockResolvedValue(undefined);
    fetchSitesMock
      .mockResolvedValueOnce([
        { name: "NightlySite", id: "1", siteId: "1" },
        { name: "OtherSite", id: "2", siteId: "2" },
      ])
      .mockResolvedValueOnce([{ name: "OtherSite", id: "2", siteId: "2" }]);
    await openNightly();
    fireEvent.click(screen.getByTestId("publish-site-delete-confirm"));
    await waitFor(() => {
      expect(deleteSiteMock).toHaveBeenCalledWith("NightlySite");
    });
    expect(await screen.findByTestId("publish-section-sites")).toBeTruthy();
    expect(screen.queryByTestId("publish-site-workspace")).toBeNull();
    await waitFor(() => {
      expect(screen.queryByTestId("publish-site-card-NightlySite")).toBeNull();
    });
    expect(screen.getByTestId("publish-site-card-OtherSite")).toBeTruthy();
  });

  it("cancel does not delete and stays on the workspace", async () => {
    await openNightly();
    fireEvent.click(screen.getByTestId("publish-site-delete-cancel"));
    expect(deleteSiteMock).not.toHaveBeenCalled();
    expect(screen.queryByTestId("publish-site-delete")).toBeNull();
    expect(screen.getByTestId("publish-site-title").textContent).toBe("NightlySite");
    expect(screen.getByTestId("publish-site-workspace")).toBeTruthy();
  });

  it.each([403, 404, 409])("HTTP %s stays on the workspace", async (status) => {
    deleteSiteMock.mockRejectedValue({ status, statusText: "no", body: "" });
    await openNightly();
    fireEvent.click(screen.getByTestId("publish-site-delete-confirm"));
    await waitFor(() => {
      expect(screen.getByTestId("publish-site-delete-error")).toBeTruthy();
    });
    expect(screen.getByTestId("publish-site-title").textContent).toBe("NightlySite");
    expect(screen.getByTestId("publish-site-workspace")).toBeTruthy();
    expect(screen.queryByTestId("publish-section-sites")).toBeNull();
  });
});
