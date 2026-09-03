/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * you may obtain a copy of the License at
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
import type { ActionMenu } from "../../../main/ts/api/developer/types";
import {
  addActionMenuChild,
  catalogsNotInChildren,
  childrenOrderEqual,
  flattenActionMenus,
  isActionMenuChildrenWritable,
  isCascadingActionMenu,
  isKnownSystemActionMenuName,
  isRestUserActionMenu,
  moveActionMenuChild,
  removeActionMenuChild,
  toChildWriteBody,
} from "../../../main/ts/developer/actionMenuChildren";

const parent: ActionMenu = {
  name: "ParentMenu",
  menuType: "MENU",
  url: "",
  properties: [{ name: "sys_restUserMenu", value: "yes" }],
};

const childA: ActionMenu = { name: "ChildA", label: "A", id: 11 };
const childB: ActionMenu = { name: "ChildB", label: "B", id: 12 };

describe("isCascadingActionMenu", () => {
  it("requires MENU type and blank URL", () => {
    expect(isCascadingActionMenu({ menuType: "MENU", url: "" })).toBe(true);
    expect(isCascadingActionMenu({ menuType: "menu", url: "  " })).toBe(true);
    expect(isCascadingActionMenu({ menuType: "MENUITEM", url: "" })).toBe(false);
    expect(isCascadingActionMenu({ menuType: "MENU", url: "/Rhythmyx/x" })).toBe(false);
  });
});

describe("isRestUserActionMenu", () => {
  it("reads sys_restUserMenu=yes", () => {
    expect(isRestUserActionMenu(parent)).toBe(true);
    expect(isRestUserActionMenu({ name: "Edit", properties: [] })).toBe(false);
    expect(isRestUserActionMenu({ name: "Edit" })).toBe(false);
  });
});

describe("isKnownSystemActionMenuName", () => {
  it("matches packaged Edit/Copy and not unique REST names", () => {
    expect(isKnownSystemActionMenuName("Edit")).toBe(true);
    expect(isKnownSystemActionMenuName("copy")).toBe(true);
    expect(isKnownSystemActionMenuName("qa4206pabcd")).toBe(false);
  });
});

describe("isActionMenuChildrenWritable", () => {
  it("is writable only for persisted REST user cascading MENU", () => {
    expect(
      isActionMenuChildrenWritable({
        isNew: true,
        isRestUser: true,
        menuType: "MENU",
        url: "",
      }),
    ).toBe(false);
    expect(
      isActionMenuChildrenWritable({
        isNew: false,
        isRestUser: false,
        menuType: "MENU",
        url: "",
      }),
    ).toBe(false);
    expect(
      isActionMenuChildrenWritable({
        isNew: false,
        isRestUser: true,
        menuType: "MENUITEM",
        url: "",
      }),
    ).toBe(false);
    expect(
      isActionMenuChildrenWritable({
        isNew: false,
        isRestUser: true,
        menuType: "MENU",
        url: "",
      }),
    ).toBe(true);
  });
});

describe("child list mutations", () => {
  it("adds, refuses duplicates, reorders, and removes", () => {
    const one = addActionMenuChild([], childA);
    expect(one.map((c) => c.name)).toEqual(["ChildA"]);
    expect(addActionMenuChild(one, childA)).toEqual(one);
    const two = addActionMenuChild(one, childB);
    expect(two.map((c) => c.name)).toEqual(["ChildA", "ChildB"]);
    const swapped = moveActionMenuChild(two, 1, -1);
    expect(swapped.map((c) => c.name)).toEqual(["ChildB", "ChildA"]);
    expect(removeActionMenuChild(swapped, 0).map((c) => c.name)).toEqual(["ChildA"]);
    expect(moveActionMenuChild(two, 0, -1)).toEqual(two);
  });

  it("detects order equality and write body names", () => {
    const a = addActionMenuChild(addActionMenuChild([], childA), childB);
    const b = addActionMenuChild(addActionMenuChild([], childA), childB);
    expect(childrenOrderEqual(a, b)).toBe(true);
    expect(childrenOrderEqual(a, moveActionMenuChild(b, 1, -1))).toBe(false);
    expect(toChildWriteBody(a)).toEqual([
      { name: "ChildA", id: 11 },
      { name: "ChildB", id: 12 },
    ]);
  });

  it("flattens nested catalog children and omits parent plus in-use rows", () => {
    const nested: ActionMenu[] = [
      { name: "ParentMenu", children: [childA] },
      { name: "Other", children: [childB] },
    ];
    expect(flattenActionMenus(nested).map((m) => m.name)).toEqual([
      "ParentMenu",
      "ChildA",
      "Other",
      "ChildB",
    ]);
    const available = catalogsNotInChildren(nested, [{ name: "ChildA" }], parent);
    expect(available.map((m) => m.name)).toEqual(["Other", "ChildB"]);
  });
});
