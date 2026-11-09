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
import {
  fetchItemScheduleDates,
  saveItemScheduleDates,
} from "@/api/publishing/itemScheduleDatesApi";

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

vi.mock("@/api/publishing/itemScheduleDatesApi", () => ({
  fetchItemScheduleDates: vi.fn(),
  saveItemScheduleDates: vi.fn(),
}));

const fetchDates = vi.mocked(fetchItemScheduleDates);
const saveDates = vi.mocked(saveItemScheduleDates);

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

describe("SiteWorkspace item schedule dates (#5929)", () => {
  beforeEach(() => {
    fetchDates.mockReset();
    saveDates.mockReset();
  });

  it("loads schedule dates for the selected workspace item", async () => {
    fetchDates.mockResolvedValue({
      itemId: "42",
      startDate: "09/18/2026 09:00 am",
      endDate: "09/19/2026 10:00 am",
      comments: "",
    });
    renderWorkspace("42");
    expect(screen.getByTestId("item-schedule-dates")).toBeTruthy();
    await waitFor(() => {
      expect(fetchDates).toHaveBeenCalledWith("42");
    });
    expect(screen.getByTestId("item-schedule-start")).toHaveValue(
      "2026-09-18T09:00",
    );
  });

  it("looks up a typed item id through the existing dates route", async () => {
    fetchDates.mockResolvedValue({
      itemId: "43",
      startDate: "",
      endDate: "",
      comments: "",
    });
    renderWorkspace();
    fireEvent.change(screen.getByTestId("item-schedule-id"), {
      target: { value: "43" },
    });
    fireEvent.click(screen.getByTestId("item-schedule-load"));
    await waitFor(() => {
      expect(fetchDates).toHaveBeenCalledWith("43");
    });
  });

  it("clears the schedule on the selected item through setitemdates", async () => {
    fetchDates
      .mockResolvedValueOnce({
        itemId: "42",
        startDate: "09/18/2026 09:00 am",
        endDate: "09/19/2026 10:00 am",
        comments: "",
      })
      .mockResolvedValueOnce({
        itemId: "42",
        startDate: "",
        endDate: "",
        comments: "",
      });
    saveDates.mockResolvedValue();
    renderWorkspace("42");
    await waitFor(() => expect(fetchDates).toHaveBeenCalledWith("42"));
    fireEvent.click(screen.getByTestId("item-schedule-clear-submit"));
    fireEvent.click(screen.getByTestId("item-schedule-clear-yes"));
    await waitFor(() => {
      expect(saveDates).toHaveBeenCalledWith({
        itemId: "42",
        startDate: "",
        endDate: "",
        comments: "",
      });
    });
    await waitFor(() => {
      expect(screen.getByTestId("item-schedule-success").textContent).toMatch(
        /cleared/i,
      );
    });
  });
});
