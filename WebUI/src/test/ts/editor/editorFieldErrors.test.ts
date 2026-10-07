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
  collectInvalidDateFieldErrors,
  collectRequiredFieldErrors,
  firstInvalidEditorFieldName,
  isEmptyEditorFieldValue,
  mapSaveApiErrorToFieldErrors,
} from "../../../main/ts/editor/editorFieldErrors";

describe("isEmptyEditorFieldValue", () => {
  it("treats whitespace as empty for text", () => {
    expect(isEmptyEditorFieldValue("text", "  ")).toBe(true);
    expect(isEmptyEditorFieldValue("text", "Home")).toBe(false);
  });

  it("treats a blank or whitespace number as empty and a stored number as present", () => {
    expect(isEmptyEditorFieldValue("number", "")).toBe(true);
    expect(isEmptyEditorFieldValue("number", "   ")).toBe(true);
    expect(isEmptyEditorFieldValue("number", "4")).toBe(false);
  });

  it("treats file/image as present when a pending file is set", () => {
    const file = new File(["x"], "hero.png", { type: "image/png" });
    expect(isEmptyEditorFieldValue("image", "", file)).toBe(false);
    expect(isEmptyEditorFieldValue("file", "", null)).toBe(true);
  });

  it("treats a cleared required file or image as empty even when the stored name remains", () => {
    const notes = new File(["x"], "notes.txt", { type: "text/plain" });
    const hero = new File(["x"], "hero.png", { type: "image/png" });
    expect(isEmptyEditorFieldValue("file", "brief.pdf", null, true)).toBe(true);
    expect(isEmptyEditorFieldValue("file", "brief.pdf", notes, true)).toBe(false);
    expect(isEmptyEditorFieldValue("file", "brief.pdf", null, false)).toBe(false);
    expect(isEmptyEditorFieldValue("file", "   ", null, false)).toBe(true);
    expect(isEmptyEditorFieldValue("image", "hero.png", null, true)).toBe(true);
    expect(isEmptyEditorFieldValue("image", "hero.png", hero, true)).toBe(false);
    expect(isEmptyEditorFieldValue("image", "hero.png", null, false)).toBe(false);
    expect(isEmptyEditorFieldValue("image", "", null)).toBe(true);
    expect(isEmptyEditorFieldValue("image", "   ", null, false)).toBe(true);
  });
});

