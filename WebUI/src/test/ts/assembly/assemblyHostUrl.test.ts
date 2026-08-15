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
  assemblyWindowName,
  buildAssemblyHostUrl,
  parsePositiveInt,
  withCmsContextPrefix,
} from "../../../main/ts/assembly/assemblyHostUrl";

describe("assemblyHostUrl", () => {
  it("builds the spa.jsp query contract with optional template", () => {
    expect(buildAssemblyHostUrl(42)).toBe(
      "/cm/app/spa.jsp?entry=assembly&contentId=42",
    );
    expect(buildAssemblyHostUrl(42, 7)).toBe(
      "/cm/app/spa.jsp?entry=assembly&contentId=42&templateId=7",
    );
    expect(buildAssemblyHostUrl(42, 0)).toBe(
      "/cm/app/spa.jsp?entry=assembly&contentId=42",
    );
  });

  it("names the popup per content id", () => {
    expect(assemblyWindowName(42)).toBe("percAssembly_42");
  });

  it("parses positive ints only", () => {
    expect(parsePositiveInt("7")).toBe(7);
    expect(parsePositiveInt("0")).toBeNull();
    expect(parsePositiveInt("-1")).toBeNull();
    expect(parsePositiveInt("x")).toBeNull();
    expect(parsePositiveInt(null)).toBeNull();
  });

  it("prefixes /Rhythmyx when the SPA is under that context", () => {
    expect(withCmsContextPrefix("/cm/app/spa.jsp?entry=assembly")).toBe(
      "/cm/app/spa.jsp?entry=assembly",
    );
  });
});
