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
import type { PSPathItem } from "../../../main/ts/api/contentExplorer/types";
import { toClipboardItem } from "../../../main/ts/contentExplorer/clipboard/toClipboardItem";

function row(partial: Partial<PSPathItem> & { name: string }): PSPathItem {
  return {
    path: partial.path ?? "",
    ...partial,
  };
}

describe("toClipboardItem (#3551)", () => {
  it("maps site / FSFolder rows used by the Explorer Sites list as folders", () => {
    const site = toClipboardItem(
      row({
        id: "s-1",
        name: "CorporateInvestments",
        path: "/Sites/CorporateInvestments",
        type: "site",
        category: "SITE",
        accessLevel: "ADMIN",
      }),
    );
    expect(site).toEqual({
      id: "s-1",
      path: "/Sites/CorporateInvestments",
      kind: "folder",
      name: "CorporateInvestments",
      sourceAccessLevel: "ADMIN",
    });

    const fsFolder = toClipboardItem(
      row({
        name: "Files",
        path: "/Sites/Demo/Files",
        type: "FSFolder",
        accessLevel: "WRITE",
      }),
    );
    expect(fsFolder?.kind).toBe("folder");
    expect(fsFolder?.id).toBe("/Sites/Demo/Files");
  });

  it("keeps a named Sites row when id and path are omitted", () => {
    const mapped = toClipboardItem(
      row({
        name: "EnterpriseInvestments",
        type: "site",
        category: "site",
      }),
    );
    expect(mapped).not.toBeNull();
    expect(mapped?.id).toBe("EnterpriseInvestments");
    expect(mapped?.path).toBe("EnterpriseInvestments");
    expect(mapped?.kind).toBe("folder");
  });

  it("maps asset category case-insensitively", () => {
    const mapped = toClipboardItem(
      row({
        id: "a-1",
        name: "hero.png",
        path: "/Assets/hero.png",
        type: "rffImage",
        category: "ASSET",
      }),
    );
    expect(mapped?.kind).toBe("asset");
  });

  it("does not treat category resource as an asset (#3552 review)", () => {
    const mapped = toClipboardItem(
      row({
        id: "r-1",
        name: "Home",
        path: "/Sites/Demo/Home",
        type: "percPage",
        category: "RESOURCE",
      }),
    );
    expect(mapped?.kind).toBe("page");
  });

  it("maps page rows as pages", () => {
    const mapped = toClipboardItem(
      row({
        id: "p-1",
        name: "Home",
        path: "/Sites/Demo/Home",
        type: "percPage",
        category: "PAGE",
      }),
    );
    expect(mapped?.kind).toBe("page");
  });

  it("returns null when the row has no id, path, or name", () => {
    expect(
      toClipboardItem({
        name: "   ",
        path: "",
      }),
    ).toBeNull();
  });
});
