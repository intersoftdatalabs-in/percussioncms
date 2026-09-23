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
  collectInvalidNumericFieldErrors,
  numericFieldProblem,
  numericMetaForSchema,
} from "../../../main/ts/editor/numericField";

describe("numericFieldProblem", () => {
  it("accepts a blank or in-range integer and a float fraction", () => {
    expect(numericFieldProblem("", { integer: true, minimum: "0", maximum: "10" })).toBeNull();
    expect(numericFieldProblem("10", { integer: true, minimum: "0", maximum: "10" })).toBeNull();
    expect(numericFieldProblem("1.5", { integer: false, minimum: "0", maximum: "2" })).toBeNull();
  });

  it("rejects non-numeric text and values outside the inclusive bounds", () => {
    expect(numericFieldProblem("abc", { integer: true, minimum: "0", maximum: "10" })).toBe(
      "invalid",
    );
    expect(numericFieldProblem("1.5", { integer: true })).toBe("invalid");
    expect(numericFieldProblem("11", { integer: true, minimum: "0", maximum: "10" })).toBe(
      "range",
    );
    expect(numericFieldProblem("9223372036854775808", { integer: true })).toBe("range");
  });
});

describe("numericMetaForSchema", () => {
  it("reads sys_Number bounds from control properties", () => {
    expect(
      numericMetaForSchema({
        control: "sys_Number",
        dataType: "integer",
        controlProperties: [
          { name: "minimum", value: "0" },
          { name: "maximum", value: "10" },
        ],
      }),
    ).toEqual({ integer: true, minimum: "0", maximum: "10" });
    expect(numericMetaForSchema({ control: "sys_EditBox", dataType: "text" })).toBeNull();
  });
});

describe("collectInvalidNumericFieldErrors", () => {
  it("maps invalid and range messages onto the field name", () => {
    const errors = collectInvalidNumericFieldErrors(
      [
        {
          name: "qty",
          kind: "number",
          value: "nope",
          numericInteger: true,
          numericMinimum: "0",
          numericMaximum: "10",
        },
        { name: "sys_title", kind: "text", value: "Home" },
      ],
      "bad",
      "range",
    );
    expect(errors).toEqual({ qty: "bad" });
  });
});
