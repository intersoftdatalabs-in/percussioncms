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
  collectYesNoNulFieldErrors,
  yesNoContainsNul,
} from "../../../main/ts/editor/yesnoField";

describe("yesNoContainsNul", () => {
  it("rejects an embedded NUL and accepts yes, no, or empty clear", () => {
    expect(yesNoContainsNul("yes")).toBe(false);
    expect(yesNoContainsNul("no")).toBe(false);
    expect(yesNoContainsNul("")).toBe(false);
    expect(yesNoContainsNul("   ")).toBe(false);
    expect(yesNoContainsNul("yes\u0000")).toBe(true);
    expect(yesNoContainsNul("no\u0000")).toBe(true);
    expect(yesNoContainsNul("\u0000")).toBe(true);
  });
});

describe("collectYesNoNulFieldErrors", () => {
  it("flags only yes-no rows and leaves number NUL and empty required for other gates", () => {
    expect(
      collectYesNoNulFieldErrors(
        [
          { name: "no_propagate", kind: "yesno", value: "yes\u0000" },
          { name: "ok", kind: "yesno", value: "yes" },
          { name: "empty", kind: "yesno", value: "" },
          { name: "qty", kind: "number", value: "4\u0000" },
          { name: "title", kind: "text", value: "z\u0000" },
        ],
        "cannot save",
      ),
    ).toEqual({ no_propagate: "cannot save" });
  });
});
