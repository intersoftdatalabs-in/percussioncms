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
  headContentForSave,
  headContentMatch,
  siteAdditionalHeadHttpFailure,
} from "@/publishing/siteAdditionalHeadEdit";

describe("siteAdditionalHeadEdit", () => {
  it("treats blank as a clear and trims stored markup", () => {
    expect(headContentForSave("   ")).toBe("");
    expect(headContentForSave("  <meta>  ")).toBe("<meta>");
    expect(headContentMatch("", "   ")).toBe(true);
    expect(headContentMatch("  <meta>  ", "<meta>")).toBe(true);
    expect(headContentMatch("<meta>", "<link>")).toBe(false);
  });

  it("maps only 400, 403, and 409", () => {
    expect(siteAdditionalHeadHttpFailure(400)).toBe("bad_request");
    expect(siteAdditionalHeadHttpFailure(403)).toBe("forbidden");
    expect(siteAdditionalHeadHttpFailure(409)).toBe("conflict");
    expect(siteAdditionalHeadHttpFailure(500)).toBeNull();
  });
});
