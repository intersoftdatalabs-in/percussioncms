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
  parseTableField,
  serializeTableField,
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
