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
  canRevertFromEditor,
  editorDirtyFieldNames,
  revertedEditorDraft,
} from "../../../main/ts/editor/editorRevert";

describe("editorRevert (#5889)", () => {
  it("rebuilds the draft from the loaded values", () => {
    expect(
      revertedEditorDraft([
        { name: "sys_title", value: "Home" },
        { name: "count", value: 3 },
        { name: "empty" },
      ]),
    ).toEqual({ sys_title: "Home", count: "3", empty: "" });
  });

  it("rebuilds an empty draft without loaded fields", () => {
    expect(revertedEditorDraft(undefined)).toEqual({});
  });

  it("names only the fields whose draft differs, in loaded order", () => {
    expect(
      editorDirtyFieldNames(
        [
          { name: "sys_title", value: "Home" },
          { name: "body", value: "Same" },
        ],
        { sys_title: "Draft title", body: "Same" },
        {},
      ),
    ).toEqual(["sys_title"]);
  });

  it("is clean when the draft matches the loaded values", () => {
    expect(
      editorDirtyFieldNames(
        [{ name: "sys_title", value: "Home" }],
        { sys_title: "Home" },
        {},
      ),
    ).toEqual([]);
  });

  it("treats a missing draft entry as the loaded value", () => {
    expect(
      editorDirtyFieldNames([{ name: "sys_title", value: "Home" }], {}, {}),
    ).toEqual([]);
  });

  it("appends staged binaries and pending clears", () => {
    const file = new File(["bytes"], "notes.txt", { type: "text/plain" });
    expect(
      editorDirtyFieldNames(
        [{ name: "sys_title", value: "Home" }],
        { sys_title: "Home" },
        { item_file_attachment: file },
        { item_image: true },
      ),
    ).toEqual(["item_file_attachment", "item_image"]);
  });

  it("ignores client edits of schema read-only fields", () => {
    expect(
      editorDirtyFieldNames(
        [{ name: "sys_title", value: "Home" }],
        { sys_title: "Draft title" },
        {},
        undefined,
        new Set(["sys_title"]),
      ),
    ).toEqual([]);
  });

  it("shows the revert control only for dirty edit mode", () => {
    expect(canRevertFromEditor("edit", true)).toBe(true);
    expect(canRevertFromEditor("edit", false)).toBe(false);
    expect(canRevertFromEditor("view", true)).toBe(false);
    expect(canRevertFromEditor("promote", true)).toBe(false);
  });
});
