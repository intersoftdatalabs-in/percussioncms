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
  buildLocationSchemeRemoveParameterBody,
  schemesAfterSuccessfulRemove,
  validateLocationSchemeRemoveParameter,
} from "@/publishing/locationSchemeRemoveParameter";

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

describe("validateLocationSchemeRemoveParameter", () => {
  it("rejects a blank name before any write", () => {
    expect(validateLocationSchemeRemoveParameter("  ", source.parameters)).toEqual({
      ok: false,
      error: LOCATION_SCHEME_PARAMETER_NAME_REQUIRED,
    });
  });

  it("rejects a name that is not stored and does not drop another parameter", () => {
    expect(validateLocationSchemeRemoveParameter("missing", source.parameters)).toEqual({
      ok: false,
      error: LOCATION_SCHEME_PARAMETER_NOT_ON_SCHEME,
    });
  });

  it("trims the stored name and keeps that parameter's type and value", () => {
    expect(validateLocationSchemeRemoveParameter("  suffix  ", source.parameters)).toEqual({
      ok: true,
      parameter: { name: "suffix", type: "BackendColumn", value: "article" },
    });
  });
});

describe("buildLocationSchemeRemoveParameterBody", () => {
  it("sends only the one parameter to remove", () => {
    const body = buildLocationSchemeRemoveParameterBody({
      name: "suffix",
      type: "BackendColumn",
      value: "article",
    });
    expect(body).toEqual({
      removeParameter: true,
      parameters: [{ name: "suffix", type: "BackendColumn", value: "article" }],
    });
    expect(body.name).toBeUndefined();
    expect(body.generator).toBeUndefined();
    expect(body.description).toBeUndefined();
    expect(body.contentTypeId).toBeUndefined();
    expect(body.templateId).toBeUndefined();
    expect(body.addParameter).toBeUndefined();
    expect(wrapLocationScheme(body).locationScheme).toEqual({
      name: undefined,
      description: undefined,
      contextId: undefined,
      generator: undefined,
      contentTypeId: undefined,
      templateId: undefined,
      removeParameter: true,
      parameters: {
        schemeParameter: [{ name: "suffix", type: "BackendColumn", value: "article" }],
      },
    });
  });
});

describe("schemesAfterSuccessfulRemove", () => {
  const other: LocationSchemeSummary = {
    schemeId: "12",
    name: "Brief",
    generator: "legacy-gen",
    description: "Short",
  };

  it("keeps a refreshed row that already dropped the parameter", () => {
    const refreshed = [{ ...source, parameters: [path] }, other];
    expect(schemesAfterSuccessfulRemove(refreshed, "11", "suffix", [source, other])).toEqual(
      refreshed,
    );
  });

  it("drops the name when the refresh still lists it and leaves the other parameter", () => {
    const next = schemesAfterSuccessfulRemove([source, other], "11", "suffix", [source, other]);
    expect(next[0].parameters).toEqual([path]);
    expect(next[0].name).toBe("Article");
    expect(next[0].generator).toBe("sys_Jexl");
    expect(next[0].description).toBe("Pages");
    expect(next[0].contentTypeId).toBe(4);
    expect(next[0].templateId).toBe(8);
    expect(next[1]).toEqual(other);
  });

  it("drops the name when the refresh omits parameters", () => {
    const refreshed = [{ ...source, parameters: undefined }, other];
    expect(
      schemesAfterSuccessfulRemove(refreshed, "11", "suffix", [source, other])[0].parameters,
    ).toEqual([path]);
  });

  it("drops the last parameter without dropping the scheme when the refresh failed", () => {
    const onlyPath: LocationSchemeSummary = { ...source, parameters: [path] };
    const next = schemesAfterSuccessfulRemove(null, "11", "path", [onlyPath, other]);
    expect(next[0].schemeId).toBe("11");
    expect(next[0].name).toBe("Article");
    expect(next[0].generator).toBe("sys_Jexl");
    expect(next[0].parameters).toEqual([]);
    expect(next[1]).toEqual(other);
  });
});
