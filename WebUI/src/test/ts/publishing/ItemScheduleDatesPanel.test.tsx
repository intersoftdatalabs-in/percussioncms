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
    expect(fetchDates).toHaveBeenCalledTimes(2);
    expect(screen.getByTestId("item-schedule-comments")).toHaveValue("");
  });

  it("keeps the comment after save because getitemdates omits it", async () => {
    fetchDates
      .mockResolvedValueOnce({
        itemId: "42",
        startDate: "09/18/2026 09:00 am",
        endDate: "09/19/2026 10:00 am",
        comments: "",
      })
      .mockResolvedValueOnce({
        itemId: "42",
        startDate: "09/18/2026 09:00 am",
        endDate: "09/19/2026 10:00 am",
        comments: "",
      });
    saveDates.mockResolvedValue();
    render(<ItemScheduleDatesPanel itemId="42" />);
    await waitFor(() => expect(fetchDates).toHaveBeenCalled());
    fireEvent.change(screen.getByTestId("item-schedule-comments"), {
      target: { value: "go live" },
    });
    fireEvent.click(screen.getByTestId("item-schedule-save"));
    await waitFor(() => {
      expect(screen.getByTestId("item-schedule-success")).toBeTruthy();
    });
    expect(screen.getByTestId("item-schedule-comments")).toHaveValue("go live");
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

  it("surfaces HTTP 409 checkout conflict, not success", async () => {
    fetchDates.mockResolvedValue({
      itemId: "42",
      startDate: "",
      endDate: "",
      comments: "",
    });
    saveDates.mockRejectedValue({
      status: 409,
      statusText: "Conflict",
      body: { message: "User other is editing this page." },
    });
    render(<ItemScheduleDatesPanel itemId="42" />);
    await waitFor(() => expect(fetchDates).toHaveBeenCalled());
    fireEvent.click(screen.getByTestId("item-schedule-save"));
    await waitFor(() => {
      expect(screen.getByTestId("item-schedule-error").textContent).toMatch(
        /editing this page|Conflict|HTTP 409/i,
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

  it("asks for an item before opening the clear confirm", async () => {
    render(<ItemScheduleDatesPanel />);
    fireEvent.click(screen.getByTestId("item-schedule-clear-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("item-schedule-error")).toBeTruthy();
    });
    expect(screen.queryByTestId("item-schedule-clear-confirm")).toBeNull();
    expect(saveDates).not.toHaveBeenCalled();
  });

  it("cancelling the clear confirm writes nothing", async () => {
    fetchDates.mockResolvedValue({
      itemId: "42",
      startDate: "09/18/2026 09:00 am",
      endDate: "09/19/2026 10:00 am",
      comments: "",
    });
    render(<ItemScheduleDatesPanel itemId="42" />);
    await waitFor(() => expect(fetchDates).toHaveBeenCalled());
    fireEvent.click(screen.getByTestId("item-schedule-clear-submit"));
    expect(screen.getByTestId("item-schedule-clear-confirm")).toBeTruthy();
    fireEvent.click(screen.getByTestId("item-schedule-clear-cancel"));
    await waitFor(() => {
      expect(screen.queryByTestId("item-schedule-clear-confirm")).toBeNull();
    });
    expect(saveDates).not.toHaveBeenCalled();
    expect(screen.queryByTestId("item-schedule-success")).toBeNull();
  });

  it("confirming clear posts empty dates and shows the empty schedule", async () => {
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
    render(<ItemScheduleDatesPanel itemId="42" />);
    await waitFor(() => expect(fetchDates).toHaveBeenCalled());
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
    expect(screen.queryByTestId("item-schedule-clear-confirm")).toBeNull();
    expect(screen.getByTestId("item-schedule-start")).toHaveValue("");
    expect(screen.getByTestId("item-schedule-end")).toHaveValue("");
    expect(screen.queryByTestId("item-schedule-error")).toBeNull();
  });

  it("a failed clear stays on the confirm as an error, not success", async () => {
    fetchDates.mockResolvedValue({
      itemId: "42",
      startDate: "09/18/2026 09:00 am",
      endDate: "09/19/2026 10:00 am",
      comments: "",
    });
    saveDates.mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: { message: "User demo is editing this page." },
    });
    render(<ItemScheduleDatesPanel itemId="42" />);
    await waitFor(() => expect(fetchDates).toHaveBeenCalled());
    fireEvent.click(screen.getByTestId("item-schedule-clear-submit"));
    fireEvent.click(screen.getByTestId("item-schedule-clear-yes"));
    await waitFor(() => {
      expect(screen.getByTestId("item-schedule-error")).toBeTruthy();
    });
    expect(
      screen.getByTestId("item-schedule-clear-confirm"),
    ).toBeTruthy();
    expect(screen.queryByTestId("item-schedule-success")).toBeNull();
  });

  it("maps application-level BADCONFIG on clear to the invalid-dates error", async () => {
    fetchDates.mockResolvedValue({
      itemId: "42",
      startDate: "09/18/2026 09:00 am",
      endDate: "09/19/2026 10:00 am",
      comments: "",
    });
    saveDates.mockRejectedValue(new Error("BADCONFIG: schedule rejected"));
    render(<ItemScheduleDatesPanel itemId="42" />);
    await waitFor(() => expect(fetchDates).toHaveBeenCalled());
    fireEvent.click(screen.getByTestId("item-schedule-clear-submit"));
    fireEvent.click(screen.getByTestId("item-schedule-clear-yes"));
    await waitFor(() => {
      expect(screen.getByTestId("item-schedule-error").textContent).toMatch(
        /Invalid schedule dates/i,
      );
    });
    expect(screen.queryByTestId("item-schedule-success")).toBeNull();
  });

  it("saves one date set to several ids and reports every row", async () => {
    fetchDates.mockImplementation(async (id: string) => ({
      itemId: id,
      startDate: "",
      endDate: "",
      comments: "",
    }));
    saveDates.mockResolvedValue();
    render(<ItemScheduleDatesPanel />);
    fireEvent.change(screen.getByTestId("item-schedule-id"), {
      target: { value: "42, 43" },
    });
    fireEvent.change(screen.getByTestId("item-schedule-start"), {
      target: { value: "2026-09-18T09:00" },
    });
    fireEvent.click(screen.getByTestId("item-schedule-save"));
    await waitFor(() => {
      expect(saveDates).toHaveBeenCalledTimes(2);
    });
    expect(saveDates).toHaveBeenNthCalledWith(1, {
      itemId: "42",
      startDate: "09/18/2026 09:00 am",
      endDate: "",
      comments: "",
    });
    expect(saveDates).toHaveBeenNthCalledWith(2, {
      itemId: "43",
      startDate: "09/18/2026 09:00 am",
      endDate: "",
      comments: "",
    });
    await waitFor(() => {
      expect(screen.getByTestId("item-schedule-success").textContent).toMatch(
        /all 2 items/i,
      );
    });
    expect(
      screen.getByTestId("item-schedule-batch-report"),
    ).toBeTruthy();
    expect(
      screen.getByTestId("item-schedule-batch-item-42").textContent,
    ).toMatch(/saved/i);
    expect(
      screen.getByTestId("item-schedule-batch-item-43").textContent,
    ).toMatch(/saved/i);
  });

  it("a per-item save failure keeps the other row and names the failure", async () => {
    fetchDates.mockImplementation(async (id: string) => ({
      itemId: id,
      startDate: "",
      endDate: "",
      comments: "",
    }));
    saveDates.mockImplementation(async (dated: { itemId: string }) => {
      if (dated.itemId === "43") {
        throw {
          status: 403,
          statusText: "Forbidden",
          body: { message: "User demo is editing this page." },
        };
      }
    });
    render(<ItemScheduleDatesPanel />);
    fireEvent.change(screen.getByTestId("item-schedule-id"), {
      target: { value: "42 43" },
    });
    fireEvent.change(screen.getByTestId("item-schedule-start"), {
      target: { value: "2026-09-18T09:00" },
    });
    fireEvent.click(screen.getByTestId("item-schedule-save"));
    await waitFor(() => {
      expect(screen.getByTestId("item-schedule-error").textContent).toMatch(
        /Saved 1 of 2/i,
      );
    });
    expect(screen.getByTestId("item-schedule-error").textContent).toMatch(
      /43 \(User demo is editing this page\.\)/,
    );
    expect(
      screen.getByTestId("item-schedule-batch-item-42").textContent,
    ).toMatch(/saved/i);
    expect(screen.queryByTestId("item-schedule-success")).toBeNull();
  });

  it("loading several ids fills from the first success and names load failures", async () => {
    fetchDates.mockImplementation(async (id: string) => {
      if (id === "42") {
        throw { status: 404, statusText: "Not Found", body: {} };
      }
      return {
        itemId: id,
        startDate: "09/18/2026 09:00 am",
        endDate: "",
        comments: "",
      };
    });
    render(<ItemScheduleDatesPanel />);
    fireEvent.change(screen.getByTestId("item-schedule-id"), {
      target: { value: "42,43" },
    });
    fireEvent.click(screen.getByTestId("item-schedule-load"));
    await waitFor(() => {
      expect(screen.getByTestId("item-schedule-error").textContent).toMatch(
        /Loaded 1 of 2/i,
      );
    });
    expect(screen.getByTestId("item-schedule-start")).toHaveValue(
      "2026-09-18T09:00",
    );
    expect(
      screen.getByTestId("item-schedule-batch-item-42").textContent,
    ).toMatch(/42: Load failed/);
  });

  it("clearing several ids reports per-item results", async () => {
    fetchDates.mockImplementation(async (id: string) => ({
      itemId: id,
      startDate: "",
      endDate: "",
      comments: "",
    }));
    saveDates.mockResolvedValue();
    render(<ItemScheduleDatesPanel />);
    fireEvent.change(screen.getByTestId("item-schedule-id"), {
      target: { value: "42, 43" },
    });
    fireEvent.click(screen.getByTestId("item-schedule-clear-submit"));
    expect(
      screen.getByTestId("item-schedule-clear-confirm").textContent,
    ).toMatch(/these 2 items/i);
    fireEvent.click(screen.getByTestId("item-schedule-clear-yes"));
    await waitFor(() => {
      expect(saveDates).toHaveBeenCalledTimes(2);
    });
    await waitFor(() => {
      expect(screen.getByTestId("item-schedule-success").textContent).toMatch(
        /cleared for all 2 items/i,
      );
    });
    expect(
      screen.getByTestId("item-schedule-batch-report"),
    ).toBeTruthy();
  });

  it("looking up several ids keeps the list and does not reselect the shell item", async () => {
    fetchDates.mockImplementation(async (id: string) => ({
      itemId: id,
      startDate: "",
      endDate: "",
      comments: "",
    }));
    const onItemIdChange = vi.fn();
    render(<ItemScheduleDatesPanel onItemIdChange={onItemIdChange} />);
    fireEvent.change(screen.getByTestId("item-schedule-id"), {
      target: { value: "42, 43" },
    });
    fireEvent.click(screen.getByTestId("item-schedule-load"));
    await waitFor(() => {
      expect(fetchDates).toHaveBeenCalledTimes(2);
    });
    expect(onItemIdChange).not.toHaveBeenCalled();
    expect(screen.getByTestId("item-schedule-id")).toHaveValue("42, 43");
  });

  it("looking up one id still reselects the shell item", async () => {
    fetchDates.mockResolvedValue({
      itemId: "42",
      startDate: "",
      endDate: "",
      comments: "",
    });
    const onItemIdChange = vi.fn();
    render(<ItemScheduleDatesPanel onItemIdChange={onItemIdChange} />);
    fireEvent.change(screen.getByTestId("item-schedule-id"), {
      target: { value: "42" },
    });
    fireEvent.click(screen.getByTestId("item-schedule-load"));
    await waitFor(() => {
      expect(fetchDates).toHaveBeenCalledWith("42");
    });
    expect(onItemIdChange).toHaveBeenCalledWith("42");
  });

  it("retrying a partial batch save re-saves only the failed ids", async () => {
    fetchDates.mockImplementation(async (id: string) => ({
      itemId: id,
      startDate: "",
      endDate: "",
      comments: "",
    }));
    let calls = 0;
    saveDates.mockImplementation(async (dated: { itemId: string }) => {
      calls += 1;
      if (dated.itemId === "43" && calls <= 2) {
        throw {
          status: 403,
          statusText: "Forbidden",
          body: { message: "User demo is editing this page." },
        };
      }
    });
    render(<ItemScheduleDatesPanel />);
    fireEvent.change(screen.getByTestId("item-schedule-id"), {
      target: { value: "42 43" },
    });
    fireEvent.change(screen.getByTestId("item-schedule-start"), {
      target: { value: "2026-09-18T09:00" },
    });
    fireEvent.click(screen.getByTestId("item-schedule-save"));
    await waitFor(() => {
      expect(screen.getByTestId("item-schedule-retry")).toBeTruthy();
    });
    expect(screen.getByTestId("item-schedule-error").textContent).toMatch(
      /Saved 1 of 2/i,
    );
    fireEvent.click(screen.getByTestId("item-schedule-retry"));
    await waitFor(() => {
      expect(saveDates).toHaveBeenCalledTimes(3);
    });
    expect(saveDates).toHaveBeenNthCalledWith(3, {
      itemId: "43",
      startDate: "09/18/2026 09:00 am",
      endDate: "",
      comments: "",
    });
    await waitFor(() => {
      expect(screen.getByTestId("item-schedule-success").textContent).toMatch(
        /all 1 items/i,
      );
    });
    expect(screen.queryByTestId("item-schedule-retry")).toBeNull();
    expect(
      screen.getByTestId("item-schedule-batch-item-42").textContent,
    ).toMatch(/saved/i);
    expect(
      screen.getByTestId("item-schedule-batch-item-43").textContent,
    ).toMatch(/saved/i);
  });

  it("retrying a partial batch clear re-clears only the failed ids", async () => {
    fetchDates.mockImplementation(async (id: string) => ({
      itemId: id,
      startDate: "",
      endDate: "",
      comments: "",
    }));
    let calls = 0;
    let failedOnce = false;
    saveDates.mockImplementation(async (dated: { itemId: string }) => {
      calls += 1;
      if (dated.itemId === "43" && !failedOnce) {
        failedOnce = true;
        throw {
          status: 409,
          statusText: "Conflict",
          body: { message: "User other is editing this page." },
        };
      }
    });
    render(<ItemScheduleDatesPanel />);
    fireEvent.change(screen.getByTestId("item-schedule-id"), {
      target: { value: "42, 43" },
    });
    fireEvent.click(screen.getByTestId("item-schedule-clear-submit"));
    fireEvent.click(screen.getByTestId("item-schedule-clear-yes"));
    await waitFor(() => {
      expect(screen.getByTestId("item-schedule-error").textContent).toMatch(
        /Cleared 1 of 2/i,
      );
    });
    expect(screen.getByTestId("item-schedule-retry")).toBeTruthy();
    fireEvent.click(screen.getByTestId("item-schedule-retry"));
    await waitFor(() => {
      expect(saveDates).toHaveBeenCalledTimes(3);
    });
    expect(saveDates).toHaveBeenNthCalledWith(3, {
      itemId: "43",
      startDate: "",
      endDate: "",
      comments: "",
    });
    await waitFor(() => {
      expect(screen.getByTestId("item-schedule-success").textContent).toMatch(
        /cleared for all 1 items/i,
      );
    });
    expect(screen.queryByTestId("item-schedule-retry")).toBeNull();
    expect(screen.queryByTestId("item-schedule-clear-confirm")).toBeNull();
  });

  it("a failed single save offers retry for that item", async () => {
    fetchDates.mockResolvedValue({
      itemId: "42",
      startDate: "",
      endDate: "",
      comments: "",
    });
    saveDates
      .mockRejectedValueOnce({
        status: 403,
        statusText: "Forbidden",
        body: { message: "User demo is editing this page." },
      })
      .mockResolvedValueOnce(undefined);
    render(<ItemScheduleDatesPanel itemId="42" />);
    await waitFor(() => expect(fetchDates).toHaveBeenCalled());
    fireEvent.click(screen.getByTestId("item-schedule-save"));
    await waitFor(() => {
      expect(screen.getByTestId("item-schedule-error")).toBeTruthy();
    });
    expect(screen.getByTestId("item-schedule-retry")).toBeTruthy();
    fireEvent.click(screen.getByTestId("item-schedule-retry"));
    await waitFor(() => {
      expect(saveDates).toHaveBeenCalledTimes(2);
    });
    await waitFor(() => {
      expect(screen.getByTestId("item-schedule-success")).toBeTruthy();
    });
    expect(screen.queryByTestId("item-schedule-retry")).toBeNull();
  });

  it("retrying a partial batch load reloads only the failed ids", async () => {
    let failedOnce = false;
    fetchDates.mockImplementation(async (id: string) => {
      if (id === "43" && !failedOnce) {
        failedOnce = true;
        throw { status: 404, statusText: "Not Found", body: {} };
      }
      return {
        itemId: id,
        startDate: "09/18/2026 09:00 am",
        endDate: "",
        comments: "",
      };
    });
    render(<ItemScheduleDatesPanel />);
    fireEvent.change(screen.getByTestId("item-schedule-id"), {
      target: { value: "42, 43" },
    });
    fireEvent.click(screen.getByTestId("item-schedule-load"));
    await waitFor(() => {
      expect(screen.getByTestId("item-schedule-error").textContent).toMatch(
        /Loaded 1 of 2/i,
      );
    });
    expect(
      screen.getByTestId("item-schedule-batch-item-43").textContent,
    ).toMatch(/43: Load failed/);
    expect(screen.getByTestId("item-schedule-retry")).toBeTruthy();
    fireEvent.click(screen.getByTestId("item-schedule-retry"));
    await waitFor(() => {
      expect(fetchDates).toHaveBeenCalledTimes(3);
    });
    expect(fetchDates).toHaveBeenNthCalledWith(3, "43");
    await waitFor(() => {
      expect(screen.getByTestId("item-schedule-success").textContent).toMatch(
        /loaded for all 1 items/i,
      );
    });
    expect(screen.queryByTestId("item-schedule-retry")).toBeNull();
    expect(
      screen.getByTestId("item-schedule-batch-item-42").textContent,
    ).toMatch(/saved/i);
    expect(
      screen.getByTestId("item-schedule-batch-item-43").textContent,
    ).toMatch(/saved/i);
  });

  it("retrying a partial batch load keeps the user's form values", async () => {
    let failedOnce = false;
    fetchDates.mockImplementation(async (id: string) => {
      if (id === "43" && !failedOnce) {
        failedOnce = true;
        throw { status: 404, statusText: "Not Found", body: {} };
      }
      return {
        itemId: id,
        startDate: "09/18/2026 09:00 am",
        endDate: "",
        comments: id === "42" ? "from-42" : "from-43",
      };
    });
    render(<ItemScheduleDatesPanel />);
    fireEvent.change(screen.getByTestId("item-schedule-id"), {
      target: { value: "42, 43" },
    });
    fireEvent.click(screen.getByTestId("item-schedule-load"));
    await waitFor(() => {
      expect(screen.getByTestId("item-schedule-error").textContent).toMatch(
        /Loaded 1 of 2/i,
      );
    });
    expect(screen.getByTestId("item-schedule-comments")).toHaveValue("from-42");
    // The user edits the form before retrying the failed item.
    fireEvent.change(screen.getByTestId("item-schedule-comments"), {
      target: { value: "user-edit" },
    });
    expect(screen.getByTestId("item-schedule-retry")).toBeTruthy();
    fireEvent.click(screen.getByTestId("item-schedule-retry"));
    await waitFor(() => {
      expect(screen.getByTestId("item-schedule-success").textContent).toMatch(
        /loaded for all 1 items/i,
      );
    });
    expect(screen.queryByTestId("item-schedule-retry")).toBeNull();
    // The retried item's stored dates must not clobber the form.
    expect(screen.getByTestId("item-schedule-comments")).toHaveValue(
      "user-edit",
    );
    expect(screen.getByTestId("item-schedule-start")).toHaveValue(
      "2026-09-18T09:00",
    );
  });

  it("a full batch load reports success with no retry", async () => {
    fetchDates.mockImplementation(async (id: string) => ({
      itemId: id,
      startDate: "09/18/2026 09:00 am",
      endDate: "",
      comments: "",
    }));
    render(<ItemScheduleDatesPanel />);
    fireEvent.change(screen.getByTestId("item-schedule-id"), {
      target: { value: "42, 43" },
    });
    fireEvent.click(screen.getByTestId("item-schedule-load"));
    await waitFor(() => {
      expect(screen.getByTestId("item-schedule-success").textContent).toMatch(
        /loaded for all 2 items/i,
      );
    });
    expect(screen.queryByTestId("item-schedule-retry")).toBeNull();
    expect(
      screen.getByTestId("item-schedule-batch-item-42").textContent,
    ).toMatch(/saved/i);
    expect(
      screen.getByTestId("item-schedule-batch-item-43").textContent,
    ).toMatch(/saved/i);
  });

  it("a failed single load offers no retry", async () => {
    fetchDates.mockRejectedValue({
      status: 404,
      statusText: "Not Found",
      body: {},
    });
    render(<ItemScheduleDatesPanel itemId="42" />);
    await waitFor(() => {
      expect(screen.getByTestId("item-schedule-error")).toBeTruthy();
    });
    expect(screen.queryByTestId("item-schedule-retry")).toBeNull();
    expect(screen.queryByTestId("item-schedule-batch-report")).toBeNull();
  });
});
