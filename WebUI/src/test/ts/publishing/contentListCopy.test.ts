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
  CONTENT_LIST_NAME_MAX_LENGTH,
  CONTENT_LIST_NAME_REQUIRED,
  CONTENT_LIST_NAME_TOO_LONG,
  contentListsAfterSuccessfulCopy,
  suggestedContentListCopyName,
  validateContentListCopyName,
} from "@/publishing/contentListCopy";

const source = {
  contentListId: "5",
  name: "NightCl",
  listType: "modern",
};

describe("content list copy name", () => {
  it("rejects a blank name", () => {
    expect(validateContentListCopyName("  ")).toEqual({
      ok: false,
      error: CONTENT_LIST_NAME_REQUIRED,
    });
  });

  it("rejects a name longer than the column", () => {
    const tooLong = "N".repeat(CONTENT_LIST_NAME_MAX_LENGTH + 1);
    expect(validateContentListCopyName(tooLong)).toEqual({
      ok: false,
      error: CONTENT_LIST_NAME_TOO_LONG,
    });
  });

  it("trims an acceptable name", () => {
    expect(validateContentListCopyName("  Copied  ")).toEqual({
      ok: true,
      name: "Copied",
    });
  });

  it("suggests a suffixed name only when it fits", () => {
    expect(suggestedContentListCopyName("NightCl")).toBe("NightCl copy");
    expect(suggestedContentListCopyName("  ")).toBe("");
    expect(
      suggestedContentListCopyName("N".repeat(CONTENT_LIST_NAME_MAX_LENGTH)),
    ).toBe("");
  });
});

describe("contentListsAfterSuccessfulCopy", () => {
  const created = {
    contentListId: "9",
    name: "NightCl copy",
    listType: "modern",
  };

  it("appends the created row when refresh omits it and keeps the source", () => {
    expect(contentListsAfterSuccessfulCopy([source], created, [source])).toEqual([
      source,
      created,
    ]);
  });

  it("keeps the source when refresh fails", () => {
    expect(contentListsAfterSuccessfulCopy(null, created, [source])).toEqual([
      source,
      created,
    ]);
  });

  it("does not duplicate a row the refresh already returned", () => {
    const refreshed = [source, created];
    expect(contentListsAfterSuccessfulCopy(refreshed, created, [source])).toEqual(
      refreshed,
    );
  });

  it("restores the source if refresh dropped it", () => {
    expect(contentListsAfterSuccessfulCopy([created], created, [source])).toEqual([
      created,
      source,
    ]);
  });
});