describe("collectRequiredFieldErrors", () => {
  it("maps empty required rows and skips optional ones", () => {
    const errors = collectRequiredFieldErrors(
      [
        { name: "sys_title", kind: "text", required: true, value: "" },
        { name: "displaytitle", kind: "text", required: true, value: "Hi" },
        { name: "page_title", kind: "text", required: false, value: "" },
      ],
      {},
      "This field is required.",
    );
    expect(errors).toEqual({ sys_title: "This field is required." });
  });

  it("refuses a blank required number and still allows an optional blank number", () => {
    const errors = collectRequiredFieldErrors(
      [
        { name: "qty", kind: "number", required: true, value: "   " },
        { name: "optionalQty", kind: "number", required: false, value: "" },
        { name: "count", kind: "number", required: true, value: "7" },
      ],
      {},
      "This field is required.",
    );
    expect(errors).toEqual({ qty: "This field is required." });
  });

  it("refuses a blank required date and still allows an optional blank date", () => {
    const errors = collectRequiredFieldErrors(
      [
        { name: "start", kind: "date", required: true, value: "" },
        { name: "end", kind: "date", required: false, value: "   " },
        { name: "publish", kind: "date", required: true, value: "2026-09-18" },
      ],
      {},
      "This field is required.",
    );
    expect(errors).toEqual({ start: "This field is required." });
  });

  it("refuses a blank required datetime and still allows an optional blank datetime", () => {
    const errors = collectRequiredFieldErrors(
      [
        { name: "event_at", kind: "datetime", required: true, value: "" },
        { name: "optionalAt", kind: "datetime", required: false, value: "   " },
        { name: "ends", kind: "datetime", required: true, value: "2026-09-18 14:30:00" },
      ],
      {},
      "This field is required.",
    );
    expect(errors).toEqual({ event_at: "This field is required." });
    expect(isEmptyEditorFieldValue("datetime", "")).toBe(true);
    expect(isEmptyEditorFieldValue("datetime", " \n\t ")).toBe(true);
    expect(isEmptyEditorFieldValue("datetime", "2026-09-18 14:30:00")).toBe(false);
  });

  it("refuses a blank required link and still allows an optional blank link", () => {
    const errors = collectRequiredFieldErrors(
      [
        { name: "page", kind: "link", required: true, value: "   " },
        { name: "optionalPage", kind: "link", required: false, value: "" },
        { name: "related", kind: "link", required: true, value: "594" },
      ],
      {},
      "This field is required.",
    );
    expect(errors).toEqual({ page: "This field is required." });
    expect(isEmptyEditorFieldValue("link", "")).toBe(true);
    expect(isEmptyEditorFieldValue("link", "  ")).toBe(true);
    expect(isEmptyEditorFieldValue("link", "//Sites/Example/index")).toBe(false);
  });

  it("refuses a blank required HTML field and still allows an optional blank HTML field", () => {
    const errors = collectRequiredFieldErrors(
      [
        { name: "text", kind: "html", required: true, value: "   " },
        { name: "optionalBody", kind: "html", required: false, value: "" },
        { name: "intro", kind: "html", required: true, value: "<p>Hi</p>" },
      ],
      {},
      "This field is required.",
    );
    expect(errors).toEqual({ text: "This field is required." });
    expect(isEmptyEditorFieldValue("html", "")).toBe(true);
    expect(isEmptyEditorFieldValue("html", " \n\t ")).toBe(true);
    expect(isEmptyEditorFieldValue("html", "<p>Hi</p>")).toBe(false);
  });

  it("refuses a blank required keyword and still allows an optional blank keyword", () => {
    const errors = collectRequiredFieldErrors(
      [
        { name: "keywords", kind: "keyword", required: true, value: "" },
        { name: "tags", kind: "keyword", required: true, value: "   " },
        { name: "optionalKeywords", kind: "keyword", required: false, value: "" },
        { name: "topic", kind: "keyword", required: true, value: "news" },
      ],
      {},
      "This field is required.",
    );
    expect(errors).toEqual({
      keywords: "This field is required.",
      tags: "This field is required.",
    });
    expect(isEmptyEditorFieldValue("keyword", "")).toBe(true);
    expect(isEmptyEditorFieldValue("keyword", " \n\t ")).toBe(true);
    expect(isEmptyEditorFieldValue("keyword", "news")).toBe(false);
  });

  it("refuses an empty required community and still allows an optional empty community", () => {
    const errors = collectRequiredFieldErrors(
      [
        { name: "sys_communityid", kind: "community", required: true, value: "" },
        { name: "otherCommunity", kind: "community", required: true, value: "   " },
        { name: "optionalCommunity", kind: "community", required: false, value: "" },
        { name: "kept", kind: "community", required: true, value: "10" },
      ],
      {},
      "This field is required.",
    );
    expect(errors).toEqual({
      sys_communityid: "This field is required.",
      otherCommunity: "This field is required.",
    });
    expect(isEmptyEditorFieldValue("community", "")).toBe(true);
    expect(isEmptyEditorFieldValue("community", " \n\t ")).toBe(true);
    expect(isEmptyEditorFieldValue("community", "10")).toBe(false);
  });

  it("refuses a required file with nothing chosen and a cleared stored name", () => {
    const notes = new File(["x"], "notes.txt", { type: "text/plain" });
    const errors = collectRequiredFieldErrors(
      [
        { name: "item_file_attachment", kind: "file", required: true, value: "" },
        { name: "brief", kind: "file", required: true, value: "brief.pdf" },
        { name: "optional_file", kind: "file", required: false, value: "" },
        { name: "img", kind: "image", required: true, value: "hero.png" },
      ],
      { notes: notes },
      "This field is required.",
      { brief: true, optional_file: true },
    );
    expect(errors).toEqual({
      item_file_attachment: "This field is required.",
      brief: "This field is required.",
    });
  });

  it("refuses an empty required table and still allows cell text or an optional empty table", () => {
    const withCells = JSON.stringify({ columns: ["day"], rows: [["Mon"]] });
    const columnsOnly = JSON.stringify({ columns: ["day", "hours"], rows: [] });
    const blankRow = JSON.stringify({ columns: ["value"], rows: [[""]] });
    const errors = collectRequiredFieldErrors(
      [
        { name: "hours", kind: "table", required: true, value: "" },
        { name: "spaces", kind: "table", required: true, value: "   " },
        { name: "blank", kind: "table", required: true, value: blankRow },
        { name: "headers", kind: "table", required: true, value: columnsOnly },
        { name: "optional", kind: "table", required: false, value: "" },
        { name: "days", kind: "table", required: true, value: withCells },
      ],
      {},
      "This field is required.",
    );
    expect(errors).toEqual({
      hours: "This field is required.",
      spaces: "This field is required.",
      blank: "This field is required.",
      headers: "This field is required.",
    });
    expect(isEmptyEditorFieldValue("table", "")).toBe(true);
    expect(isEmptyEditorFieldValue("table", "   ")).toBe(true);
    expect(isEmptyEditorFieldValue("table", blankRow)).toBe(true);
    expect(isEmptyEditorFieldValue("table", columnsOnly)).toBe(true);
    expect(isEmptyEditorFieldValue("table", withCells)).toBe(false);
  });

  it("refuses a required image with nothing chosen and a cleared stored name", () => {
    const next = new File(["x"], "next.png", { type: "image/png" });
    const errors = collectRequiredFieldErrors(
      [
        { name: "img", kind: "image", required: true, value: "" },
        { name: "hero", kind: "image", required: true, value: "hero.png" },
        { name: "optional_img", kind: "image", required: false, value: "old.png" },
        { name: "replacement", kind: "image", required: true, value: "hero.png" },
      ],
      { replacement: next },
      "This field is required.",
      { hero: true, optional_img: true, replacement: true },
    );
    expect(errors).toEqual({
      img: "This field is required.",
      hero: "This field is required.",
    });
  });
});

