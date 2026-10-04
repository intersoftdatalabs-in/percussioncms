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
import type { LocationSchemeSummary } from "@/api/publishing/designApi";
import {
  LOCATION_SCHEME_NAME_MAX_LENGTH,
  LOCATION_SCHEME_NAME_REQUIRED,
  LOCATION_SCHEME_NAME_TOO_LONG,
  buildLocationSchemeCopyBody,
  schemesAfterSuccessfulCopy,
  suggestedLocationSchemeCopyName,
  validateLocationSchemeCopyName,
} from "@/publishing/locationSchemeCopy";

const source: LocationSchemeSummary = {
  schemeId: "11",
  name: "Article",
  description: "Pages",
  generator: "Java/global/percussion/contentassembler/sys_JexlAssemblyLocation",
  contentTypeId: 4,
  templateId: 8,
  contextId: "3",
  parameters: [
    { name: "path", type: "String", value: "$sys.site.path", sequence: 0 },
    { name: "prefix", type: "String", value: "news", sequence: 1 },
  ],
};

describe("validateLocationSchemeCopyName", () => {
  it("rejects a blank name", () => {
    expect(validateLocationSchemeCopyName("   ")).toEqual({
      ok: false,
      error: LOCATION_SCHEME_NAME_REQUIRED,
    });
  });

  it("rejects a name longer than the SCHEMENAME column", () => {
    const raw = "n".repeat(LOCATION_SCHEME_NAME_MAX_LENGTH + 1);
    expect(validateLocationSchemeCopyName(raw)).toEqual({
      ok: false,
      error: LOCATION_SCHEME_NAME_TOO_LONG,
    });
  });

  it("accepts a trimmed name at the column limit", () => {
    const raw = `  ${"n".repeat(LOCATION_SCHEME_NAME_MAX_LENGTH)}  `;
    expect(validateLocationSchemeCopyName(raw)).toEqual({
      ok: true,
      name: "n".repeat(LOCATION_SCHEME_NAME_MAX_LENGTH),
    });
  });
});

describe("buildLocationSchemeCopyBody", () => {
  it("copies path parameters and drops the source scheme id", () => {
    const body = buildLocationSchemeCopyBody(source, "Article copy", "3");
    expect(body.schemeId).toBeUndefined();
    expect(body).toEqual({
      name: "Article copy",
      generator: source.generator,
      description: "Pages",
      contentTypeId: 4,
      templateId: 8,
      contextId: "3",
      copy: true,
      parameters: [
        { name: "path", type: "String", value: "$sys.site.path", sequence: 0 },
        { name: "prefix", type: "String", value: "news", sequence: 1 },
      ],
    });
  });
});

describe("schemesAfterSuccessfulCopy", () => {
  it("keeps the refreshed list when it already contains the copy", () => {
    const created = { schemeId: "12", name: "Article copy" };
    const refreshed = [source, created];
    expect(schemesAfterSuccessfulCopy(refreshed, created, [source])).toEqual(
      refreshed,
    );
  });

  it("appends the created scheme when the refresh missed it", () => {
    const created = { schemeId: "12", name: "Article copy" };
    expect(schemesAfterSuccessfulCopy(null, created, [source])).toEqual([
      source,
      created,
    ]);
  });
});

describe("suggestedLocationSchemeCopyName", () => {
  it("suggests source plus copy when that fits", () => {
    expect(suggestedLocationSchemeCopyName("Article")).toBe("Article copy");
  });

  it("does not suggest a name that would be overlong", () => {
    expect(
      suggestedLocationSchemeCopyName("n".repeat(LOCATION_SCHEME_NAME_MAX_LENGTH)),
    ).toBe("");
  });
});
