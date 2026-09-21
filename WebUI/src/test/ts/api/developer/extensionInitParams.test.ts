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
  EXTENSION_VERSION_PARAM,
  initParamsFingerprint,
  initParamsToRows,
  mergeInitParametersForWrite,
  rowsToInitParams,
} from "../../../../main/ts/api/developer/extensionInitParams";
import { wrapExtensionForWire } from "../../../../main/ts/api/developer/extensionsApi";

describe("extensionInitParams", () => {
  it("omits reserved className and version from dialog rows", () => {
    expect(
      initParamsToRows({
        className: "com.example.X",
        [EXTENSION_VERSION_PARAM]: "1",
        "com.percussion.user.description": "hello",
      }),
    ).toEqual([{ key: "com.percussion.user.description", value: "hello" }]);
  });

  it("drops blank keys and reserved keys from rows", () => {
    expect(
      rowsToInitParams([
        { key: " ", value: "x" },
        { key: "className", value: "com.example.Hacked" },
        { key: "com.percussion.user.description", value: "d" },
      ]),
    ).toEqual({ "com.percussion.user.description": "d" });
  });

  it("marks removed extra keys as null on write so REST deletes them", () => {
    const merged = mergeInitParametersForWrite({
      previous: {
        className: "com.example.X",
        [EXTENSION_VERSION_PARAM]: "1",
        "com.percussion.user.description": "old",
        extra: "keep-me-not",
      },
      className: "com.example.Y",
      rows: [{ key: "com.percussion.user.description", value: "new" }],
    });
    expect(merged.className).toBe("com.example.Y");
    expect(merged[EXTENSION_VERSION_PARAM]).toBe("1");
    expect(merged["com.percussion.user.description"]).toBe("new");
    expect(merged.extra).toBeUndefined();
    expect(initParamsFingerprint([{ key: "com.percussion.user.description", value: "new" }])).toBe(
      JSON.stringify({ "com.percussion.user.description": "new" }),
    );
  });

  it("wraps null init-parameter values on the Jackson entry list", () => {
    expect(
      wrapExtensionForWire({
        extensionName: "my_user_ext",
        supportedInterfaces: ["com.percussion.extension.IPSUdfProcessor"],
        initParameters: {
          className: "com.example.X",
          extra: null,
        },
      }),
    ).toEqual(
      expect.objectContaining({
        Extension: expect.objectContaining({
          initParameters: {
            entry: [
              { key: "className", value: "com.example.X" },
              { key: "extra", value: null },
            ],
          },
        }),
      }),
    );
  });
});