describe("firstInvalidEditorFieldName", () => {
  it("returns the first row in form order that has an error", () => {
    expect(
      firstInvalidEditorFieldName(["sys_title", "displaytitle", "body"], {
        displaytitle: "bad",
        body: "also",
      }),
    ).toBe("displaytitle");
  });

  it("returns null when no named row has an error", () => {
    expect(firstInvalidEditorFieldName(["sys_title"], {})).toBeNull();
    expect(
      firstInvalidEditorFieldName(["sys_title"], { other: "x" }),
    ).toBeNull();
  });
});

describe("collectInvalidDateFieldErrors", () => {
  it("flags unparseable date values and skips empty optional dates", () => {
    const errors = collectInvalidDateFieldErrors(
      [
        { name: "sys_contentstartdate", kind: "date", required: false, value: "not-a-date" },
        { name: "event_at", kind: "datetime", required: false, value: "2026-13-40 99:99" },
        { name: "ok", kind: "date", required: false, value: "2026-09-18" },
        { name: "empty", kind: "date", required: false, value: "" },
        { name: "title", kind: "text", required: false, value: "x" },
      ],
      "Enter a valid date.",
    );
    expect(errors.sys_contentstartdate).toBe("Enter a valid date.");
    expect(errors.event_at).toBe("Enter a valid date.");
    expect(errors.ok).toBeUndefined();
    expect(errors.empty).toBeUndefined();
    expect(errors.title).toBeUndefined();
  });
});

describe("mapSaveApiErrorToFieldErrors", () => {
  const known = ["sys_title", "displaytitle"];

  it("maps RestError errorData field name onto the row", () => {
    const mapped = mapSaveApiErrorToFieldErrors(
      {
        status: 400,
        statusText: "Bad Request",
        body: {
          Error: {
            message: "displaytitle is required",
            errorData: "displaytitle",
          },
        },
      },
      known,
      "Could not save the item.",
    );
    expect(mapped.fieldErrors.displaytitle).toMatch(/displaytitle/i);
    expect(mapped.banner).toMatch(/displaytitle/i);
  });

  it("maps fieldErrors bag entries", () => {
    const mapped = mapSaveApiErrorToFieldErrors(
      {
        status: 400,
        statusText: "Bad Request",
        body: {
          fieldErrors: { sys_title: "Title cannot be blank" },
        },
      },
      known,
      "Could not save the item.",
    );
    expect(mapped.fieldErrors.sys_title).toBe("Title cannot be blank");
  });

  it("maps a quoted field name in the banner when no structured field", () => {
    const mapped = mapSaveApiErrorToFieldErrors(
      {
        status: 400,
        statusText: "Bad Request",
        body: { message: "The value of the field 'sys_title' is invalid" },
      },
      known,
      "Could not save the item.",
    );
    expect(mapped.fieldErrors.sys_title).toMatch(/invalid/i);
  });

  it("leaves unnamed 400s on the banner only", () => {
    const mapped = mapSaveApiErrorToFieldErrors(
      {
        status: 400,
        statusText: "Bad Request",
        body: { message: "Item is locked" },
      },
      known,
      "Could not save the item.",
    );
    expect(mapped.fieldErrors).toEqual({});
    expect(mapped.banner).toBe("Item is locked");
  });
});
