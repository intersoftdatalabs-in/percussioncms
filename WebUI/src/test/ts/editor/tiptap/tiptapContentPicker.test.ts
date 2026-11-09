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
  contentPickerSearchRows,
  normalizeManagedId,
  resolveManagedImageRef,
  resolveManagedLinkRef,
} from "../../../../main/ts/editor/tiptap/tiptapContentPicker";

describe("contentPickerSearchRows", () => {
  it("maps search results to selectable rows with rebuilt paths", () => {
    const rows = contentPickerSearchRows([
      { id: "77", title: "About", name: "about", folderPath: "/Sites/News", type: "page" },
      { id: "88", title: "Logo", name: "logo.png", folderPath: "/Assets/Images", type: "asset" },
    ]);
    expect(rows).toEqual([
      {
        key: "item:/Sites/News/about",
        name: "About",
        path: "/Sites/News/about",
        folder: false,
        contentId: 77,
      },
      {
        key: "item:/Assets/Images/logo.png",
        name: "Logo",
        path: "/Assets/Images/logo.png",
        folder: false,
        contentId: 88,
      },
    ]);
  });

  it("drops results without a parseable content id or path", () => {
    expect(
      contentPickerSearchRows([
        { id: "ghost", title: "Ghost", name: "ghost", folderPath: "/Sites" },
        { title: "No id", name: "noid", folderPath: "/Sites" },
        { id: "99", title: "", name: "", folderPath: "" },
      ]),
    ).toEqual([]);
    expect(contentPickerSearchRows(null)).toEqual([]);
    expect(contentPickerSearchRows(undefined)).toEqual([]);
  });
});

describe("normalizeManagedId", () => {
  it("stringifies ids and drops blanks", () => {
    expect(normalizeManagedId(77)).toBe("77");
    expect(normalizeManagedId("512")).toBe("512");
    expect(normalizeManagedId(null)).toBeUndefined();
    expect(normalizeManagedId(undefined)).toBeUndefined();
    expect(normalizeManagedId("  ")).toBeUndefined();
  });
});

describe("resolveManagedLinkRef", () => {
  it("resolves the classic managed-link reference", () => {
    expect(
      resolveManagedLinkRef(
        {
          sys_dependentid: 77,
          sys_dependentvariantid: 512,
          url: "/Sites/About",
          title: "About us",
        },
        "/Sites/About",
      ),
    ).toEqual({
      href: "/Sites/About",
      title: "About us",
      dependentId: "77",
      dependentVariantId: "512",
      jcrPath: "/Sites/About",
    });
  });

  it("returns null without a preview or URL", () => {
    expect(resolveManagedLinkRef(null, "/Sites/About")).toBeNull();
    expect(
      resolveManagedLinkRef({ title: "No url" }, "/Sites/About"),
    ).toBeNull();
  });
});

describe("resolveManagedImageRef", () => {
  it("resolves the classic managed-image reference with alt prefill", () => {
    expect(
      resolveManagedImageRef(
        {
          sys_dependentid: 88,
          url: "/Assets/Images/logo.png",
          altText: "Company logo",
          title: "Logo",
        },
        "/Assets/Images/logo.png",
      ),
    ).toEqual({
      src: "/Assets/Images/logo.png",
      alt: "Company logo",
      title: "Logo",
      dependentId: "88",
      jcrPath: "/Assets/Images/logo.png",
    });
  });

  it("returns null without a preview or URL", () => {
    expect(resolveManagedImageRef(undefined, "/Assets/x.png")).toBeNull();
    expect(resolveManagedImageRef({ url: "  " }, "/Assets/x.png")).toBeNull();
  });
});
