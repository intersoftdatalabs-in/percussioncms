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
  buildLocationSchemeRenameBody,
  schemesAfterSuccessfulRename,
  validateLocationSchemeRenameName,
} from "@/publishing/locationSchemeRename";

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
  ],
};

const other: LocationSchemeSummary = {
  schemeId: "12",
  name: "Brief",
  description: "Short",
  generator: "legacy-gen",
  contentTypeId: 5,
  templateId: 9,
  contextId: "3",
};

describe("validateLocationSchemeRenameName", () => {
  it("rejects a blank name", () => {
    expect(validateLocationSchemeRenameName("   ")).toEqual({
      ok: false,
      error: LOCATION_SCHEME_NAME_REQUIRED,
    });
  });

  it("rejects a name longer than the SCHEMENAME column", () => {
    expect(
      validateLocationSchemeRenameName("n".repeat(LOCATION_SCHEME_NAME_MAX_LENGTH + 1)),
    ).toEqual({
      ok: false,
      error: LOCATION_SCHEME_NAME_TOO_LONG,
    });
  });

  it("trims a usable name", () => {
    expect(validateLocationSchemeRenameName("  Night  ")).toEqual({
      ok: true,
      name: "Night",
    });
  });
});

describe("buildLocationSchemeRenameBody", () => {
  it("sends the name only", () => {
    const body = buildLocationSchemeRenameBody("Renamed");
    expect(body).toEqual({ name: "Renamed" });
    expect(body).not.toHaveProperty("generator");
    expect(body).not.toHaveProperty("description");
    expect(body).not.toHaveProperty("contentTypeId");
    expect(body).not.toHaveProperty("templateId");
    expect(body).not.toHaveProperty("contextId");
    expect(body).not.toHaveProperty("parameters");
    expect(body).not.toHaveProperty("schemeId");
  });
});

describe("schemesAfterSuccessfulRename", () => {
  it("uses the refreshed list when the reload succeeded", () => {
    const refreshed = [{ ...source, name: "Renamed" }, other];
    expect(schemesAfterSuccessfulRename(refreshed, "11", "Renamed", [source, other])).toBe(
      refreshed,
    );
  });

  it("patches only the name when the reload failed", () => {
    expect(schemesAfterSuccessfulRename(null, "11", "Renamed", [source, other])).toEqual([
      { ...source, name: "Renamed" },
      other,
    ]);
  });
});
