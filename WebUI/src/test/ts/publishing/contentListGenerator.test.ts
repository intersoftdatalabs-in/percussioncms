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
import type { ContentListSummary } from "@/api/publishing/designApi";
import {
  CONTENT_LIST_GENERATOR_MAX_LENGTH,
  buildContentListGeneratorBody,
  contentListsAfterSuccessfulGenerator,
  validateContentListGenerator,
} from "@/publishing/contentListGenerator";

const source: ContentListSummary = {
  contentListId: "5",
  name: "NightCl",
  description: "Night notes",
  listType: "modern",
  generator: "sys_Search",
  url: "/Rhythmyx/contentlist",
  itemFilterId: "public",
  itemFilterName: "public",
};

describe("validateContentListGenerator", () => {
  it("rejects a blank generator", () => {
    expect(validateContentListGenerator("   ")).toEqual({
      ok: false,
      error: "Content list generator is required",
    });
  });

  it("rejects a generator longer than the column", () => {
    expect(
      validateContentListGenerator(
        "g".repeat(CONTENT_LIST_GENERATOR_MAX_LENGTH + 1),
      ),
    ).toEqual({
      ok: false,
      error: "Content list generator must be 256 characters or fewer",
    });
  });

  it("trims a usable generator", () => {
    expect(validateContentListGenerator("  sys_Changed  ")).toEqual({
      ok: true,
      generator: "sys_Changed",
    });
  });
});

describe("buildContentListGeneratorBody", () => {
  it("sends the generator only", () => {
    expect(buildContentListGeneratorBody("sys_Changed")).toEqual({
      generator: "sys_Changed",
    });
    const body = buildContentListGeneratorBody("sys_Changed");
    expect(body).not.toHaveProperty("name");
    expect(body).not.toHaveProperty("description");
    expect(body).not.toHaveProperty("listType");
    expect(body).not.toHaveProperty("url");
    expect(body).not.toHaveProperty("itemFilterId");
    expect(body).not.toHaveProperty("contentListId");
  });
});

describe("contentListsAfterSuccessfulGenerator", () => {
  const other: ContentListSummary = {
    contentListId: "6",
    name: "Other",
    description: "other notes",
    listType: "legacy",
    url: "/Rhythmyx/legacy",
  };

  it("uses the refreshed list when the reload succeeded", () => {
    const refreshed = [{ ...source, generator: "sys_Changed" }, other];
    expect(
      contentListsAfterSuccessfulGenerator(refreshed, "5", "sys_Changed", [
        source,
        other,
      ]),
    ).toBe(refreshed);
  });

  it("patches only the generator when the reload failed", () => {
    expect(
      contentListsAfterSuccessfulGenerator(null, "5", "sys_Changed", [
        source,
        other,
      ]),
    ).toEqual([{ ...source, generator: "sys_Changed" }, other]);
  });
});
