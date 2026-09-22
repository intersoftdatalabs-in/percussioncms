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

import { describe, it, expect, vi, beforeEach } from "vitest";
import {
  fetchItemScheduleDates,
  saveItemScheduleDates,
} from "@/api/publishing/itemScheduleDatesApi";
import * as client from "@/api/client";

vi.mock("@/api/client", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/api/client")>();
  return {
    ...actual,
    get: vi.fn(),
    post: vi.fn(),
  };
});

const getMock = vi.mocked(client.get);
const postMock = vi.mocked(client.post);

describe("itemScheduleDatesApi", () => {
  beforeEach(() => {
    getMock.mockReset();
    postMock.mockReset();
  });

  it("does not GET for a blank id", async () => {
    await expect(fetchItemScheduleDates("  ")).resolves.toEqual({
      itemId: "",
      startDate: "",
      endDate: "",
      comments: "",
    });
    expect(getMock).not.toHaveBeenCalled();
  });

  it("GETs getitemdates/{id} and unwraps ItemDates", async () => {
    getMock.mockResolvedValue({
      ItemDates: {
        itemId: "42",
        startDate: "09/18/2026 09:00 am",
        endDate: "09/19/2026 10:00 am",
      },
    });
    const dates = await fetchItemScheduleDates("42");
    expect(getMock).toHaveBeenCalledWith(
      "/services/itemmanagement/item/getitemdates/42",
    );
    expect(dates.startDate).toBe("09/18/2026 09:00 am");
  });

  it("POSTs ItemDates and throws on HTTP 400", async () => {
    postMock.mockRejectedValue({
      status: 400,
      statusText: "Bad Request",
      body: { message: "Invalid date range" },
    });
    await expect(
      saveItemScheduleDates({
        itemId: "42",
        startDate: "01/01/2000 09:00 am",
        endDate: "",
        comments: "",
      }),
    ).rejects.toMatchObject({ status: 400 });
    expect(postMock).toHaveBeenCalledWith(
      "/services/itemmanagement/item/setitemdates",
      {
        ItemDates: {
          itemId: "42",
          startDate: "01/01/2000 09:00 am",
          endDate: "",
          comments: "",
        },
      },
    );
  });

  it("throws on HTTP 403", async () => {
    postMock.mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: { message: "FORBIDDEN" },
    });
    await expect(
      saveItemScheduleDates({
        itemId: "42",
        startDate: "",
        endDate: "",
        comments: "",
      }),
    ).rejects.toMatchObject({ status: 403 });
  });

  it("throws on HTTP 409", async () => {
    postMock.mockRejectedValue({
      status: 409,
      statusText: "Conflict",
      body: { message: "editing" },
    });
    await expect(
      saveItemScheduleDates({
        itemId: "42",
        startDate: "",
        endDate: "",
        comments: "",
      }),
    ).rejects.toMatchObject({ status: 409 });
  });

  it("throws on HTTP 200 FORBIDDEN body", async () => {
    postMock.mockResolvedValue({ status: "FORBIDDEN" });
    await expect(
      saveItemScheduleDates({
        itemId: "42",
        startDate: "",
        endDate: "",
        comments: "",
      }),
    ).rejects.toThrow("FORBIDDEN");
  });
});
