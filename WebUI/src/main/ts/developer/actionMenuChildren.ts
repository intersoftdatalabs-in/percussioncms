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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import type { ActionMenu } from "../api/developer/types";
import {
  ACTION_MENU_TYPE_MENU,
  REST_USER_MENU_PROP,
} from "../api/developer/actionMenusApi";

/** Identity sent on PUT /services/actions/{idOrName}/children. */
export type ActionMenuChildRef = Pick<ActionMenu, "name"> & {
  id?: number;
  guidString?: string;
};

export function normalizeActionMenuType(menuType: string | undefined | null): string {
  return menuType == null ? "" : menuType.trim().toUpperCase();
}

/**
 * REST cascading parent: type MENU with a blank URL (UI-04 children PUT).
 */
export function isCascadingActionMenu(opts: {
  menuType?: string | null;
  url?: string | null;
}): boolean {
  return normalizeActionMenuType(opts.menuType) === ACTION_MENU_TYPE_MENU && !(opts.url || "").trim();
}

function propertyValue(menu: ActionMenu | null | undefined, name: string): string {
  const props = menu?.properties;
  if (!Array.isArray(props)) {
    return "";
  }
  const key = name.toLowerCase();
  for (const p of props) {
    if ((p.name || "").trim().toLowerCase() === key) {
      return (p.value || "").trim();
    }
  }
  return "";
}

/** REST-created user menus carry {@code sys_restUserMenu=yes}. */
export function isRestUserActionMenu(menu: ActionMenu | null | undefined): boolean {
  return propertyValue(menu, REST_USER_MENU_PROP).toLowerCase() === "yes";
}

/**
 * Packaged Workbench {@code Menus/System} names used to fail-closed the
 * children composer when GET omits {@code sys_restUserMenu}. Unique REST
 * user names are not in this list.
 */
export const SYSTEM_ACTION_MENU_NAMES: readonly string[] = [
  "Edit",
  "Copy",
  "Paste",
  "Open",
  "Delete",
  "Checkout",
  "Checkin",
  "Preview",
];

export function isKnownSystemActionMenuName(name: string | undefined | null): boolean {
  const key = (name || "").trim().toLowerCase();
  if (!key) {
    return false;
  }
  return SYSTEM_ACTION_MENU_NAMES.some((n) => n.toLowerCase() === key);
}

export function actionMenuChildKey(menu: {
  name?: string | null;
  id?: number;
  guidString?: string | null;
}): string {
  const name = (menu.name || "").trim().toLowerCase();
  if (name) {
    return `n:${name}`;
  }
  if (menu.id != null && menu.id > 0) {
    return `i:${menu.id}`;
  }
  const g = (menu.guidString || "").trim().toLowerCase();
  if (g) {
    return `g:${g}`;
  }
  return "";
}

export function childrenOrderSignature(children: ActionMenuChildRef[]): string {
  return children.map((c) => actionMenuChildKey(c)).join("\n");
}

export function childrenOrderEqual(a: ActionMenuChildRef[], b: ActionMenuChildRef[]): boolean {
  return childrenOrderSignature(a) === childrenOrderSignature(b);
}

export function hasActionMenuChild(children: ActionMenuChildRef[], candidate: ActionMenuChildRef): boolean {
  const key = actionMenuChildKey(candidate);
  if (!key) {
    return false;
  }
  return children.some((c) => actionMenuChildKey(c) === key);
}

export function flattenActionMenus(menus: ActionMenu[]): ActionMenu[] {
  const out: ActionMenu[] = [];
  const seen = new Set<string>();
  const walk = (items: ActionMenu[]): void => {
    for (const m of items) {
      const key = actionMenuChildKey(m);
      if (key) {
        if (seen.has(key)) {
          walk(Array.isArray(m.children) ? m.children : []);
          continue;
        }
        seen.add(key);
      }
      out.push(m);
      walk(Array.isArray(m.children) ? m.children : []);
    }
  };
  walk(menus);
  return out;
}

export function catalogsNotInChildren(
  catalog: ActionMenu[],
  children: ActionMenuChildRef[],
  parent: ActionMenuChildRef,
): ActionMenu[] {
  const parentKey = actionMenuChildKey(parent);
  return flattenActionMenus(catalog).filter((row) => {
    const key = actionMenuChildKey(row);
    if (!key || key === parentKey) {
      return false;
    }
    return !hasActionMenuChild(children, row);
  });
}

export function addActionMenuChild(
  children: ActionMenuChildRef[],
  candidate: ActionMenu,
): ActionMenuChildRef[] {
  const name = (candidate.name || "").trim();
  if (!name || hasActionMenuChild(children, candidate)) {
    return children;
  }
  const row: ActionMenuChildRef = { name };
  if (candidate.id != null && candidate.id > 0) {
    row.id = candidate.id;
  }
  if (candidate.guidString) {
    row.guidString = candidate.guidString;
  }
  return [...children, row];
}

export function removeActionMenuChild(children: ActionMenuChildRef[], index: number): ActionMenuChildRef[] {
  if (index < 0 || index >= children.length) {
    return children;
  }
  return children.filter((_, i) => i !== index);
}

export function moveActionMenuChild(
  children: ActionMenuChildRef[],
  index: number,
  delta: -1 | 1,
): ActionMenuChildRef[] {
  const next = index + delta;
  if (index < 0 || index >= children.length || next < 0 || next >= children.length) {
    return children;
  }
  const copy = children.slice();
  const [row] = copy.splice(index, 1);
  copy.splice(next, 0, row);
  return copy;
}

export function toChildWriteBody(children: ActionMenuChildRef[]): ActionMenuChildRef[] {
  return children.map((c) => {
    const name = (c.name || "").trim();
    const row: ActionMenuChildRef = { name };
    if (c.id != null && c.id > 0) {
      row.id = c.id;
    }
    return row;
  });
}

/**
 * Children composer is writable for REST user cascading MENU parents only.
 * System / packaged parents stay read-only (Save disabled; PUT would be 409).
 */
export function isActionMenuChildrenWritable(opts: {
  isNew: boolean;
  isRestUser: boolean;
  menuType?: string | null;
  url?: string | null;
}): boolean {
  if (opts.isNew || !opts.isRestUser) {
    return false;
  }
  return isCascadingActionMenu({ menuType: opts.menuType, url: opts.url });
}
