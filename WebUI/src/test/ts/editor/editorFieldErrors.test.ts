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
  collectRequiredFieldErrors,
  isEmptyEditorFieldValue,
  mapSaveApiErrorToFieldErrors,
} from "../../../main/ts/editor/editorFieldErrors";

describe("isEmptyEditorFieldValue", () => {
  it("treats whitespace as empty for text", () => {
    expect(isEmptyEditorFieldValue("text", "  ")).toBe(true);
    expect(isEmptyEditorFieldValue("text", "Home")).toBe(false);
  });

  it("treats file/image as present when a pending file is set", () => {
    const file = new File(["x"], "hero.png", { type: "image/png" });
    expect(isEmptyEditorFieldValue("image", "", file)).toBe(false);
    expect(isEmptyEditorFieldValue("file", "", null)).toBe(true);
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
