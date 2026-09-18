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
import { describe, expect, it, vi, beforeEach } from "vitest";
import { ItemScheduleDatesPanel } from "@/publishing/components/ItemScheduleDatesPanel";
import {
  fetchItemScheduleDates,
  saveItemScheduleDates,
} from "@/api/publishing/itemScheduleDatesApi";

vi.mock("@/api/publishing/itemScheduleDatesApi", () => ({
  fetchItemScheduleDates: vi.fn(),
  saveItemScheduleDates: vi.fn(),
}));

const fetchDates = vi.mocked(fetchItemScheduleDates);
const saveDates = vi.mocked(saveItemScheduleDates);

describe("ItemScheduleDatesPanel", () => {
  beforeEach(() => {
    fetchDates.mockReset();
    saveDates.mockReset();
  });

  it("needs an item id before load", async () => {
    render(<ItemScheduleDatesPanel />);
    expect(screen.getByTestId("item-schedule-dates")).toBeTruthy();
    fireEvent.click(screen.getByTestId("item-schedule-load"));
    await waitFor(() => {
      expect(screen.getByTestId("item-schedule-error")).toBeTruthy();
    });
    expect(fetchDates).not.toHaveBeenCalled();
  });

  it("loads dates for a deep-linked item id", async () => {
    fetchDates.mockResolvedValue({
      itemId: "42",
      startDate: "09/18/2026 09:00 am",
      endDate: "09/19/2026 10:00 am",
      comments: "note",
    });
    render(<ItemScheduleDatesPanel itemId="42" />);
    await waitFor(() => {
      expect(fetchDates).toHaveBeenCalledWith("42");
    });
    expect(screen.getByTestId("item-schedule-start")).toHaveValue(
      "2026-09-18T09:00",
    );
    expect(screen.getByTestId("item-schedule-comments")).toHaveValue("note");
  });

  it("saves converted dates", async () => {
    fetchDates.mockResolvedValue({
      itemId: "42",
      startDate: "09/18/2026 09:00 am",
      endDate: "09/19/2026 10:00 am",
      comments: "",
    });
    saveDates.mockResolvedValue();
    render(<ItemScheduleDatesPanel itemId="42" />);
    await waitFor(() => {
      expect(fetchDates).toHaveBeenCalled();
    });
    fireEvent.click(screen.getByTestId("item-schedule-save"));
    await waitFor(() => {
      expect(saveDates).toHaveBeenCalledWith({
        itemId: "42",
        startDate: "09/18/2026 09:00 am",
        endDate: "09/19/2026 10:00 am",
        comments: "",
      });
    });
    expect(screen.getByTestId("item-schedule-success")).toBeTruthy();
  });

  it("surfaces HTTP 400 as an invalid-dates error", async () => {
    fetchDates.mockResolvedValue({
      itemId: "42",
      startDate: "",
      endDate: "",
      comments: "",
    });
    saveDates.mockRejectedValue({
      status: 400,
      statusText: "Bad Request",
      body: { message: "Invalid date range" },
    });
    render(<ItemScheduleDatesPanel itemId="42" />);
    await waitFor(() => expect(fetchDates).toHaveBeenCalled());
    fireEvent.click(screen.getByTestId("item-schedule-save"));
    await waitFor(() => {
      expect(screen.getByTestId("item-schedule-error").textContent).toMatch(
        /Invalid date range/i,
      );
    });
    expect(screen.queryByTestId("item-schedule-success")).toBeNull();
  });

  it("surfaces HTTP 403 as forbidden, not success", async () => {
    fetchDates.mockResolvedValue({
      itemId: "42",
      startDate: "",
      endDate: "",
      comments: "",
    });
    saveDates.mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: { message: "User demo is editing this page." },
    });
    render(<ItemScheduleDatesPanel itemId="42" />);
    await waitFor(() => expect(fetchDates).toHaveBeenCalled());
    fireEvent.click(screen.getByTestId("item-schedule-save"));
    await waitFor(() => {
      expect(screen.getByTestId("item-schedule-error").textContent).toMatch(
        /editing this page|Forbidden|Publish Forbidden/i,
      );
    });
    expect(screen.queryByTestId("item-schedule-success")).toBeNull();
  });

  it("blocks inverted date ranges without calling save", async () => {
    fetchDates.mockResolvedValue({
      itemId: "42",
      startDate: "09/19/2026 09:00 am",
      endDate: "09/18/2026 09:00 am",
      comments: "",
    });
    render(<ItemScheduleDatesPanel itemId="42" />);
    await waitFor(() => expect(fetchDates).toHaveBeenCalled());
    fireEvent.click(screen.getByTestId("item-schedule-save"));
    await waitFor(() => {
      expect(screen.getByTestId("item-schedule-error")).toBeTruthy();
    });
    expect(saveDates).not.toHaveBeenCalled();
  });
});
