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
import { collectInvalidLongTextFieldErrors } from "../../../main/ts/editor/longTextField";
import { collectInvalidSingleLineTextFieldErrors } from "../../../main/ts/editor/singleLineTextField";
import {
  collectInvalidHtmlFieldErrors,
  collectUnsafeHtmlFieldErrors,
  htmlContainsNul,
  htmlLooksUnsafe,
} from "../../../main/ts/editor/htmlField";

describe("htmlLooksUnsafe", () => {
  it("allows ordinary markup", () => {
    expect(htmlLooksUnsafe("<p>Hello <strong>world</strong></p>")).toBe(false);
  });

  it("flags script, event handlers, and javascript URLs", () => {
    expect(htmlLooksUnsafe('<p><script>alert(1)</script></p>')).toBe(true);
    expect(htmlLooksUnsafe('<img src="x" onerror="alert(1)">')).toBe(true);
    expect(htmlLooksUnsafe('<a href="javascript:alert(1)">x</a>')).toBe(true);
  });
});

describe("collectUnsafeHtmlFieldErrors", () => {
  it("maps only html rows", () => {
    const errors = collectUnsafeHtmlFieldErrors(
      [
        { name: "sys_title", kind: "text", value: "<script>x</script>" },
        { name: "text", kind: "html", value: "<script>x</script>" },
      ],
      "unsafe",
    );
    expect(errors).toEqual({ text: "unsafe" });
  });
});

describe("htmlContainsNul", () => {
  it("accepts ordinary HTML and rejects an embedded NUL", () => {
    expect(htmlContainsNul("<p>Hello</p>")).toBe(false);
    expect(htmlContainsNul("")).toBe(false);
    expect(htmlContainsNul("<p>bad\u0000value</p>")).toBe(true);
  });
});

describe("collectInvalidHtmlFieldErrors", () => {
  it("flags only HTML rows and leaves the other NUL gates alone", () => {
    const rows = [
      { name: "body", kind: "html" as const, value: "<p>bad\u0000</p>" },
      { name: "intro", kind: "html" as const, value: "<p>ok</p>" },
      { name: "summary", kind: "text" as const, value: "z\u0000" },
      { name: "notes", kind: "longtext" as const, value: "x\u0000y" },
    ];
    expect(collectInvalidHtmlFieldErrors(rows, "cannot save")).toEqual({
      body: "cannot save",
    });
    expect(collectUnsafeHtmlFieldErrors(rows, "unsafe")).toEqual({});
    expect(collectInvalidSingleLineTextFieldErrors(rows, "text cannot save")).toEqual({
      summary: "text cannot save",
    });
    expect(collectInvalidLongTextFieldErrors(rows, "long cannot save")).toEqual({
      notes: "long cannot save",
    });
  });

  it("does not treat unsafe markup without a NUL as a NUL error", () => {
    const rows = [
      { name: "body", kind: "html" as const, value: "<script>x</script>" },
    ];
    expect(collectInvalidHtmlFieldErrors(rows, "cannot save")).toEqual({});
    expect(collectUnsafeHtmlFieldErrors(rows, "unsafe")).toEqual({ body: "unsafe" });
  });
});
