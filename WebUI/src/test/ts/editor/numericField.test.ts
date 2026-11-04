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
  collectDecimalNulFieldErrors,
  collectInvalidNumericFieldErrors,
  collectNumberNulFieldErrors,
  numberContainsNul,
  numericFieldProblem,
  numericMetaForSchema,
} from "../../../main/ts/editor/numericField";

describe("numericFieldProblem", () => {
  it("accepts a blank or in-range integer and a float fraction", () => {
    expect(numericFieldProblem("", { integer: true, minimum: "0", maximum: "10" })).toBeNull();
    expect(numericFieldProblem("   ", { integer: true, minimum: "0", maximum: "10" })).toBeNull();
    expect(numericFieldProblem("10", { integer: true, minimum: "0", maximum: "10" })).toBeNull();
    expect(numericFieldProblem("1.5", { integer: false, minimum: "0", maximum: "2" })).toBeNull();
    expect(numericFieldProblem("", { integer: false, minimum: "0", maximum: "2" })).toBeNull();
    expect(numericFieldProblem("   ", { integer: false })).toBeNull();
  });

  it("rejects non-numeric text and values outside the inclusive bounds", () => {
    expect(numericFieldProblem("abc", { integer: true, minimum: "0", maximum: "10" })).toBe(
      "invalid",
    );
    expect(numericFieldProblem("1.5", { integer: true })).toBe("invalid");
    expect(numericFieldProblem("-1", { integer: true, minimum: "0", maximum: "10" })).toBe(
      "range",
    );
    expect(numericFieldProblem("11", { integer: true, minimum: "0", maximum: "10" })).toBe(
      "range",
    );
    expect(numericFieldProblem("0", { integer: true, minimum: "0", maximum: "10" })).toBeNull();
    expect(numericFieldProblem("-0.1", { integer: false, minimum: "0", maximum: "2" })).toBe(
      "range",
    );
    expect(numericFieldProblem("2.1", { integer: false, minimum: "0", maximum: "2" })).toBe(
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
    expect(
      numericMetaForSchema({
        control: "sys_Number",
        dataType: "float",
        controlProperties: [
          { name: "min", value: "0" },
          { name: "max", value: "2" },
        ],
      }),
    ).toEqual({ integer: false, minimum: "0", maximum: "2" });
    expect(numericMetaForSchema({ control: "sys_EditBox", dataType: "text" })).toBeNull();
  });
});

describe("numberContainsNul", () => {
  it("rejects an embedded NUL and accepts a valid integer, decimal, or empty clear", () => {
    expect(numberContainsNul("4")).toBe(false);
    expect(numberContainsNul("1.5")).toBe(false);
    expect(numberContainsNul("")).toBe(false);
    expect(numberContainsNul("   ")).toBe(false);
    expect(numberContainsNul("4\u0000")).toBe(true);
    expect(numberContainsNul("1.5\u0000")).toBe(true);
    expect(numberContainsNul("\u0000")).toBe(true);
  });
});

describe("collectNumberNulFieldErrors", () => {
  it("flags only whole-number rows and leaves float NUL and empty required for other gates", () => {
    expect(
      collectNumberNulFieldErrors(
        [
          { name: "qty", kind: "number", value: "4\u0000", numericInteger: true },
          { name: "ok", kind: "number", value: "4", numericInteger: true },
          { name: "empty", kind: "number", value: "", numericInteger: true },
          { name: "rate", kind: "number", value: "1.5\u0000", numericInteger: false },
          { name: "title", kind: "text", value: "z\u0000" },
        ],
        "cannot save",
      ),
    ).toEqual({ qty: "cannot save" });
  });
});

describe("collectDecimalNulFieldErrors", () => {
  it("flags only float rows and leaves whole-number NUL and empty required for other gates", () => {
    expect(
      collectDecimalNulFieldErrors(
        [
          { name: "rate", kind: "number", value: "1.5\u0000", numericInteger: false },
          { name: "ok", kind: "number", value: "1.5", numericInteger: false },
          { name: "empty", kind: "number", value: "", numericInteger: false },
          { name: "qty", kind: "number", value: "4\u0000", numericInteger: true },
          { name: "title", kind: "text", value: "z\u0000" },
        ],
        "cannot save",
      ),
    ).toEqual({ rate: "cannot save" });
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

  it("maps a below-minimum and an above-maximum value to the range message", () => {
    const errors = collectInvalidNumericFieldErrors(
      [
        {
          name: "low",
          kind: "number",
          value: "-1",
          numericInteger: true,
          numericMinimum: "0",
          numericMaximum: "10",
        },
        {
          name: "high",
          kind: "number",
          value: "11",
          numericInteger: true,
          numericMinimum: "0",
          numericMaximum: "10",
        },
      ],
      "bad",
      "outside",
    );
    expect(errors).toEqual({ low: "outside", high: "outside" });
  });

  it("accepts a cleared optional float without flagging a sibling integer", () => {
    const errors = collectInvalidNumericFieldErrors(
      [
        { name: "rate", kind: "number", value: "", numericInteger: false },
        {
          name: "qty",
          kind: "number",
          value: "4",
          numericInteger: true,
          numericMinimum: "0",
          numericMaximum: "10",
        },
      ],
      "bad",
      "outside",
    );
    expect(errors).toEqual({});
  });
});
