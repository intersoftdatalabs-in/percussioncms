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
  canPostReplaceLandingPage,
  resolveLandingPagePick,
} from "../../../main/ts/architecture/landingPagePicker";

describe("landingPagePicker (#3304)", () => {
  it("resolves a page selection to id and label", () => {
    expect(
      resolveLandingPagePick({
        items: [
          {
            id: "  page-1  ",
            name: "About",
            path: "//Sites/Demo/About",
            type: "page",
          },
        ],
      }),
    ).toEqual({ ok: true, id: "page-1", label: "About" });
  });

  it("treats missing or blank id as empty (do not POST)", () => {
    expect(resolveLandingPagePick(null)).toEqual({
      ok: false,
      error: "empty",
    });
    expect(resolveLandingPagePick({ items: [] })).toEqual({
      ok: false,
      error: "empty",
    });
    expect(resolveLandingPagePick({ items: [{ id: "  " }] })).toEqual({
      ok: false,
      error: "empty",
    });
  });

  it("rejects folder and asset picks", () => {
    expect(
      resolveLandingPagePick({
        items: [{ id: "f1", name: "Folder", type: "folder" }],
      }),
    ).toEqual({ ok: false, error: "notPage" });
    expect(
      resolveLandingPagePick({
        items: [{ id: "a1", name: "Image", category: "asset" }],
      }),
    ).toEqual({ ok: false, error: "notPage" });
  });

  it("accepts percPage-style type names and id-only items", () => {
    expect(
      resolveLandingPagePick({
        items: [{ id: "p2", name: "Home", type: "percPage" }],
      }),
    ).toEqual({ ok: true, id: "p2", label: "Home" });
    expect(resolveLandingPagePick({ items: [{ id: "p3" }] })).toEqual({
      ok: true,
      id: "p3",
      label: "p3",
    });
  });

  it("blocks replace POST without section or page id", () => {
    expect(canPostReplaceLandingPage("sec", "page")).toBe(true);
    expect(canPostReplaceLandingPage("  ", "page")).toBe(false);
    expect(canPostReplaceLandingPage("sec", "")).toBe(false);
    expect(canPostReplaceLandingPage(null, "page")).toBe(false);
  });
});
