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
import {
  collectInvalidSingleLineTextFieldErrors,
  singleLineTextContainsNul,
} from "../../../main/ts/editor/singleLineTextField";

describe("singleLineTextContainsNul", () => {
  it("accepts ordinary single-line text and rejects an embedded NUL", () => {
    expect(singleLineTextContainsNul("Updated headline")).toBe(false);
    expect(singleLineTextContainsNul("")).toBe(false);
    expect(singleLineTextContainsNul("bad\u0000value")).toBe(true);
  });
});

describe("collectInvalidSingleLineTextFieldErrors", () => {
  it("flags only single-line text rows and leaves the long-text NUL gate alone", () => {
    const rows = [
      { name: "summary", kind: "text" as const, value: "bad\u0000value" },
      { name: "title", kind: "text" as const, value: "ok" },
      { name: "notes", kind: "longtext" as const, value: "x\u0000y" },
      { name: "body", kind: "html" as const, value: "z\u0000" },
    ];
    expect(collectInvalidSingleLineTextFieldErrors(rows, "cannot save")).toEqual({
      summary: "cannot save",
    });
    expect(collectInvalidLongTextFieldErrors(rows, "long cannot save")).toEqual({
      notes: "long cannot save",
    });
  });
});
