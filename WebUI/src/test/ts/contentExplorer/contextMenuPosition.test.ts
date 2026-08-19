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
import { clampContextMenuPosition } from "../../../main/ts/contentExplorer/contextMenuPosition";

describe("clampContextMenuPosition (#3629)", () => {
  const viewport = { width: 800, height: 600 };

  it("keeps an in-viewport click unchanged", () => {
    expect(clampContextMenuPosition(40, 80, viewport)).toEqual({
      x: 40,
      y: 80,
    });
  });

  it("clamps a click near the bottom-right so the menu stays on screen", () => {
    const pos = clampContextMenuPosition(790, 590, viewport);
    expect(pos.x).toBeLessThanOrEqual(800 - 240 - 8);
    expect(pos.y).toBeLessThanOrEqual(600 - 280 - 8);
    expect(pos.x).toBeGreaterThanOrEqual(8);
    expect(pos.y).toBeGreaterThanOrEqual(8);
  });

  it("does not use negative coordinates", () => {
    expect(clampContextMenuPosition(-20, -4, viewport)).toEqual({
      x: 8,
      y: 8,
    });
  });
});
