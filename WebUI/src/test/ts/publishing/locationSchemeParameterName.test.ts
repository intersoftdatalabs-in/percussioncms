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
  LOCATION_SCHEME_PARAMETER_EXISTS,
  LOCATION_SCHEME_PARAMETER_NAME_REQUIRED,
  LOCATION_SCHEME_PARAMETER_NAME_TOO_LONG,
  LOCATION_SCHEME_PARAMETER_NOT_ON_SCHEME,
  buildLocationSchemeParameterNameBody,
  schemesAfterSuccessfulParameterName,
  validateLocationSchemeParameterName,
} from "@/publishing/locationSchemeParameterName";

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

describe("validateLocationSchemeParameterName", () => {
  it("rejects a blank new name and does not clear the stored name", () => {
    expect(validateLocationSchemeParameterName("suffix", "   ", source.parameters)).toEqual({
      ok: false,
      error: LOCATION_SCHEME_PARAMETER_NAME_REQUIRED,
    });
  });

  it("rejects an overlong new name and a blank stored name", () => {
    expect(validateLocationSchemeParameterName("suffix", "n".repeat(51), source.parameters)).toEqual(
      {
        ok: false,
        error: LOCATION_SCHEME_PARAMETER_NAME_TOO_LONG,
      },
    );
    expect(validateLocationSchemeParameterName("  ", "fileSuffix", source.parameters)).toEqual({
      ok: false,
      error: LOCATION_SCHEME_PARAMETER_NAME_REQUIRED,
    });
  });

  it("rejects a duplicate of another parameter and a name that is not stored", () => {
    expect(validateLocationSchemeParameterName("suffix", " path ", source.parameters)).toEqual({
      ok: false,
      error: LOCATION_SCHEME_PARAMETER_EXISTS,
    });
    expect(validateLocationSchemeParameterName("missing", "fileSuffix", source.parameters)).toEqual(
      {
        ok: false,
        error: LOCATION_SCHEME_PARAMETER_NOT_ON_SCHEME,
      },
    );
  });

  it("keeps the stored type, value, and sequence and allows the same name", () => {
    expect(
      validateLocationSchemeParameterName("  suffix  ", "  fileSuffix  ", source.parameters),
    ).toEqual({
      ok: true,
      parameter: {
        name: "suffix",
        newName: "fileSuffix",
        type: "BackendColumn",
        value: "article",
        sequence: 1,
      },
    });
    expect(validateLocationSchemeParameterName("suffix", "suffix", source.parameters).ok).toBe(
      true,
    );
  });
});

describe("buildLocationSchemeParameterNameBody", () => {
  it("sends only the stored name and the new name", () => {
    const body = buildLocationSchemeParameterNameBody({
      name: "suffix",
      newName: "fileSuffix",
      type: "BackendColumn",
      value: "article",
      sequence: 1,
    });
    expect(body).toEqual({
      updateParameterName: true,
      parameters: [{ name: "suffix", newName: "fileSuffix" }],
    });
    expect(body.name).toBeUndefined();
    expect(body.generator).toBeUndefined();
    expect(body.description).toBeUndefined();
    expect(body.contentTypeId).toBeUndefined();
    expect(body.templateId).toBeUndefined();
    expect(body.addParameter).toBeUndefined();
    expect(body.removeParameter).toBeUndefined();
    expect(body.updateParameterValue).toBeUndefined();
    expect(body.updateParameterType).toBeUndefined();
    expect(body.updateParameterSequence).toBeUndefined();
    expect(body.parameters?.[0].type).toBeUndefined();
    expect(body.parameters?.[0].value).toBeUndefined();
    expect(body.parameters?.[0].sequence).toBeUndefined();
    expect(wrapLocationScheme(body).locationScheme).toEqual({
      name: undefined,
      description: undefined,
      contextId: undefined,
      generator: undefined,
      contentTypeId: undefined,
      templateId: undefined,
      updateParameterName: true,
      parameters: {
        schemeParameter: [{ name: "suffix", newName: "fileSuffix" }],
      },
    });
  });
});

describe("schemesAfterSuccessfulParameterName", () => {
  const other: LocationSchemeSummary = {
    schemeId: "12",
    name: "Brief",
    generator: "legacy-gen",
    description: "Short",
  };
  const updated = {
    name: "suffix",
    newName: "fileSuffix",
    type: "BackendColumn",
    value: "article",
    sequence: 1,
  };

  it("keeps a refreshed row that already has the new name", () => {
    const refreshed = [
      { ...source, parameters: [path, { ...suffix, name: "fileSuffix" }] },
      other,
    ];
    expect(
      schemesAfterSuccessfulParameterName(refreshed, "11", updated, [source, other]),
    ).toEqual(refreshed);
  });

  it("replaces only that name when the refresh still has the previous one", () => {
    const next = schemesAfterSuccessfulParameterName([source, other], "11", updated, [
      source,
      other,
    ]);
    expect(next[0].parameters).toEqual([path, { ...suffix, name: "fileSuffix" }]);
    expect(next[0].parameters?.[0].name).toBe("path");
    expect(next[0].parameters?.[1].type).toBe("BackendColumn");
    expect(next[0].parameters?.[1].value).toBe("article");
    expect(next[0].parameters?.[1].sequence).toBe(1);
    expect(next[0].name).toBe("Article");
    expect(next[0].generator).toBe("sys_Jexl");
    expect(next[1]).toEqual(other);
  });

  it("renames from the previous list when the refresh omits parameters", () => {
    const next = schemesAfterSuccessfulParameterName(
      [{ ...source, parameters: [] }, other],
      "11",
      updated,
      [source, other],
    );
    expect(next[0].parameters).toEqual([path, { ...suffix, name: "fileSuffix" }]);
  });
});
