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

import { afterEach, describe, expect, it, vi } from "vitest";
import type { PSPathItem } from "../../../main/ts/api/contentExplorer/types";
import { EXPLORER_MSG } from "../../../main/ts/contentExplorer/messages";
import {
  datetimeLocalToServerDate,
  formatServerScheduleDate,
  getItemScheduleDates,
  isScheduleActionName,
  parseItemScheduleDates,
  parseServerScheduleDate,
  scheduleSelectedItem,
  serverDateToDatetimeLocal,
  setItemScheduleDates,
  validateScheduleDateRange,
} from "../../../main/ts/contentExplorer/itemScheduleDates";

afterEach(() => {
  vi.restoreAllMocks();
});

function item(overrides: Partial<PSPathItem> = {}): PSPathItem {
  return {
    name: "page",
    path: "/Sites/Demo/page",
    type: "percPage",
    id: "42",
    ...overrides,
  };
}

describe("parseItemScheduleDates", () => {
  it("unwraps JAXB ItemDates", () => {
    expect(
      parseItemScheduleDates(
        {
          ItemDates: {
            itemId: "42",
            startDate: "09/18/2026 09:00 am",
            endDate: "09/19/2026 10:00 am",
            comments: "note",
          },
        },
        "fallback",
      ),
    ).toEqual({
      itemId: "42",
      startDate: "09/18/2026 09:00 am",
      endDate: "09/19/2026 10:00 am",
      comments: "note",
    });
  });

  it("accepts an unwrapped body", () => {
    expect(
      parseItemScheduleDates({ itemId: "7", startDate: "", endDate: "" }, "x"),
    ).toEqual({
      itemId: "7",
      startDate: "",
      endDate: "",
      comments: "",
    });
  });
});

describe("server date conversion", () => {
  it("round-trips local MM/dd/yyyy hh:mm a through datetime-local", () => {
    const server = "09/18/2026 03:00 pm";
    const local = serverDateToDatetimeLocal(server);
    expect(local).toBe("2026-09-18T15:00");
    expect(datetimeLocalToServerDate(local)).toBe(server);
    const parsed = parseServerScheduleDate(server);
    expect(parsed).not.toBeNull();
    expect(formatServerScheduleDate(parsed as Date)).toBe(server);
  });

  it("maps midnight and noon", () => {
    expect(serverDateToDatetimeLocal("01/02/2026 12:00 am")).toBe(
      "2026-01-02T00:00",
    );
    expect(datetimeLocalToServerDate("2026-01-02T00:00")).toBe(
      "01/02/2026 12:00 am",
    );
    expect(datetimeLocalToServerDate("2026-01-02T12:00")).toBe(
      "01/02/2026 12:00 pm",
    );
  });

  it("validates same and inverted ranges", () => {
    expect(
      validateScheduleDateRange(
        "09/18/2026 09:00 am",
        "09/18/2026 09:00 am",
      ),
    ).toBe(EXPLORER_MSG.SCHEDULE_DATES_SAME);
    expect(
      validateScheduleDateRange(
        "09/19/2026 09:00 am",
        "09/18/2026 09:00 am",
      ),
    ).toBe(EXPLORER_MSG.SCHEDULE_DATE_RANGE);
    expect(
      validateScheduleDateRange(
        "09/18/2026 09:00 am",
        "09/19/2026 09:00 am",
      ),
    ).toBeNull();
    expect(validateScheduleDateRange("", "")).toBeNull();
  });
});

describe("get/set schedule dates", () => {
  it("GETs getitemdates/{id}", async () => {
    vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response(
        JSON.stringify({
          ItemDates: { itemId: "42", startDate: "09/18/2026 09:00 am" },
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );
    const dates = await getItemScheduleDates("42");
    expect(dates.startDate).toBe("09/18/2026 09:00 am");
    expect(String(global.fetch.mock.calls[0]?.[0] ?? "")).toContain(
      "itemmanagement/item/getitemdates/42",
    );
  });

  it("POSTs ItemDates envelope and throws on HTTP 200 FORBIDDEN", async () => {
    vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response(JSON.stringify({ status: "FORBIDDEN" }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    await expect(
      setItemScheduleDates({
        itemId: "42",
        startDate: "",
        endDate: "",
        comments: "",
      }),
    ).rejects.toThrow("FORBIDDEN");
    const req = global.fetch.mock.calls[0];
    expect(String(req?.[0] ?? "")).toContain(
      "itemmanagement/item/setitemdates",
    );
    expect(req?.[1]?.method).toBe("POST");
    expect(JSON.parse(String(req?.[1]?.body ?? "{}"))).toEqual({
      ItemDates: {
        itemId: "42",
        startDate: "",
        endDate: "",
        comments: "",
      },
    });
  });

  it("throws on wrapped BADCONFIG", async () => {
    vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response(
        JSON.stringify({
          SitePublishResponse: {
            status: "BADCONFIG",
            warningMessage: "Could not connect to publishing server",
          },
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );
    await expect(
      setItemScheduleDates({
        itemId: "42",
        startDate: "",
        endDate: "",
        comments: "",
      }),
    ).rejects.toThrow("Could not connect to publishing server");
  });

  it("throws on HTTP 200 INVALID", async () => {
    vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response(JSON.stringify({ status: "INVALID" }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    await expect(
      setItemScheduleDates({
        itemId: "42",
        startDate: "09/18/2026 09:00 am",
        endDate: "",
        comments: "",
      }),
    ).rejects.toThrow("INVALID");
  });

  it("scheduleSelectedItem returns false for folders", async () => {
    const fetchSpy = vi.spyOn(global, "fetch");
    expect(
      await scheduleSelectedItem(
        item({
          id: "1",
          name: "Sites",
          path: "/Sites",
          type: "folder",
          leaf: false,
        }),
        { itemId: "1", startDate: "", endDate: "", comments: "" },
      ),
    ).toBe(false);
    expect(fetchSpy).not.toHaveBeenCalled();
  });

  it("recognizes Schedule action names", () => {
    expect(isScheduleActionName("Schedule")).toBe(true);
    expect(isScheduleActionName("schedule dates")).toBe(true);
    expect(isScheduleActionName("Publish_Now")).toBe(false);
  });
});
