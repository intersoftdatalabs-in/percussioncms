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
  LOCATION_SCHEME_GENERATOR_MAX_LENGTH,
  buildLocationSchemeGeneratorBody,
  schemesAfterSuccessfulGenerator,
  validateLocationSchemeGenerator,
} from "@/publishing/locationSchemeGenerator";

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

describe("validateLocationSchemeGenerator", () => {
  it("rejects a blank generator", () => {
    expect(validateLocationSchemeGenerator("   ")).toEqual({
      ok: false,
      error: "Location scheme generator is required",
    });
  });

  it("rejects a generator longer than the column", () => {
    expect(
      validateLocationSchemeGenerator(
        "g".repeat(LOCATION_SCHEME_GENERATOR_MAX_LENGTH + 1),
      ),
    ).toEqual({
      ok: false,
      error: "Location scheme generator must be 255 characters or fewer",
    });
  });

  it("trims a usable generator", () => {
    expect(validateLocationSchemeGenerator("  sys_Changed  ")).toEqual({
      ok: true,
      generator: "sys_Changed",
    });
  });
});

describe("buildLocationSchemeGeneratorBody", () => {
  it("sends the generator only", () => {
    expect(buildLocationSchemeGeneratorBody("sys_Changed")).toEqual({
      generator: "sys_Changed",
    });
    const body = buildLocationSchemeGeneratorBody("sys_Changed");
    expect(body).not.toHaveProperty("name");
    expect(body).not.toHaveProperty("description");
    expect(body).not.toHaveProperty("contentTypeId");
    expect(body).not.toHaveProperty("templateId");
    expect(body).not.toHaveProperty("contextId");
    expect(body).not.toHaveProperty("parameters");
    expect(body).not.toHaveProperty("schemeId");
  });
});

describe("schemesAfterSuccessfulGenerator", () => {
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
    const refreshed = [{ ...source, generator: "sys_Changed" }, other];
    expect(
      schemesAfterSuccessfulGenerator(refreshed, "11", "sys_Changed", [
        source,
        other,
      ]),
    ).toBe(refreshed);
  });

  it("patches only the generator when the reload failed", () => {
    expect(
      schemesAfterSuccessfulGenerator(null, "11", "sys_Changed", [source, other]),
    ).toEqual([{ ...source, generator: "sys_Changed" }, other]);
  });
});
