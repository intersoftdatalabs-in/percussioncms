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
  bindingsEqual,
  cloneBindings,
  normalizeBindingsForSave,
  validateBindings,
} from "../../../main/ts/design/templateBindings";

describe("templateBindings helpers (#2809)", () => {
  it("cloneBindings copies fields and defaults empty strings", () => {
    const src = [
      { executionOrder: 2, variable: "a", expression: "1" },
      { variable: undefined as unknown as string, expression: undefined as unknown as string },
    ];
    const cloned = cloneBindings(src);
    expect(cloned).toEqual([
      { executionOrder: 2, variable: "a", expression: "1" },
      { executionOrder: undefined, variable: "", expression: "" },
    ]);
    cloned[0].variable = "changed";
    expect(src[0].variable).toBe("a");
  });

  it("bindingsEqual detects length and field differences", () => {
    const a = [{ executionOrder: 1, variable: "x", expression: "1" }];
    expect(bindingsEqual(a, [{ ...a[0] }])).toBe(true);
    expect(bindingsEqual(a, [])).toBe(false);
    expect(
      bindingsEqual(a, [{ executionOrder: 1, variable: "x", expression: "2" }]),
    ).toBe(false);
  });

  it("validateBindings requires variable and expression per row", () => {
    expect(validateBindings([])).toBeNull();
    expect(
      validateBindings([{ variable: "v", expression: "e" }]),
    ).toBeNull();
    expect(validateBindings([{ variable: "  ", expression: "e" }])).toBe(
      "bindings[0].variable is required",
    );
    expect(validateBindings([{ variable: "v", expression: "" }])).toBe(
      "bindings[0].expression is required",
    );
    expect(
      validateBindings([
        { variable: "ok", expression: "1" },
        { variable: "y", expression: "  " },
      ]),
    ).toBe("bindings[1].expression is required");
  });

  it("normalizeBindingsForSave trims and assigns default order", () => {
    expect(
      normalizeBindingsForSave([
        { variable: "  $x  ", expression: " 1 " },
        { executionOrder: 5, variable: "y", expression: "2" },
      ]),
    ).toEqual([
      { executionOrder: 1, variable: "$x", expression: "1" },
      { executionOrder: 5, variable: "y", expression: "2" },
    ]);
  });
});
