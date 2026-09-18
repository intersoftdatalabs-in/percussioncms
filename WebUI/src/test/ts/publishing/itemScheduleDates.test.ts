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

import { describe, expect, it } from "vitest";
import {
  itemScheduleShellHref,
  spaItemScheduleHref,
} from "@/publishing/itemScheduleDates";

describe("item schedule hrefs", () => {
  it("builds sites-workspace deep links with siteId and itemId", () => {
    expect(
      itemScheduleShellHref({ siteId: "9", itemId: "42" }),
    ).toBe("/cm/app/publish/sites?siteId=9&itemId=42");
    expect(spaItemScheduleHref({ siteId: "9", itemId: "42" })).toContain(
      "entry=publish",
    );
    expect(spaItemScheduleHref({ siteId: "9", itemId: "42" })).toContain(
      "section=sites",
    );
    expect(spaItemScheduleHref({ siteId: "9", itemId: "42" })).toContain(
      "itemId=42",
    );
  });

  it("drops unsafe ids", () => {
    expect(itemScheduleShellHref({ itemId: "<script>" })).toBe(
      "/cm/app/publish/sites",
    );
  });
});
