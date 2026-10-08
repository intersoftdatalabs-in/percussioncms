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
import { wrapLocationScheme, type LocationSchemeSummary } from "@/api/publishing/designApi";
import {
  LOCATION_SCHEME_PARAMETER_NAME_REQUIRED,
  LOCATION_SCHEME_PARAMETER_NOT_ON_SCHEME,
  LOCATION_SCHEME_PARAMETER_TYPE_REQUIRED,
  LOCATION_SCHEME_PARAMETER_TYPE_TOO_LONG,
  buildLocationSchemeParameterTypeBody,
  locationSchemeParameterTypeChoices,
  schemesAfterSuccessfulParameterType,
  validateLocationSchemeParameterType,
} from "@/publishing/locationSchemeParameterType";

const path = { name: "path", type: "String", value: "$sys.site.path", sequence: 0 };
const suffix = { name: "suffix", type: "BackendColumn", value: "article", sequence: 1 };

const source: LocationSchemeSummary = {
  schemeId: "11",
  name: "Article",
  generator: "sys_Jexl",
  description: "Pages",
  contentTypeId: 4,
  templateId: 8,
  parameters: [path, suffix],
};

describe("validateLocationSchemeParameterType", () => {
  it("rejects a blank type and does not clear the stored type", () => {
    expect(validateLocationSchemeParameterType("suffix", "   ", source.parameters)).toEqual({
      ok: false,
      error: LOCATION_SCHEME_PARAMETER_TYPE_REQUIRED,
    });
  });

  it("rejects a type longer than the column", () => {
    expect(validateLocationSchemeParameterType("suffix", "T".repeat(51), source.parameters)).toEqual(
      {
        ok: false,
        error: LOCATION_SCHEME_PARAMETER_TYPE_TOO_LONG,
      },
    );
  });

  it("rejects a blank name and a name that is not stored", () => {
    expect(validateLocationSchemeParameterType("  ", "String", source.parameters)).toEqual({
      ok: false,
      error: LOCATION_SCHEME_PARAMETER_NAME_REQUIRED,
    });
    expect(validateLocationSchemeParameterType("missing", "String", source.parameters)).toEqual({
      ok: false,
      error: LOCATION_SCHEME_PARAMETER_NOT_ON_SCHEME,
    });
  });

  it("trims the new type and keeps the stored name, value, and sequence", () => {
    expect(validateLocationSchemeParameterType("  suffix  ", "  String  ", source.parameters)).toEqual(
      {
        ok: true,
        parameter: {
          name: "suffix",
          type: "String",
          value: "article",
          sequence: 1,
        },
      },
    );
  });
});

describe("locationSchemeParameterTypeChoices", () => {
  it("keeps an unknown stored type selectable", () => {
    expect(locationSchemeParameterTypeChoices("Number")).toEqual([
      "Number",
      "String",
      "BackendColumn",
    ]);
    expect(locationSchemeParameterTypeChoices("BackendColumn")).toEqual([
      "String",
      "BackendColumn",
    ]);
  });
});

describe("buildLocationSchemeParameterTypeBody", () => {
  it("sends only the one parameter type to change", () => {
    const body = buildLocationSchemeParameterTypeBody({
      name: "suffix",
      type: "String",
      value: "article",
      sequence: 1,
    });
    expect(body).toEqual({
      updateParameterType: true,
      parameters: [{ name: "suffix", type: "String", value: "article" }],
    });
    expect(body.name).toBeUndefined();
    expect(body.generator).toBeUndefined();
    expect(body.description).toBeUndefined();
    expect(body.contentTypeId).toBeUndefined();
    expect(body.templateId).toBeUndefined();
    expect(body.addParameter).toBeUndefined();
    expect(body.removeParameter).toBeUndefined();
    expect(body.updateParameterValue).toBeUndefined();
    expect(body.parameters?.[0].sequence).toBeUndefined();
    expect(wrapLocationScheme(body).locationScheme).toEqual({
      name: undefined,
      description: undefined,
      contextId: undefined,
      generator: undefined,
      contentTypeId: undefined,
      templateId: undefined,
      updateParameterType: true,
      parameters: {
        schemeParameter: [{ name: "suffix", type: "String", value: "article" }],
      },
    });
  });
});

describe("schemesAfterSuccessfulParameterType", () => {
  const other: LocationSchemeSummary = {
    schemeId: "12",
    name: "Brief",
    generator: "legacy-gen",
    description: "Short",
  };
  const updated = { name: "suffix", type: "String", value: "article", sequence: 1 };

  it("keeps a refreshed row that already has the new type", () => {
    const refreshed = [
      { ...source, parameters: [path, { ...suffix, type: "String" }] },
      other,
    ];
    expect(schemesAfterSuccessfulParameterType(refreshed, "11", updated, [source, other])).toEqual(
      refreshed,
    );
  });

  it("replaces only that type when the refresh still has the previous one", () => {
    const next = schemesAfterSuccessfulParameterType([source, other], "11", updated, [
      source,
      other,
    ]);
    expect(next[0].parameters).toEqual([path, { ...suffix, type: "String" }]);
    expect(next[0].name).toBe("Article");
    expect(next[0].generator).toBe("sys_Jexl");
    expect(next[0].description).toBe("Pages");
    expect(next[0].contentTypeId).toBe(4);
    expect(next[0].templateId).toBe(8);
    expect(next[1]).toEqual(other);
  });

  it("patches the previous list when the refresh omits parameters", () => {
    const refreshed = [{ ...source, parameters: undefined }, other];
    const next = schemesAfterSuccessfulParameterType(refreshed, "11", updated, [source, other]);
    expect(next[0].parameters).toEqual([path, { ...suffix, type: "String" }]);
    expect(next[1]).toEqual(other);
  });
});
