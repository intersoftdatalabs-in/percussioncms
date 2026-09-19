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
  DEFAULT_RUNTIME_PARAM_TYPE,
  emptyRuntimeParam,
  rowsToRuntimeParams,
  runtimeParamsFingerprint,
  runtimeParamsToRows,
} from "../../../../main/ts/api/developer/extensionRuntimeParams";

describe("extensionRuntimeParams", () => {
  it("normalizes arrays, JAXB single objects, and null", () => {
    expect(
      runtimeParamsToRows([{ name: "htmlParams", dataType: "java.util.Map" }]),
    ).toEqual([{ name: "htmlParams", dataType: "java.util.Map", description: "" }]);
    // JAXB unwraps a single-element list to a bare object — must not read as empty.
    expect(runtimeParamsToRows({ name: "solo", dataType: "int" })).toEqual([
      { name: "solo", dataType: "int", description: "" },
    ]);
    expect(runtimeParamsToRows(null)).toEqual([]);
    expect(runtimeParamsToRows(undefined)).toEqual([]);
    expect(emptyRuntimeParam()).toEqual({
      name: "",
      dataType: DEFAULT_RUNTIME_PARAM_TYPE,
      description: "",
    });
  });

  it("round-trips rows and drops blank names so clear-save is an empty list", () => {
    const rows = runtimeParamsToRows([
      { name: " htmlParams ", dataType: "java.util.Map", description: "params" },
    ]);
    rows.push({ name: "  ", dataType: "int", description: "" });
    const list = rowsToRuntimeParams(rows);
    expect(list).toEqual([
      { name: "htmlParams", dataType: "java.util.Map", description: "params" },
    ]);
    expect(rowsToRuntimeParams([])).toEqual([]);
    expect(runtimeParamsFingerprint([])).toBe("[]");
    expect(runtimeParamsFingerprint(rows)).toBe(JSON.stringify(list));
  });
});
