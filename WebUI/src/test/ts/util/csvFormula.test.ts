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

/**
 * Shared spreadsheet-formula-injection neutralization (#6175).
 */

import { describe, expect, it } from "vitest";
import {
  CSV_FORMULA_TRIGGER_CHARS,
  neutralizeCsvFormulaCell,
} from "../../../main/ts/util/csvFormula";

describe("neutralizeCsvFormulaCell", () => {
  it("prefixes a single-quote text marker on formula-trigger leads", () => {
    expect(neutralizeCsvFormulaCell("=cmd|' /C calc'!A0")).toBe(
      "'=cmd|' /C calc'!A0",
    );
    expect(neutralizeCsvFormulaCell("=1+1")).toBe("'=1+1");
    expect(neutralizeCsvFormulaCell("+1+1")).toBe("'+1+1");
    expect(neutralizeCsvFormulaCell("-1+1")).toBe("'-1+1");
    expect(neutralizeCsvFormulaCell("@SUM(A1:A2)")).toBe("'@SUM(A1:A2)");
    expect(neutralizeCsvFormulaCell("\t=1+1")).toBe("'\t=1+1");
    expect(neutralizeCsvFormulaCell("\r=1+1")).toBe("'\r=1+1");
  });

  it("leaves non-trigger values byte-identical", () => {
    expect(neutralizeCsvFormulaCell("")).toBe("");
    expect(neutralizeCsvFormulaCell("Cherry")).toBe("Cherry");
    expect(neutralizeCsvFormulaCell("  =1+1")).toBe("  =1+1");
    expect(neutralizeCsvFormulaCell("1+1")).toBe("1+1");
    expect(neutralizeCsvFormulaCell("'=1+1")).toBe("'=1+1");
    expect(neutralizeCsvFormulaCell("a,b")).toBe("a,b");
  });

  it("covers exactly the trigger set", () => {
    expect([...CSV_FORMULA_TRIGGER_CHARS].sort()).toEqual(
      ["\t", "\r", "+", "-", "=", "@"].sort(),
    );
  });
});
