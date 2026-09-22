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
  collectUnsafeHtmlFieldErrors,
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
