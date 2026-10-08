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
  CONTENT_LIST_URL_MAX_LENGTH,
  buildContentListUrlBody,
  contentListsAfterSuccessfulUrl,
  validateContentListUrl,
} from "@/publishing/contentListUrl";

const source: ContentListSummary = {
  contentListId: "8",
  name: "LegacyCl",
  description: "legacy notes",
  listType: "legacy",
  url: "/Rhythmyx/legacyList",
  itemFilterId: "",
  itemFilterName: "",
};

describe("validateContentListUrl", () => {
  it("rejects a blank URL", () => {
    expect(validateContentListUrl("   ")).toEqual({
      ok: false,
      error: "Content list URL is required",
    });
  });

  it("rejects a URL longer than the column", () => {
    expect(
      validateContentListUrl("u".repeat(CONTENT_LIST_URL_MAX_LENGTH + 1)),
    ).toEqual({
      ok: false,
      error: "Content list URL must be 2100 characters or fewer",
    });
  });

  it("trims a usable URL", () => {
    expect(validateContentListUrl("  /Rhythmyx/night-next  ")).toEqual({
      ok: true,
      url: "/Rhythmyx/night-next",
    });
  });

  it("accepts a URL at the column length", () => {
    const url = "u".repeat(CONTENT_LIST_URL_MAX_LENGTH);
    expect(validateContentListUrl(url)).toEqual({ ok: true, url });
  });
});

describe("buildContentListUrlBody", () => {
  it("sends the URL only", () => {
    expect(buildContentListUrlBody("/Rhythmyx/night-next")).toEqual({
      url: "/Rhythmyx/night-next",
    });
    const body = buildContentListUrlBody("/Rhythmyx/night-next");
    expect(body).not.toHaveProperty("name");
    expect(body).not.toHaveProperty("description");
    expect(body).not.toHaveProperty("listType");
    expect(body).not.toHaveProperty("generator");
    expect(body).not.toHaveProperty("itemFilterId");
    expect(body).not.toHaveProperty("contentListId");
  });
});

describe("contentListsAfterSuccessfulUrl", () => {
  const other: ContentListSummary = {
    contentListId: "5",
    name: "NightCl",
    description: "Night notes",
    listType: "modern",
    generator: "sys_Search",
  };

  it("uses the refreshed list when the reload succeeded", () => {
    const refreshed = [{ ...source, url: "/Rhythmyx/night-next" }, other];
    expect(
      contentListsAfterSuccessfulUrl(refreshed, "8", "/Rhythmyx/night-next", [
        source,
        other,
      ]),
    ).toBe(refreshed);
  });

  it("patches only the URL when the reload failed", () => {
    expect(
      contentListsAfterSuccessfulUrl(null, "8", "/Rhythmyx/night-next", [
        source,
        other,
      ]),
    ).toEqual([{ ...source, url: "/Rhythmyx/night-next" }, other]);
  });
});
