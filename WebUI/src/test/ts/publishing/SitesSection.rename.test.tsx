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
import { renameSite } from "@/api/developer/sitesApi";
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
    renameSite: vi.fn(),
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
const renameSiteMock = renameSite as ReturnType<typeof vi.fn>;

async function openNightly(): Promise<void> {
  render(<SitesSection />);
  fireEvent.click(await screen.findByTestId("publish-site-card-NightlySite"));
  await screen.findByTestId("publish-site-workspace");
  fireEvent.click(screen.getByTestId("publish-site-rename-open"));
}

describe("SitesSection rename the open site", () => {
  beforeEach(() => {
    fetchSitesMock.mockReset();
    renameSiteMock.mockReset();
    fetchSitesMock.mockResolvedValue([
      { name: "NightlySite", id: "1", siteId: "1" },
      { name: "OtherSite", id: "2", siteId: "2" },
    ]);
  });

  it("renames on confirm and the list shows the new name after back", async () => {
    renameSiteMock.mockResolvedValue({ name: "RenamedSite" });
    await openNightly();
    fireEvent.change(screen.getByTestId("publish-site-rename-name"), {
      target: { value: " RenamedSite " },
    });
    fireEvent.click(screen.getByTestId("publish-site-rename-save"));
    await waitFor(() => {
      expect(renameSiteMock).toHaveBeenCalledWith("NightlySite", "RenamedSite");
    });
    expect(screen.getByTestId("publish-site-title").textContent).toBe("RenamedSite");
    expect(screen.queryByTestId("publish-site-rename")).toBeNull();
    fireEvent.click(screen.getByRole("button", { name: "Back" }));
    expect(await screen.findByTestId("publish-site-card-RenamedSite")).toBeTruthy();
    expect(screen.queryByTestId("publish-site-card-NightlySite")).toBeNull();
  });

  it("cancel, blank, unchanged, and duplicate names do not write", async () => {
    await openNightly();
    fireEvent.click(screen.getByTestId("publish-site-rename-cancel"));
    expect(renameSiteMock).not.toHaveBeenCalled();
    expect(screen.getByTestId("publish-site-title").textContent).toBe("NightlySite");

    fireEvent.click(screen.getByTestId("publish-site-rename-open"));
    fireEvent.change(screen.getByTestId("publish-site-rename-name"), {
      target: { value: "   " },
    });
    expect(screen.getByTestId("publish-site-rename-save")).toBeDisabled();
    fireEvent.change(screen.getByTestId("publish-site-rename-name"), {
      target: { value: "nightlysite" },
    });
    expect(screen.getByTestId("publish-site-rename-save")).toBeDisabled();
    fireEvent.change(screen.getByTestId("publish-site-rename-name"), {
      target: { value: "othersite" },
    });
    fireEvent.click(screen.getByTestId("publish-site-rename-save"));
    expect(renameSiteMock).not.toHaveBeenCalled();
    expect(screen.getByTestId("publish-site-rename-error")).toBeTruthy();
    expect(screen.getByTestId("publish-site-title").textContent).toBe("NightlySite");
    expect(screen.getByTestId("publish-site-workspace")).toBeTruthy();
  });

  it.each([400, 403, 409])("HTTP %s stays on the workspace", async (status) => {
    renameSiteMock.mockRejectedValue({ status, statusText: "no", body: "" });
    await openNightly();
    fireEvent.change(screen.getByTestId("publish-site-rename-name"), {
      target: { value: "FreshName" },
    });
    fireEvent.click(screen.getByTestId("publish-site-rename-save"));
    await waitFor(() => {
      expect(screen.getByTestId("publish-site-rename-error")).toBeTruthy();
    });
    expect(screen.getByTestId("publish-site-title").textContent).toBe("NightlySite");
    expect(screen.getByTestId("publish-site-workspace")).toBeTruthy();
    expect(screen.queryByTestId("publish-section-sites")).toBeNull();
  });
});
