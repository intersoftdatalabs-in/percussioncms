/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
  formatProgressLabel,
  progressPercent,
} from "@/publishing/progressUtils";

describe("progressPercent", () => {
  it("returns null for invalid totals", () => {
    expect(progressPercent(1, 0)).toBeNull();
    expect(progressPercent(null, 10)).toBeNull();
  });

  it("clamps 0-100", () => {
    expect(progressPercent(0, 10)).toBe(0);
    expect(progressPercent(5, 10)).toBe(50);
    expect(progressPercent(15, 10)).toBe(100);
  });
});

describe("formatProgressLabel", () => {
  it("formats percent and dash", () => {
    expect(formatProgressLabel(1, 4)).toContain("25%");
    expect(formatProgressLabel(null, null)).toBe("—");
  });
});
