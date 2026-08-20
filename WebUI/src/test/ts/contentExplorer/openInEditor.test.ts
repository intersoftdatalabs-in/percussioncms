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

  it("opens the React editor host for a page path", () => {
    const openSpy = vi.fn();
    vi.spyOn(window, "open").mockImplementation(openSpy);
    openInEditor({
      id: "55",
      name: "Home",
      path: "/Sites/Demo/Home",
      type: "page",
    });
    expect(openSpy).toHaveBeenCalled();
    const dest = String(openSpy.mock.calls[0]?.[0] ?? "");
    expect(dest).toContain("entry=editor");
    expect(dest).toContain("contentId=55");
    expect(dest).not.toContain("view=editor");
  });

  it("does not open leftover CM1 editor for folders even with a path (#3638)", () => {
    const openWindow = vi.fn();
    const findByPath = vi.fn();
    openInEditor(
      {
        id: "ci-folder",
        name: "Pages",
        path: "/Sites/Demo/Pages/",
        type: "Folder",
      },
      { openWindow, findByPath },
    );
    expect(openWindow).not.toHaveBeenCalled();
    expect(findByPath).not.toHaveBeenCalled();
  });

  it("resolves an unparseable id via path to spa.jsp?entry=editor (#3638)", async () => {
    const openWindow = vi.fn().mockReturnValue({});
    const findByPath = vi.fn().mockResolvedValue({ id: "1-101-88" });
    openInEditor(
      {
        id: "ci-home",
        name: "Home",
        path: "/Sites/Demo/Home",
        type: "page",
      },
      { openWindow, findByPath },
    );
    await vi.waitFor(() => expect(openWindow).toHaveBeenCalled());
    expect(findByPath).toHaveBeenCalledWith("/Sites/Demo/Home");
    const dest = String(openWindow.mock.calls[0]?.[0] ?? "");
    expect(dest).toContain("entry=editor");
    expect(dest).toContain("contentId=88");
    expect(dest).not.toContain("view=editor");
    expect(dest).not.toContain("editAsset.jsp");
  });
});
