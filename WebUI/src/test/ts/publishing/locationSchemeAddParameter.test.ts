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
import { buildLocationSchemeRenameBody } from "@/publishing/locationSchemeRename";
import {
  LOCATION_SCHEME_PARAMETER_EXISTS,
  LOCATION_SCHEME_PARAMETER_NAME_REQUIRED,
  LOCATION_SCHEME_PARAMETER_NAME_TOO_LONG,
  LOCATION_SCHEME_PARAMETER_TYPE_TOO_LONG,
  LOCATION_SCHEME_PARAMETER_VALUE_REQUIRED,
  LOCATION_SCHEME_PARAMETER_NAME_MAX_LENGTH,
  LOCATION_SCHEME_PARAMETER_TYPE_MAX_LENGTH,
  buildLocationSchemeAddParameterBody,
  schemesAfterSuccessfulAdd,
  validateLocationSchemeAddParameter,
} from "@/publishing/locationSchemeAddParameter";

const existing = [{ name: "path", type: "String", value: "$sys.site.path", sequence: 0 }];

const source: LocationSchemeSummary = {
  schemeId: "11",
  name: "Article",
  generator: "sys_Jexl",
  description: "Pages",
  contentTypeId: 4,
  templateId: 8,
  parameters: existing,
};

describe("validateLocationSchemeAddParameter", () => {
  it("rejects an empty name or value before any write", () => {
    expect(validateLocationSchemeAddParameter("  ", "String", "article", existing)).toEqual({
      ok: false,
      error: LOCATION_SCHEME_PARAMETER_NAME_REQUIRED,
    });
    expect(validateLocationSchemeAddParameter("suffix", "String", "   ", existing)).toEqual({
      ok: false,
      error: LOCATION_SCHEME_PARAMETER_VALUE_REQUIRED,
    });
  });

  it("rejects an overlong name or type", () => {
    expect(
      validateLocationSchemeAddParameter(
        "n".repeat(LOCATION_SCHEME_PARAMETER_NAME_MAX_LENGTH + 1),
        "String",
        "article",
        existing,
      ),
    ).toEqual({ ok: false, error: LOCATION_SCHEME_PARAMETER_NAME_TOO_LONG });
    expect(
      validateLocationSchemeAddParameter(
        "suffix",
        "t".repeat(LOCATION_SCHEME_PARAMETER_TYPE_MAX_LENGTH + 1),
        "article",
        existing,
      ),
    ).toEqual({ ok: false, error: LOCATION_SCHEME_PARAMETER_TYPE_TOO_LONG });
  });

  it("rejects a duplicate name and does not add a second row", () => {
    expect(validateLocationSchemeAddParameter(" path ", "String", "other", existing)).toEqual({
      ok: false,
      error: LOCATION_SCHEME_PARAMETER_EXISTS,
    });
  });

  it("trims one new parameter", () => {
    expect(
      validateLocationSchemeAddParameter(
        "  suffix  ",
        "  BackendColumn  ",
        "  Contentstatus.contentid  ",
        existing,
      ),
    ).toEqual({
      ok: true,
      parameter: {
        name: "suffix",
        type: "BackendColumn",
        value: "Contentstatus.contentid",
      },
    });
  });
});

describe("buildLocationSchemeAddParameterBody", () => {
  it("sends only the one parameter to append", () => {
    const body = buildLocationSchemeAddParameterBody({
      name: "suffix",
      type: "BackendColumn",
      value: "Contentstatus.contentid",
    });
    expect(body).toEqual({
      addParameter: true,
      parameters: [
        {
          name: "suffix",
          type: "BackendColumn",
          value: "Contentstatus.contentid",
        },
      ],
    });
    expect(body.name).toBeUndefined();
    expect(body.generator).toBeUndefined();
    expect(body.description).toBeUndefined();
    expect(body.contentTypeId).toBeUndefined();
    expect(body.templateId).toBeUndefined();
    expect(wrapLocationScheme(body).locationScheme).toEqual({
      name: undefined,
      description: undefined,
      contextId: undefined,
      generator: undefined,
      contentTypeId: undefined,
      templateId: undefined,
      addParameter: true,
      schemeParameter: [
        {
          name: "suffix",
          type: "BackendColumn",
          value: "Contentstatus.contentid",
        },
      ],
    });
    expect(buildLocationSchemeRenameBody("Renamed")).toEqual({ name: "Renamed" });
  });
});

describe("schemesAfterSuccessfulAdd", () => {
  const other: LocationSchemeSummary = {
    schemeId: "12",
    name: "Brief",
    generator: "legacy-gen",
    description: "Short",
  };
  const added = {
    name: "suffix",
    type: "BackendColumn",
    value: "Contentstatus.contentid",
  };

  it("keeps a refreshed row that already lists the new parameter", () => {
    const refreshed = [
      {
        ...source,
        parameters: [...existing, { ...added, sequence: 1 }],
      },
      other,
    ];
    expect(schemesAfterSuccessfulAdd(refreshed, "11", added, [source, other])).toEqual(
      refreshed,
    );
  });

  it("appends onto the previous parameters when the refresh omits them", () => {
    const refreshed = [{ ...source, parameters: undefined }, other];
    expect(schemesAfterSuccessfulAdd(refreshed, "11", added, [source, other])[0].parameters).toEqual(
      [...existing, { ...added, sequence: 1 }],
    );
    expect(schemesAfterSuccessfulAdd(refreshed, "11", added, [source, other])[1]).toEqual(other);
  });

  it("appends when the refresh failed and leaves the other scheme", () => {
    const next = schemesAfterSuccessfulAdd(null, "11", added, [source, other]);
    expect(next[0].name).toBe("Article");
    expect(next[0].generator).toBe("sys_Jexl");
    expect(next[0].parameters).toEqual([...existing, { ...added, sequence: 1 }]);
    expect(next[1]).toEqual(other);
  });
});
