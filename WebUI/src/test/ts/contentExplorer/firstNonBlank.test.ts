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
import { firstNonBlank } from "../../../main/ts/contentExplorer/firstNonBlank";

describe("firstNonBlank (#3557)", () => {
  it("returns the first non-whitespace string or number", () => {
    expect(firstNonBlank(null, "  ", 42, "later")).toBe("42");
    expect(firstNonBlank(undefined, "", "home")).toBe("home");
  });

  it("returns null when every value is blank", () => {
    expect(firstNonBlank(null, undefined, "  ", "")).toBeNull();
    expect(firstNonBlank()).toBeNull();
  });
});
