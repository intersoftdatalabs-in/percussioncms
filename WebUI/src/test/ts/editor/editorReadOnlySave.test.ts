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
  fieldsForEditorSave,
  schemaReadOnlyFieldNames,
  type EditorSaveFieldRow,
} from "../../../main/ts/editor/editorReadOnlySave";

describe("schemaReadOnlyFieldNames", () => {
  it("keeps only fields the content type marks read-only", () => {
    expect(
      schemaReadOnlyFieldNames([
        { name: "sys_title", readOnly: false },
        { name: "displaytitle", readOnly: true },
        { name: "  ", readOnly: true },
        { name: "body" },
      ]),
    ).toEqual(new Set(["displaytitle"]));
    expect(schemaReadOnlyFieldNames(undefined).size).toBe(0);
  });
});

describe("fieldsForEditorSave", () => {
  const rows: EditorSaveFieldRow[] = [
    { name: "sys_title", kind: "text", readOnly: false, value: "Home page" },
    { name: "displaytitle", kind: "text", readOnly: true, value: "Hacked" },
    { name: "qty", kind: "number", value: "4", numericMinimum: "0", numericMaximum: "10" },
    { name: "lockedQty", kind: "number", readOnly: true, value: "99" },
    { name: "page", kind: "link", value: "42" },
    { name: "photo", kind: "image", value: "" },
    { name: "attach", kind: "file", readOnly: true, value: "" },
  ];

  it("omits schema read-only fields and binary widgets", () => {
    const sent = fieldsForEditorSave(rows);
    expect(sent.map((field) => field.name)).toEqual(["sys_title", "qty", "page"]);
    expect(sent.find((field) => field.name === "displaytitle")).toBeUndefined();
    expect(sent.find((field) => field.name === "lockedQty")).toBeUndefined();
    expect(sent.find((field) => field.name === "sys_title")?.value).toBe("Home page");
    expect(sent.find((field) => field.name === "qty")).toEqual({
      name: "qty",
      value: "4",
      dataType: "integer",
      minimum: "0",
      maximum: "10",
    });
    expect(sent.find((field) => field.name === "page")?.dataType).toBe("link");
  });

  it("marks a non-integer number as float", () => {
    expect(
      fieldsForEditorSave([
        { name: "price", kind: "number", value: "1.5", numericInteger: false },
      ]),
    ).toEqual([
      {
        name: "price",
        value: "1.5",
        dataType: "float",
        minimum: undefined,
        maximum: undefined,
      },
    ]);
  });
});
