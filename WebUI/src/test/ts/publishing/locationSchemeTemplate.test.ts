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
  buildLocationSchemeTemplateBody,
  schemesAfterSuccessfulTemplate,
  validateLocationSchemeTemplate,
} from "@/publishing/locationSchemeTemplate";

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

describe("validateLocationSchemeTemplate", () => {
  it("rejects a blank template so the stored id is not cleared", () => {
    expect(validateLocationSchemeTemplate("   ")).toEqual({
      ok: false,
      error: "Location scheme template is required",
    });
  });

  it.each(["abc", "12.5", "-4", "0", "1e2", "004"])(
    "rejects a non-numeric template %s",
    (raw) => {
      expect(validateLocationSchemeTemplate(raw)).toEqual({
        ok: false,
        error: "Location scheme template must be a number",
      });
    },
  );

  it("rejects an integer past the safe range", () => {
    expect(validateLocationSchemeTemplate("9".repeat(20))).toEqual({
      ok: false,
      error: "Location scheme template must be a number",
    });
  });

  it("trims a positive template id", () => {
    expect(validateLocationSchemeTemplate("  12  ")).toEqual({
      ok: true,
      templateId: 12,
    });
  });
});

describe("buildLocationSchemeTemplateBody", () => {
  it("sends the template id only", () => {
    expect(buildLocationSchemeTemplateBody(12)).toEqual({ templateId: 12 });
    const body = buildLocationSchemeTemplateBody(12);
    expect(body).not.toHaveProperty("name");
    expect(body).not.toHaveProperty("generator");
    expect(body).not.toHaveProperty("description");
    expect(body).not.toHaveProperty("contentTypeId");
    expect(body).not.toHaveProperty("contextId");
    expect(body).not.toHaveProperty("parameters");
    expect(body).not.toHaveProperty("schemeId");
  });
});

describe("schemesAfterSuccessfulTemplate", () => {
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
    const refreshed = [{ ...source, templateId: 12 }, other];
    expect(schemesAfterSuccessfulTemplate(refreshed, "11", 12, [source, other])).toBe(
      refreshed,
    );
  });

  it("patches only the template when the reload failed", () => {
    expect(schemesAfterSuccessfulTemplate(null, "11", 12, [source, other])).toEqual([
      { ...source, templateId: 12 },
      other,
    ]);
  });
});
