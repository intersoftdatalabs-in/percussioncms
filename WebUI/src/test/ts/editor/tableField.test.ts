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
import {
  collectTableNulFieldErrors,
  parseTableField,
  serializeTableField,
  tableFieldContainsNul,
  tableFieldIsEmpty,
} from "../../../main/ts/editor/tableField";

describe("tableField", () => {
  it("round-trips rows and columns and treats a blank default grid as empty", () => {
    expect(tableFieldIsEmpty("")).toBe(true);
    expect(serializeTableField(parseTableField(""))).toBe("");
    const stored = serializeTableField({
      columns: ["day", "hours"],
      rows: [
        ["Mon", "8"],
        ["Tue", ""],
      ],
    });
    expect(parseTableField(stored)).toEqual({
      columns: ["day", "hours"],
      rows: [
        ["Mon", "8"],
        ["Tue", ""],
      ],
    });
    expect(tableFieldIsEmpty(stored)).toBe(false);
  });

  it("keeps a legacy plain string as one cell", () => {
    expect(parseTableField("not-json")).toEqual({
      columns: ["value"],
      rows: [["not-json"]],
    });
  });

  it("keeps a blank row the author added", () => {
    const stored = serializeTableField({
      columns: ["value"],
      rows: [[""]],
    });
    expect(stored).toBe('{"columns":["value"],"rows":[[""]]}');
    expect(tableFieldIsEmpty(stored)).toBe(true);
  });

  it("keeps column names when every cell is blank", () => {
    const raw = '{"columns":["day","hours"],"rows":[]}';
    expect(tableFieldIsEmpty(raw)).toBe(true);
    expect(serializeTableField(parseTableField(raw))).toBe(
      '{"columns":["day","hours"],"rows":[]}',
    );
  });
});

describe("tableFieldContainsNul", () => {
  it("rejects a cell NUL after JSON unescape and leaves empty and normal cells alone", () => {
    const escaped = JSON.stringify({ columns: ["day"], rows: [["Mon\u0000"]] });
    expect(escaped.includes("\u0000")).toBe(false);
    expect(tableFieldContainsNul(escaped)).toBe(true);
    expect(tableFieldIsEmpty(escaped)).toBe(false);
    expect(tableFieldContainsNul(JSON.stringify({ columns: ["day"], rows: [["Mon"]] }))).toBe(
      false,
    );
    expect(tableFieldContainsNul("")).toBe(false);
    expect(tableFieldContainsNul('{"columns":["day"],"rows":[["   "]]}')).toBe(false);
    expect(tableFieldIsEmpty('{"columns":["day"],"rows":[["   "]]}')).toBe(true);
    expect(tableFieldContainsNul("legacy\u0000")).toBe(true);
  });
});

describe("collectTableNulFieldErrors", () => {
  it("flags only table rows and does not treat an empty table as a NUL", () => {
    const nul = JSON.stringify({ columns: ["day"], rows: [["Mon\u0000"]] });
    const rows = [
      { name: "hours", kind: "table" as const, value: nul },
      { name: "notes", kind: "table" as const, value: "" },
      { name: "title", kind: "text" as const, value: "z\u0000" },
      {
        name: "days",
        kind: "table" as const,
        value: JSON.stringify({ columns: ["day"], rows: [["Tue"]] }),
      },
    ];
    expect(collectTableNulFieldErrors(rows, "cannot save")).toEqual({
      hours: "cannot save",
    });
    expect(tableFieldIsEmpty("")).toBe(true);
    expect(tableFieldContainsNul("")).toBe(false);
  });
});
