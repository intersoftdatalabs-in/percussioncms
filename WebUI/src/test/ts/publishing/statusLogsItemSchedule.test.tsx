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
import { StatusSection } from "@/publishing/sections/StatusSection";
import { LogsSection } from "@/publishing/sections/LogsSection";
import {
  fetchItemScheduleDates,
  saveItemScheduleDates,
} from "@/api/publishing/itemScheduleDatesApi";

vi.mock("@/api/publishing/itemScheduleDatesApi", () => ({
  fetchItemScheduleDates: vi.fn(),
  saveItemScheduleDates: vi.fn(),
}));

vi.mock("@/api/publishing/itemHistoryApi", () => ({
  fetchItemPublishingHistory: vi.fn().mockResolvedValue([]),
}));

vi.mock("@/api/publishing/publishApi", () => ({
  publishSite: vi.fn(),
  incrementalPublishSite: vi.fn(),
}));

vi.mock("@/api/publishing/serversApi", () => ({
  stopPublishing: vi.fn(),
}));

vi.mock("@/api/publishing/statusApi", () => ({
  fetchCurrentJobs: vi.fn().mockResolvedValue([]),
  fetchPublishingLogs: vi.fn().mockResolvedValue([]),
  fetchLogDetails: vi.fn(),
  purgePublishingLogs: vi.fn(),
}));

vi.mock("@/api/home/homeApi", () => ({
  fetchSites: vi.fn().mockResolvedValue([]),
}));

const fetchDates = vi.mocked(fetchItemScheduleDates);
const saveDates = vi.mocked(saveItemScheduleDates);

const DATES = {
  itemId: "42",
  startDate: "09/18/2026 09:00 am",
  endDate: "09/19/2026 10:00 am",
  comments: "",
};

describe("Status/Logs sections item schedule dates (ITM-05)", () => {
  beforeEach(() => {
    fetchDates.mockReset();
    saveDates.mockReset();
    fetchDates.mockResolvedValue({ ...DATES });
    saveDates.mockResolvedValue(undefined);
  });

  it("Status shows the schedule panel and deep-linked itemId loads dates", async () => {
    const onId = vi.fn();
    render(<StatusSection itemId="42" onItemIdChange={onId} />);
    await waitFor(() => {
      expect(fetchDates).toHaveBeenCalledWith("42");
    });
    expect(screen.getByTestId("item-schedule-dates")).toBeTruthy();
    expect(screen.getByTestId("item-schedule-start")).toHaveValue(
      "2026-09-18T09:00",
    );
  });

  it("Logs shows the schedule panel sharing the shell item id", async () => {
    render(<LogsSection itemId="42" onItemIdChange={vi.fn()} />);
    await waitFor(() => {
      expect(fetchDates).toHaveBeenCalledWith("42");
    });
    expect(screen.getByTestId("item-schedule-dates")).toBeTruthy();
    expect(screen.getByTestId("item-schedule-end")).toHaveValue(
      "2026-09-19T10:00",
    );
  });

  it("typing an id in the shell schedule lookup shares it with the shell", async () => {
    const onId = vi.fn();
    render(<StatusSection onItemIdChange={onId} />);
    fireEvent.change(screen.getByTestId("item-schedule-id"), {
      target: { value: "77" },
    });
    fireEvent.click(screen.getByTestId("item-schedule-load"));
    await waitFor(() => {
      expect(fetchDates).toHaveBeenCalledWith("77");
    });
    expect(onId).toHaveBeenCalledWith("77");
  });

  it("save success names success; 403 stays an error, not success", async () => {
    render(<StatusSection itemId="42" onItemIdChange={vi.fn()} />);
    await waitFor(() => {
      expect(fetchDates).toHaveBeenCalledWith("42");
    });
    fireEvent.click(screen.getByTestId("item-schedule-save"));
    await waitFor(() => {
      expect(screen.getByTestId("item-schedule-success")).toBeTruthy();
    });
    expect(saveDates).toHaveBeenCalledWith(
      expect.objectContaining({ itemId: "42" }),
    );

    saveDates.mockRejectedValueOnce({
      status: 403,
      message: "FORBIDDEN",
    });
    fireEvent.click(screen.getByTestId("item-schedule-save"));
    await waitFor(() => {
      expect(screen.getByTestId("item-schedule-error")).toBeTruthy();
    });
    expect(screen.queryByTestId("item-schedule-success")).toBeNull();
  });

  it("clear cancel writes nothing and leaves loaded dates", async () => {
    render(<LogsSection itemId="42" onItemIdChange={vi.fn()} />);
    await waitFor(() => {
      expect(fetchDates).toHaveBeenCalledWith("42");
    });
    fireEvent.click(screen.getByTestId("item-schedule-clear-submit"));
    expect(screen.getByTestId("item-schedule-clear-confirm")).toBeTruthy();
    fireEvent.click(screen.getByTestId("item-schedule-clear-cancel"));
    await waitFor(() => {
      expect(
        screen.queryByTestId("item-schedule-clear-confirm"),
      ).toBeNull();
    });
    expect(saveDates).not.toHaveBeenCalled();
    expect(screen.getByTestId("item-schedule-start")).toHaveValue(
      "2026-09-18T09:00",
    );
  });
});
