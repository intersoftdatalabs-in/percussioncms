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
  LOCATION_SCHEME_DESCRIPTION_MAX_LENGTH,
  buildLocationSchemeDescriptionBody,
  schemesAfterSuccessfulDescription,
  validateLocationSchemeDescription,
} from "@/publishing/locationSchemeDescription";

const source: LocationSchemeSummary = {
  schemeId: "11",
  name: "Article",
  description: "Pages",
  generator: "sys_Jexl",
  contentTypeId: 4,
  templateId: 8,
  contextId: "3",
  parameters: [{ name: "path", type: "String", value: "$sys.site.path", sequence: 0 }],
};

describe("validateLocationSchemeDescription", () => {
  it("allows a blank description so the stored text can be cleared", () => {
    expect(validateLocationSchemeDescription("   ")).toEqual({
      ok: true,
      description: "",
    });
  });

  it("rejects a description longer than the column", () => {
    expect(
      validateLocationSchemeDescription(
        "d".repeat(LOCATION_SCHEME_DESCRIPTION_MAX_LENGTH + 1),
      ),
    ).toEqual({
      ok: false,
      error: "Location scheme description must be 255 characters or fewer",
    });
  });

  it("trims a usable description", () => {
    expect(validateLocationSchemeDescription("  Night notes  ")).toEqual({
      ok: true,
      description: "Night notes",
    });
  });
});

describe("buildLocationSchemeDescriptionBody", () => {
  it("sends the description only, including a blank clear", () => {
    expect(buildLocationSchemeDescriptionBody("Night notes")).toEqual({
      description: "Night notes",
    });
    expect(buildLocationSchemeDescriptionBody("")).toEqual({ description: "" });
    const body = buildLocationSchemeDescriptionBody("Night notes");
    expect(body).not.toHaveProperty("name");
    expect(body).not.toHaveProperty("generator");
    expect(body).not.toHaveProperty("contentTypeId");
    expect(body).not.toHaveProperty("templateId");
    expect(body).not.toHaveProperty("contextId");
    expect(body).not.toHaveProperty("parameters");
    expect(body).not.toHaveProperty("schemeId");
  });
});

describe("schemesAfterSuccessfulDescription", () => {
  const other: LocationSchemeSummary = {
    schemeId: "12",
    name: "Brief",
    description: "Short",
    generator: "legacy-gen",
    contentTypeId: 5,
    templateId: 9,
    contextId: "3",
  };

  it("uses the refreshed list when the reload succeeded", () => {
    const refreshed = [{ ...source, description: "Night notes" }, other];
    expect(
      schemesAfterSuccessfulDescription(refreshed, "11", "Night notes", [
        source,
        other,
      ]),
    ).toBe(refreshed);
  });

  it("patches only the description when the reload failed", () => {
    expect(
      schemesAfterSuccessfulDescription(null, "11", "Night notes", [source, other]),
    ).toEqual([{ ...source, description: "Night notes" }, other]);
  });

  it("clears only the saved scheme when the reload failed", () => {
    expect(schemesAfterSuccessfulDescription(null, "11", "", [source, other])).toEqual([
      { ...source, description: "" },
      other,
    ]);
  });
});
