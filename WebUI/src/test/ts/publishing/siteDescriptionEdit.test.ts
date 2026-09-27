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
  descriptionsMatch,
  siteDescriptionHttpFailure,
} from "@/publishing/siteDescriptionEdit";

describe("site description edit (#4977)", () => {
  it("treats blank and whitespace-only drafts as unchanged", () => {
    expect(descriptionsMatch("", "   ")).toBe(true);
    expect(descriptionsMatch("  Nightly  ", "Nightly")).toBe(true);
    expect(descriptionsMatch("Nightly", "Other")).toBe(false);
  });

  it("keeps 400, 403, and 409 on the failure path", () => {
    expect(siteDescriptionHttpFailure(400)).toBe("bad_request");
    expect(siteDescriptionHttpFailure(403)).toBe("forbidden");
    expect(siteDescriptionHttpFailure(409)).toBe("conflict");
    expect(siteDescriptionHttpFailure(500)).toBeNull();
  });
});
