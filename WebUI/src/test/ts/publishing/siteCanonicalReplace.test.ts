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
  canonicalReplaceHttpFailure,
  canonicalReplaceLabel,
  parseCanonicalReplace,
} from "@/publishing/siteCanonicalReplace";

describe("siteCanonicalReplace", () => {
  it("parses booleans and common wire strings", () => {
    expect(parseCanonicalReplace(true)).toBe(true);
    expect(parseCanonicalReplace(false)).toBe(false);
    expect(parseCanonicalReplace(" YES ")).toBe(true);
    expect(parseCanonicalReplace("no")).toBe(false);
    expect(parseCanonicalReplace("maybe")).toBeNull();
    expect(parseCanonicalReplace(undefined)).toBeNull();
  });

  it("labels yes, no, and missing", () => {
    expect(canonicalReplaceLabel(true)).toBe("yes");
    expect(canonicalReplaceLabel(false)).toBe("no");
    expect(canonicalReplaceLabel(null)).toBe("—");
  });

  it("maps only 400, 403, and 409", () => {
    expect(canonicalReplaceHttpFailure(400)).toBe("bad_request");
    expect(canonicalReplaceHttpFailure(403)).toBe("forbidden");
    expect(canonicalReplaceHttpFailure(409)).toBe("conflict");
    expect(canonicalReplaceHttpFailure(500)).toBeNull();
  });
});
