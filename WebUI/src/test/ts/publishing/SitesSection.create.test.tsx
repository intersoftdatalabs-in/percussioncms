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
import { fetchSites } from "@/api/home/homeApi";
import { createSite } from "@/api/developer/sitesApi";
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
    createSite: vi.fn(),
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
const createSiteMock = createSite as ReturnType<typeof vi.fn>;

describe("SitesSection create site", () => {
  beforeEach(() => {
    fetchSitesMock.mockReset();
    createSiteMock.mockReset();
    fetchSitesMock.mockResolvedValue([
      { name: "Corporate Investments", id: "1", siteId: "1" },
    ]);
  });

  it("creates a valid site and opens its workspace", async () => {
    createSiteMock.mockResolvedValue({ name: "NightlySite" });
    fetchSitesMock
      .mockResolvedValueOnce([
        { name: "Corporate Investments", id: "1", siteId: "1" },
      ])
      .mockResolvedValueOnce([
        { name: "Corporate Investments", id: "1", siteId: "1" },
        { name: "NightlySite", id: "9", siteId: "9" },
      ]);
    render(<SitesSection />);
    await waitFor(() => {
      expect(screen.getByText("Corporate Investments")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("publish-sites-create"));
    fireEvent.change(screen.getByTestId("publish-site-create-name"), {
      target: { value: " NightlySite " },
    });
    fireEvent.click(screen.getByTestId("publish-site-create-save"));
    await waitFor(() => {
      expect(createSiteMock).toHaveBeenCalledWith({ name: "NightlySite" });
    });
    expect(await screen.findByTestId("publish-site-workspace")).toBeTruthy();
    expect(screen.queryByTestId("publish-section-sites")).toBeNull();
  });

  it("does not open a workspace when the new site has no numeric id", async () => {
    createSiteMock.mockResolvedValue({ name: "NightlySite" });
    fetchSitesMock.mockResolvedValue([
      { name: "NightlySite", id: "NightlySite", siteId: "NightlySite" },
    ]);
    render(<SitesSection />);
    await screen.findByTestId("publish-sites-create");
    fireEvent.click(screen.getByTestId("publish-sites-create"));
    fireEvent.change(screen.getByTestId("publish-site-create-name"), {
      target: { value: "NightlySite" },
    });
    fireEvent.click(screen.getByTestId("publish-site-create-save"));
    expect(await screen.findByRole("alert")).toBeTruthy();
    expect(screen.queryByTestId("publish-site-workspace")).toBeNull();
    expect(screen.getByTestId("publish-section-sites")).toBeTruthy();
  });

  it("cancel and a blank name create nothing", async () => {
    render(<SitesSection />);
    await screen.findByTestId("publish-sites-create");
    fireEvent.click(screen.getByTestId("publish-sites-create"));
    fireEvent.click(screen.getByTestId("publish-site-create-save"));
    expect(createSiteMock).not.toHaveBeenCalled();
    fireEvent.click(screen.getByTestId("publish-site-create-cancel"));
    expect(screen.queryByTestId("publish-site-create")).toBeNull();
    expect(screen.getByTestId("publish-section-sites")).toBeTruthy();
    expect(createSiteMock).not.toHaveBeenCalled();
  });

  it("does not post a name that is already on the list", async () => {
    render(<SitesSection />);
    await screen.findByText("Corporate Investments");
    fireEvent.click(screen.getByTestId("publish-sites-create"));
    fireEvent.change(screen.getByTestId("publish-site-create-name"), {
      target: { value: "corporate investments" },
    });
    fireEvent.click(screen.getByTestId("publish-site-create-save"));
    expect(createSiteMock).not.toHaveBeenCalled();
    expect(screen.getByTestId("publish-site-create-error")).toBeTruthy();
    expect(screen.queryByTestId("publish-site-workspace")).toBeNull();
    expect(screen.getByTestId("publish-section-sites")).toBeTruthy();
  });

  it.each([400, 403, 409])(
    "HTTP %s stays on the list and does not open a workspace",
    async (status) => {
      createSiteMock.mockRejectedValue({ status, statusText: "no", body: "" });
      render(<SitesSection />);
      await screen.findByTestId("publish-sites-create");
      fireEvent.click(screen.getByTestId("publish-sites-create"));
      fireEvent.change(screen.getByTestId("publish-site-create-name"), {
        target: { value: "NightlySite" },
      });
      fireEvent.click(screen.getByTestId("publish-site-create-save"));
      await waitFor(() => {
        expect(screen.getByTestId("publish-site-create-error")).toBeTruthy();
      });
      expect(screen.queryByTestId("publish-site-workspace")).toBeNull();
      expect(screen.getByTestId("publish-section-sites")).toBeTruthy();
    },
  );

  it("empty state opens the same create panel", async () => {
    fetchSitesMock.mockResolvedValue([]);
    render(<SitesSection />);
    const open = await screen.findByTestId("publish-empty-sites-create");
    fireEvent.click(open);
    expect(screen.getByTestId("publish-site-create")).toBeTruthy();
    expect(createSiteMock).not.toHaveBeenCalled();
  });
});
