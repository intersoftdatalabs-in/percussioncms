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
import { pageBasedHttpFailure, pageBasedLabel, parsePageBased } from "@/publishing/sitePageBased";

describe("sitePageBased", () => {
  it("parses booleans and common wire strings", () => {
    expect(parsePageBased(true)).toBe(true);
    expect(parsePageBased(false)).toBe(false);
    expect(parsePageBased(" YES ")).toBe(true);
    expect(parsePageBased("f")).toBe(false);
    expect(parsePageBased(null)).toBeNull();
    expect(parsePageBased("maybe")).toBeNull();
  });

  it("labels only known booleans", () => {
    expect(pageBasedLabel(true)).toBe("yes");
    expect(pageBasedLabel(false)).toBe("no");
    expect(pageBasedLabel(null)).toBe("—");
  });

  it("maps only 400, 403, and 409", () => {
    expect(pageBasedHttpFailure(400)).toBe("bad_request");
    expect(pageBasedHttpFailure(403)).toBe("forbidden");
    expect(pageBasedHttpFailure(409)).toBe("conflict");
    expect(pageBasedHttpFailure(500)).toBeNull();
  });
});
