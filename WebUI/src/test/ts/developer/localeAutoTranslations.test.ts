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
import type { AutoTranslationRow } from "../../../main/ts/api/developer/types";
import {
  duplicateLocaleAutoTranslationKey,
  filterLocaleAutoTranslations,
  isLocaleAutoTranslationSetReady,
  isRowForLocale,
  mergeLocaleAutoTranslations,
  normalizeScopedLocale,
  otherLocaleAutoTranslations,
  scopeAutoTranslationRowsToLocale,
} from "../../../main/ts/developer/localeAutoTranslations";

const frRow: AutoTranslationRow = {
  locale: "fr-ca",
  contentTypeName: "percPage",
  workflowName: "Default Workflow",
  communityName: "Default",
};

const frSecond: AutoTranslationRow = {
  locale: "FR_CA",
  contentTypeName: "percBlog",
  workflowName: "Default Workflow",
  communityName: "Default",
};

const enRow: AutoTranslationRow = {
  locale: "en-us",
  contentTypeName: "percPage",
  workflowName: "Default Workflow",
  communityName: "Default",
};

describe("localeAutoTranslations", () => {
  it("normalizes the scoped locale like the REST layer", () => {
    expect(normalizeScopedLocale("FR_CA ")).toBe("fr-ca");
    expect(normalizeScopedLocale(null)).toBe("");
    expect(normalizeScopedLocale(undefined)).toBe("");
  });

  it("matches rows case- and underscore-insensitively", () => {
    expect(isRowForLocale(frRow, "fr-ca")).toBe(true);
    expect(isRowForLocale(frSecond, "fr-ca")).toBe(true);
    expect(isRowForLocale(enRow, "fr-ca")).toBe(false);
    expect(isRowForLocale(frRow, "")).toBe(false);
    expect(isRowForLocale(null, "fr-ca")).toBe(false);
  });

  it("filters the full set down to one locale in set order", () => {
    const rows = [enRow, frRow, frSecond];
    expect(filterLocaleAutoTranslations(rows, "fr-ca")).toEqual([frRow, frSecond]);
    expect(filterLocaleAutoTranslations(rows, "")).toEqual([]);
    expect(filterLocaleAutoTranslations(rows, "xx-qa")).toEqual([]);
  });

  it("keeps every other locale's rows untouched", () => {
    const rows = [enRow, frRow, frSecond];
    expect(otherLocaleAutoTranslations(rows, "fr-ca")).toEqual([enRow]);
    expect(otherLocaleAutoTranslations(rows, "")).toEqual(rows);
  });

  it("forces edited rows onto the open locale", () => {
    const scoped = scopeAutoTranslationRowsToLocale(
      [{ ...frRow, locale: "en-us" }, { ...frSecond, locale: "" }],
      "fr-ca",
    );
    expect(scoped.map((r) => r.locale)).toEqual(["fr-ca", "fr-ca"]);
    expect(scoped[0].contentTypeName).toBe("percPage");
  });

  it("merges replacements after the untouched rows", () => {
    const edited: AutoTranslationRow = {
      locale: "fr-ca",
      contentTypeName: "percFile",
      workflowName: "Default Workflow",
      communityName: "Default",
    };
    expect(mergeLocaleAutoTranslations([enRow, frRow, frSecond], "fr-ca", [edited])).toEqual([
      enRow,
      edited,
    ]);
  });

  it("merges an empty edit as a clear of only that locale", () => {
    expect(mergeLocaleAutoTranslations([enRow, frRow], "fr-ca", [])).toEqual([enRow]);
    expect(mergeLocaleAutoTranslations([frRow], "fr-ca", [])).toEqual([]);
  });

  it("treats an empty edit as a valid save and rejects incomplete rows", () => {
    expect(isLocaleAutoTranslationSetReady([])).toBe(true);
    expect(isLocaleAutoTranslationSetReady([frRow])).toBe(true);
    expect(
      isLocaleAutoTranslationSetReady([{ ...frRow, workflowName: "" }]),
    ).toBe(false);
  });

  it("reports duplicates inside one locale after scoping", () => {
    expect(duplicateLocaleAutoTranslationKey([frRow, frSecond], "fr-ca")).toBeNull();
    expect(
      duplicateLocaleAutoTranslationKey([frRow, { ...frRow }], "fr-ca"),
    ).toBe("fr-ca|percpage");
  });
});
