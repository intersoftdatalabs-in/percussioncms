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
  classifyEditorControl,
  isEditorFieldRequired,
  mergeEditorRows,
} from "../../../main/ts/editor/controlKinds";
import type { ItemEditorFields } from "../../../main/ts/editor/itemFieldsApi";

describe("classifyEditorControl", () => {
  it("maps TinyMCE / file / image / keyword / community controls", () => {
    expect(classifyEditorControl({ control: "sys_tinymce" }, "body")).toBe("html");
    expect(classifyEditorControl({ control: "sys_File" }, "item_file_attachment")).toBe(
      "file",
    );
    expect(classifyEditorControl({ control: "sys_webImageFX" }, "img")).toBe("image");
    expect(classifyEditorControl({ control: "sys_DropDownSingle" }, "keywords")).toBe(
      "keyword",
    );
    expect(classifyEditorControl({ control: "sys_DropDownSingle" }, "sys_communityid")).toBe(
      "community",
    );
    expect(classifyEditorControl({ control: "sys_EditBox" }, "sys_title")).toBe("text");
    expect(classifyEditorControl({ control: "sys_CalendarSimple" }, "sys_contentstartdate")).toBe(
      "date",
    );
    expect(
      classifyEditorControl({ control: "sys_CalendarSimple", dataType: "datetime" }, "event_at"),
    ).toBe("datetime");
    expect(classifyEditorControl({ control: "sys_TextArea" }, "description")).toBe(
      "longtext",
    );
    expect(
      classifyEditorControl({ control: "sys_EditBox", dataType: "text" }, "sys_title"),
    ).toBe("text");
    expect(
      classifyEditorControl({ control: "sys_EditBox", dataType: "maxtext" }, "description"),
    ).toBe("longtext");
    expect(classifyEditorControl({ control: "sys_PageLink", dataType: "text" }, "page")).toBe(
      "link",
    );
    expect(
      classifyEditorControl({ control: "sys_EditBox", dataType: "link" }, "target"),
    ).toBe("link");
    expect(
      classifyEditorControl({ control: "sys_RelatedContentTable" }, "slots"),
    ).not.toBe("link");
    expect(classifyEditorControl({ control: "sys_Number", dataType: "integer" }, "qty")).toBe(
      "number",
    );
    expect(classifyEditorControl({ control: "sys_EditBox", dataType: "float" }, "rate")).toBe(
      "number",
    );
    expect(classifyEditorControl({ control: "sys_Table" }, "hours")).toBe("table");
    expect(classifyEditorControl({ control: "sys_EditBox", dataType: "table" }, "grid")).toBe(
      "table",
    );
    expect(classifyEditorControl({ control: "sys_RelatedContentTable" }, "slots")).not.toBe(
      "table",
    );
  });
});

describe("mergeEditorRows", () => {
  const payload: ItemEditorFields = {
    contentId: "42",
    contentType: "percRichText",
    name: "Intro",
    checkoutUser: "admin",
    fields: [{ name: "sys_title", value: "Intro" }],
  };

  it("uses content-type labels and injects schema-only rich controls", () => {
    const rows = mergeEditorRows(payload, [
      { name: "sys_title", label: "Title", control: "sys_EditBox" },
      { name: "text", label: "Body", control: "sys_tinymce" },
      { name: "img", label: "Image", control: "sys_webImageFX" },
      { name: "keywords", label: "Keywords", control: "sys_DropDownSingle" },
      { name: "sys_communityid", label: "Community", control: "sys_DropDownSingle" },
    ]);
    expect(rows.find((r) => r.name === "sys_title")?.label).toBe("Title");
    expect(rows.find((r) => r.name === "text")?.kind).toBe("html");
    expect(rows.find((r) => r.name === "img")?.kind).toBe("image");
    expect(rows.find((r) => r.name === "keywords")?.kind).toBe("keyword");
    expect(rows.find((r) => r.name === "sys_communityid")?.kind).toBe("community");
  });

  it("copies required from the catalog flag or occurrence", () => {
    const rows = mergeEditorRows(payload, [
      { name: "sys_title", label: "Title", required: true },
      { name: "text", label: "Body", control: "sys_tinymce", occurrence: "required" },
    ]);
    expect(isEditorFieldRequired({ required: true })).toBe(true);
    expect(isEditorFieldRequired({ occurrence: "oneOrMore" })).toBe(true);
    expect(isEditorFieldRequired({ required: false, occurrence: "optional" })).toBe(
      false,
    );
    expect(rows.find((r) => r.name === "sys_title")?.required).toBe(true);
    expect(rows.find((r) => r.name === "text")?.required).toBe(true);
  });
});
