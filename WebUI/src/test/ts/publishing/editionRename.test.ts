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
  EDITION_NAME_REQUIRED,
  EDITION_NAME_TOO_LONG,
  EDITION_PRIORITY_FORM_DEFAULT,
  editionRenameBody,
  editionRenameIsNameOnly,
  validateEditionName,
} from "@/publishing/editionRename";

const stored = {
  editionId: "12",
  name: "NightEd",
  siteId: "1",
  comment: "keep-me",
  priority: 5,
};

describe("validateEditionName", () => {
  it("rejects a blank name", () => {
    expect(validateEditionName("   ")).toEqual({
      ok: false,
      error: EDITION_NAME_REQUIRED,
    });
  });

  it("rejects a name longer than 100 characters", () => {
    expect(validateEditionName("N".repeat(101))).toEqual({
      ok: false,
      error: EDITION_NAME_TOO_LONG,
    });
  });

  it("trims a name that fits", () => {
    expect(validateEditionName("  NightEd  ")).toEqual({
      ok: true,
      name: "NightEd",
    });
  });
});

describe("edition rename body", () => {
  it("is name-only when comment and priority still match the row", () => {
    expect(editionRenameIsNameOnly(stored, "keep-me", 5)).toBe(true);
    expect(editionRenameBody(stored, "RenamedEd", "1")).toEqual({
      editionId: "12",
      name: "RenamedEd",
      siteId: "1",
    });
  });

  it("treats the form default priority as unchanged when the row has none", () => {
    expect(
      editionRenameIsNameOnly(
        { comment: undefined, priority: undefined },
        "",
        EDITION_PRIORITY_FORM_DEFAULT,
      ),
    ).toBe(true);
  });

  it("is not name-only when comment or priority was edited", () => {
    expect(editionRenameIsNameOnly(stored, "edited", 5)).toBe(false);
    expect(editionRenameIsNameOnly(stored, "keep-me", 4)).toBe(false);
  });
});
