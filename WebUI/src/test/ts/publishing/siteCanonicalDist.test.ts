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
  canonicalDistHttpFailure,
  canonicalDistsMatch,
  isValidCanonicalDist,
} from "@/publishing/siteCanonicalDist";

describe("site canonical distribution edit (#5031)", () => {
  it("accepts only pages and sections, ignoring case and surrounding space", () => {
    expect(isValidCanonicalDist(" pages ")).toBe(true);
    expect(isValidCanonicalDist("SECTIONS")).toBe(true);
    expect(isValidCanonicalDist("files")).toBe(false);
    expect(isValidCanonicalDist("")).toBe(false);
    expect(isValidCanonicalDist("   ")).toBe(false);
  });

  it("treats the same destination as unchanged and rejects a switch or blank", () => {
    expect(canonicalDistsMatch("PAGES", " pages ")).toBe(true);
    expect(canonicalDistsMatch("pages", "sections")).toBe(false);
    expect(canonicalDistsMatch("", "pages")).toBe(false);
    expect(canonicalDistsMatch("widgets", "pages")).toBe(false);
  });

  it("keeps 400, 403, and 409 on the failure path", () => {
    expect(canonicalDistHttpFailure(400)).toBe("bad_request");
    expect(canonicalDistHttpFailure(403)).toBe("forbidden");
    expect(canonicalDistHttpFailure(409)).toBe("conflict");
    expect(canonicalDistHttpFailure(500)).toBeNull();
  });
});
