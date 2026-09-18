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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { describe, expect, it } from "vitest";
import {
  DEFAULT_METHOD_RETURN_TYPE,
  methodsFingerprint,
  methodsToRows,
  normalizeMethods,
  rowsToMethods,
} from "../../../../main/ts/api/developer/extensionMethods";

describe("extensionMethods", () => {
  it("flattens JSON arrays, Jackson entry-list, and plain object maps", () => {
    expect(
      normalizeMethods({
        name: "productVersion",
        returnType: "java.lang.String",
        parameters: { name: "n", dataType: "int" },
      }).productVersion,
    ).toEqual({
      name: "productVersion",
      returnType: "java.lang.String",
      description: "",
      parameters: [{ name: "n", dataType: "int", description: "" }],
    });
    expect(
      normalizeMethods([
        { name: "productVersion", returnType: "java.lang.String" },
      ]).productVersion?.name,
    ).toBe("productVersion");
    expect(
      normalizeMethods({
        entry: [
          {
            key: "productVersion",
            value: { name: "productVersion", returnType: "java.lang.String" },
          },
        ],
      }),
    ).toEqual({
      productVersion: {
        name: "productVersion",
        returnType: "java.lang.String",
        description: "",
        parameters: [],
      },
    });
    expect(
      normalizeMethods({
        ExtensionMethod: {
          name: "productVersion",
          returnType: "java.lang.String",
          parameters: { ExtensionParameter: { name: "n", dataType: "int" } },
        },
      }).productVersion,
    ).toEqual({
      name: "productVersion",
      returnType: "java.lang.String",
      description: "",
      parameters: [{ name: "n", dataType: "int", description: "" }],
    });
    expect(
      normalizeMethods({
        productVersion: { description: "ver" },
      }).productVersion,
    ).toEqual({
      name: "productVersion",
      returnType: DEFAULT_METHOD_RETURN_TYPE,
      description: "ver",
      parameters: [],
    });
  });

  it("round-trips rows and drops blank names so clear-save is empty map", () => {
    const rows = methodsToRows({
      productVersion: {
        name: "productVersion",
        returnType: "java.lang.String",
        parameters: [{ name: "n", dataType: "int" }],
      },
    });
    rows.push({ name: "  ", returnType: "java.lang.Object", parameters: [] });
    const map = rowsToMethods(rows);
    expect(Object.keys(map)).toEqual(["productVersion"]);
    expect(map.productVersion.parameters?.[0]?.name).toBe("n");
    expect(methodsFingerprint([])).toBe("{}");
  });
});
