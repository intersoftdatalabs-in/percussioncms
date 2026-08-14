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
import type { NavTreeNode } from "../../../main/ts/api/architecture/types";
import {
  buildNavParentMap,
  collectVisibleNavNodes,
  isNavTreeRovingKey,
  resolveNavTreeKey,
} from "../../../main/ts/architecture/navTreeKeyboard";

const tree: NavTreeNode = {
  id: "root",
  title: "Home",
  folderPath: "//Sites/Demo",
  sectionType: "section",
  requiresLogin: false,
  children: [
    {
      id: "about",
      title: "About",
      folderPath: "//Sites/Demo/About",
      sectionType: "section",
      requiresLogin: false,
      children: [
        {
          id: "team",
          title: "Team",
          folderPath: "//Sites/Demo/About/Team",
          sectionType: "section",
          requiresLogin: false,
          children: [],
        },
      ],
    },
    {
      id: "members",
      title: "Members",
      folderPath: "//Sites/Demo/Members",
      sectionType: "section",
      requiresLogin: true,
      children: [],
    },
  ],
};

const expandedAll = { root: true, about: true };

describe("navTreeKeyboard helpers (#3354)", () => {
  it("does not treat Tab as a tree-owned key (no trap)", () => {
    expect(isNavTreeRovingKey("Tab")).toBe(false);
    expect(isNavTreeRovingKey("Shift")).toBe(false);
    expect(resolveNavTreeKey("Tab", tree, tree, expandedAll)).toEqual({
      action: "none",
    });
    expect(resolveNavTreeKey("Escape", tree, tree, expandedAll)).toEqual({
      action: "none",
    });
  });

  it("owns Arrow / Home / End / Enter / Space", () => {
    expect(isNavTreeRovingKey("ArrowDown")).toBe(true);
    expect(isNavTreeRovingKey("Home")).toBe(true);
    expect(isNavTreeRovingKey("End")).toBe(true);
    expect(isNavTreeRovingKey("Enter")).toBe(true);
    expect(isNavTreeRovingKey(" ")).toBe(true);
  });

  it("collects visible nodes in document order", () => {
    const collapsed = collectVisibleNavNodes(tree, { root: false });
    expect(collapsed.map((n) => n.id)).toEqual(["root"]);

    const rootOnly = collectVisibleNavNodes(tree, { root: true });
    expect(rootOnly.map((n) => n.id)).toEqual(["root", "about", "members"]);

    const all = collectVisibleNavNodes(tree, expandedAll);
    expect(all.map((n) => n.id)).toEqual(["root", "about", "team", "members"]);
  });

  it("maps parents for ArrowLeft", () => {
    const map = buildNavParentMap(tree);
    expect(map.get("root")).toBeNull();
    expect(map.get("about")).toBe("root");
    expect(map.get("team")).toBe("about");
    expect(map.get("members")).toBe("root");
  });

  it("moves focus with Arrow Up/Down and Home/End", () => {
    const about = tree.children[0];
    expect(resolveNavTreeKey("ArrowDown", tree, tree, expandedAll)).toEqual({
      action: "focus",
      id: "about",
    });
    expect(resolveNavTreeKey("ArrowUp", about, tree, expandedAll)).toEqual({
      action: "focus",
      id: "root",
    });
    expect(resolveNavTreeKey("Home", about, tree, expandedAll)).toEqual({
      action: "focus",
      id: "root",
    });
    expect(resolveNavTreeKey("End", about, tree, expandedAll)).toEqual({
      action: "focus",
      id: "members",
    });
  });

  it("expands, enters child, and collapses with Arrow Right/Left", () => {
    const about = tree.children[0];
    expect(
      resolveNavTreeKey("ArrowRight", about, tree, { root: true, about: false }),
    ).toEqual({ action: "expand", id: "about" });
    expect(resolveNavTreeKey("ArrowRight", about, tree, expandedAll)).toEqual({
      action: "focus",
      id: "team",
    });
    expect(resolveNavTreeKey("ArrowLeft", about, tree, expandedAll)).toEqual({
      action: "collapse",
      id: "about",
    });
    expect(
      resolveNavTreeKey("ArrowLeft", tree.children[1], tree, { root: true }),
    ).toEqual({ action: "focus", id: "root" });
  });

  it("selects and toggles branches on Enter/Space", () => {
    expect(resolveNavTreeKey("Enter", tree, tree, expandedAll)).toEqual({
      action: "select",
      id: "root",
      toggleExpand: true,
    });
    expect(
      resolveNavTreeKey(" ", tree.children[1], tree, expandedAll),
    ).toEqual({
      action: "select",
      id: "members",
      toggleExpand: false,
    });
  });

  it("consumes arrows at the boundary without moving", () => {
    expect(resolveNavTreeKey("ArrowUp", tree, tree, expandedAll)).toEqual({
      action: "prevent",
    });
    expect(
      resolveNavTreeKey("ArrowDown", tree.children[1], tree, expandedAll),
    ).toEqual({ action: "prevent" });
    expect(resolveNavTreeKey("ArrowLeft", tree, tree, { root: false })).toEqual(
      { action: "prevent" },
    );
  });
});
