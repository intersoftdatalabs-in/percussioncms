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
  knownLegacyInitialScreens,
  mapInitialScreenToSection,
} from "@/home/deepLinkMap";

describe("mapInitialScreenToSection", () => {
  it("maps legacy library to library", () => {
    expect(mapInitialScreenToSection("library")).toBe("library");
  });

  it("maps list to recent", () => {
    expect(mapInitialScreenToSection("list")).toBe("recent");
  });

  it("maps search and newitem", () => {
    expect(mapInitialScreenToSection("search")).toBe("search");
    expect(mapInitialScreenToSection("newitem")).toBe("create");
  });

  it("defaults unknown and empty to recent", () => {
    expect(mapInitialScreenToSection(null)).toBe("recent");
    expect(mapInitialScreenToSection("")).toBe("recent");
    expect(mapInitialScreenToSection("not-a-screen")).toBe("recent");
  });

  it("accepts modern section names including bookmarks and gadgets", () => {
    expect(mapInitialScreenToSection("create")).toBe("create");
    expect(mapInitialScreenToSection("bookmarks")).toBe("bookmarks");
    expect(mapInitialScreenToSection("gadgets")).toBe("gadgets");
  });

  it("maps former dashboard aliases to gadgets (PR-7)", () => {
    expect(mapInitialScreenToSection("dash")).toBe("gadgets");
    expect(mapInitialScreenToSection("dashboard")).toBe("gadgets");
    expect(mapInitialScreenToSection("widgets")).toBe("gadgets");
  });

  it("exposes known legacy screens", () => {
    expect(knownLegacyInitialScreens()).toEqual(
      expect.arrayContaining(["library", "list", "search", "newitem", "dash"]),
    );
  });
});
