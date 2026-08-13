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

import { afterEach, describe, expect, it, vi } from "vitest";
import { openInEditor } from "../../../main/ts/contentExplorer/openInEditor";

describe("openInEditor (#3330)", () => {
  const originalHref = window.location.href;

  afterEach(() => {
    vi.unstubAllGlobals();
    try {
      window.location.href = originalHref;
    } catch {
      // jsdom may freeze location; ignore
    }
  });

  it("does not navigate to the editor for Folder path items", () => {
    const hrefSpy = vi.fn();
    vi.stubGlobal("location", { href: originalHref });
    Object.defineProperty(window.location, "href", {
      configurable: true,
      get: () => originalHref,
      set: hrefSpy,
    });
    openInEditor({
      id: "16777215-101-1",
      name: "New-Folder",
      path: "/Folders/New-Folder/",
      type: "Folder",
    });
    expect(hrefSpy).not.toHaveBeenCalled();
  });

  it("navigates to the editor for a page path", () => {
    const hrefSpy = vi.fn();
    Object.defineProperty(window.location, "href", {
      configurable: true,
      get: () => originalHref,
      set: hrefSpy,
    });
    openInEditor({
      id: "55",
      name: "Home",
      path: "/Sites/Demo/Home",
      type: "page",
    });
    expect(hrefSpy).toHaveBeenCalled();
    const dest = String(hrefSpy.mock.calls[0][0]);
    expect(dest).toContain("view=editor");
    expect(dest).toContain(encodeURIComponent("/Sites/Demo/Home"));
  });
});
