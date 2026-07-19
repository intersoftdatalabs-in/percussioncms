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
  isAllowlistedSection,
  knownSectionAliases,
  mapIdParam,
  mapSectionParam,
} from "@/publishing/deepLinkMap";

describe("mapSectionParam", () => {
  it("defaults empty to sites", () => {
    expect(mapSectionParam(null)).toBe("sites");
    expect(mapSectionParam("")).toBe("sites");
    expect(mapSectionParam("unknown")).toBe("sites");
  });

  it("maps status logs design runtime and aliases", () => {
    expect(mapSectionParam("status")).toBe("status");
    expect(mapSectionParam("LOGS")).toBe("logs");
    expect(mapSectionParam("design")).toBe("design");
    expect(mapSectionParam("editions")).toBe("runtime");
    expect(mapSectionParam("servers")).toBe("sites");
  });

  it("allowlists known sections", () => {
    expect(isAllowlistedSection("status")).toBe(true);
    expect(isAllowlistedSection("nope")).toBe(false);
    expect(knownSectionAliases()).toEqual(
      expect.arrayContaining(["status", "design", "runtime"]),
    );
  });
});

describe("mapIdParam", () => {
  it("accepts safe ids and rejects injection", () => {
    expect(mapIdParam("site-1")).toBe("site-1");
    expect(mapIdParam("abc_DEF")).toBe("abc_DEF");
    expect(mapIdParam("<script>")).toBe("");
    expect(mapIdParam("a/b")).toBe("");
  });
});
