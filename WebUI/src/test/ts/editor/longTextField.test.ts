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
  collectInvalidLongTextFieldErrors,
  longTextContainsNul,
} from "../../../main/ts/editor/longTextField";

describe("longTextContainsNul", () => {
  it("accepts multiline text and rejects an embedded NUL", () => {
    expect(longTextContainsNul("line one\nline two")).toBe(false);
    expect(longTextContainsNul("bad\u0000value")).toBe(true);
  });
});

describe("collectInvalidLongTextFieldErrors", () => {
  it("flags only long-text rows", () => {
    const errors = collectInvalidLongTextFieldErrors(
      [
        { name: "sys_title", kind: "text", value: "bad\u0000" },
        { name: "description", kind: "longtext", value: "ok\nmore" },
        { name: "notes", kind: "longtext", value: "x\u0000y" },
      ],
      "cannot save",
    );
    expect(errors).toEqual({ notes: "cannot save" });
  });
});
