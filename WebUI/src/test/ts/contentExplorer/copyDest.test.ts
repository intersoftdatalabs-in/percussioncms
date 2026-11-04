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
  copyDestContainsNul,
  copyDestIsBlank,
} from "../../../main/ts/contentExplorer/copyDest";

describe("copyDestContainsNul (#5551)", () => {
  it("is true only for U+0000", () => {
    expect(copyDestContainsNul("/Assets/Dst")).toBe(false);
    expect(copyDestContainsNul("")).toBe(false);
    expect(copyDestContainsNul("  ")).toBe(false);
    expect(copyDestContainsNul("/Assets/Dst\u0000")).toBe(true);
    expect(copyDestContainsNul("/Assets/\u0000Dst")).toBe(true);
    expect(copyDestContainsNul("\u0000")).toBe(true);
    expect(copyDestContainsNul(" \u0000 ")).toBe(true);
  });
});

describe("copyDestIsBlank (#5553)", () => {
  it("is true for empty and whitespace-only paths", () => {
    expect(copyDestIsBlank("")).toBe(true);
    expect(copyDestIsBlank("  ")).toBe(true);
    expect(copyDestIsBlank("\t\n")).toBe(true);
    expect(copyDestIsBlank("/Assets/Dst")).toBe(false);
    expect(copyDestIsBlank("\u0000")).toBe(false);
  });
});
