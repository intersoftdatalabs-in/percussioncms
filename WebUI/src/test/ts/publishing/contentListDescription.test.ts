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
  CONTENT_LIST_DESCRIPTION_MAX_LENGTH,
  buildContentListDescriptionBody,
  contentListsAfterSuccessfulDescription,
  validateContentListDescription,
} from "@/publishing/contentListDescription";

const source: ContentListSummary = {
  contentListId: "5",
  name: "NightCl",
  description: "old notes",
  listType: "modern",
  generator: "sys_Search",
  itemFilterId: "public",
  itemFilterName: "public",
};

describe("validateContentListDescription", () => {
  it("treats blank text as a clear", () => {
    expect(validateContentListDescription("   ")).toEqual({
      ok: true,
      description: "",
    });
  });

  it("rejects a description longer than the column", () => {
    expect(
      validateContentListDescription(
        "d".repeat(CONTENT_LIST_DESCRIPTION_MAX_LENGTH + 1),
      ),
    ).toEqual({
      ok: false,
      error: "Content list description must be 255 characters or fewer",
    });
  });

  it("trims a usable description", () => {
    expect(validateContentListDescription("  Night notes  ")).toEqual({
      ok: true,
      description: "Night notes",
    });
  });
});

describe("buildContentListDescriptionBody", () => {
  it("sends the description only, including an empty clear", () => {
    expect(buildContentListDescriptionBody("Night notes")).toEqual({
      description: "Night notes",
    });
    expect(buildContentListDescriptionBody("")).toEqual({ description: "" });
    const body = buildContentListDescriptionBody("Night notes");
    expect(body).not.toHaveProperty("name");
    expect(body).not.toHaveProperty("listType");
    expect(body).not.toHaveProperty("generator");
    expect(body).not.toHaveProperty("url");
    expect(body).not.toHaveProperty("itemFilterId");
    expect(body).not.toHaveProperty("contentListId");
  });
});

describe("contentListsAfterSuccessfulDescription", () => {
  const other: ContentListSummary = {
    contentListId: "6",
    name: "Other",
    description: "other notes",
    listType: "legacy",
    url: "/Rhythmyx/legacy",
  };

  it("uses the refreshed list when the reload succeeded", () => {
    const refreshed = [{ ...source, description: "Night notes" }, other];
    expect(
      contentListsAfterSuccessfulDescription(refreshed, "5", "Night notes", [
        source,
        other,
      ]),
    ).toBe(refreshed);
  });

  it("patches only the description when the reload failed", () => {
    expect(
      contentListsAfterSuccessfulDescription(null, "5", "Night notes", [
        source,
        other,
      ]),
    ).toEqual([{ ...source, description: "Night notes" }, other]);
  });

  it("clears only the description when the reload failed", () => {
    expect(
      contentListsAfterSuccessfulDescription(null, "5", "", [source, other]),
    ).toEqual([{ ...source, description: "" }, other]);
  });
});
