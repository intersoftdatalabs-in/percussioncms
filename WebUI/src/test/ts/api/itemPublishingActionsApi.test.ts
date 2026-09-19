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

import { beforeEach, describe, expect, it, vi } from "vitest";

const get = vi.fn();

vi.mock("../../../main/ts/api/client", () => ({
  get: (...args: unknown[]) => get(...args),
}));

import { fetchItemPublishingActions } from "../../../main/ts/api/publishing/itemPublishingActionsApi";

describe("itemPublishingActionsApi", () => {
  beforeEach(() => {
    get.mockReset();
  });

  it("GETs the existing publishingActions endpoint and parses rows", async () => {
    get.mockResolvedValue([
      { name: "Publish", enabled: true },
      { name: "Stage", enabled: false },
    ]);
    const rows = await fetchItemPublishingActions("42");
    expect(get).toHaveBeenCalledTimes(1);
    expect(get).toHaveBeenCalledWith(
      expect.stringMatching(/\/sitemanage\/publish\/publishingActions\/42$/),
    );
    expect(rows).toEqual([
      { name: "Publish", enabled: true },
      { name: "Stage", enabled: false },
    ]);
  });

  it("returns empty for a blank item id without calling the server", async () => {
    await expect(fetchItemPublishingActions("  ")).resolves.toEqual([]);
    expect(get).not.toHaveBeenCalled();
  });

  it("propagates 403/404 ApiErrors to the menu mapper", async () => {
    get.mockRejectedValue({ status: 403, message: "denied" });
    await expect(fetchItemPublishingActions("42")).rejects.toMatchObject({
      status: 403,
    });
  });
});
