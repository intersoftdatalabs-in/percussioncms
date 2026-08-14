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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Pure helpers for #3379 Explorer action-toolbar Playwright: unwrap
 * GET /actions/find and detect MENU parents with children.
 */

"use strict";

/**
 * @param {unknown} payload
 * @returns {any[]}
 */
function unwrapFindPayload(payload) {
  if (payload == null) return [];
  if (Array.isArray(payload)) return payload;
  if (typeof payload === "object") {
    const raw =
      payload.ActionMenu ??
      payload.ActionMenuList ??
      payload.actionMenu ??
      payload.actionMenuList;
    if (Array.isArray(raw)) return raw;
    if (raw && typeof raw === "object") return [raw];
  }
  return [];
}

/**
 * @param {unknown} children
 * @returns {any[]}
 */
function unwrapChildren(children) {
  if (children == null) return [];
  if (Array.isArray(children)) return children;
  if (typeof children === "object") {
    const raw =
      children.ActionMenuList ??
      children.ActionMenu ??
      children.actionMenuList ??
      children.actionMenu;
    if (Array.isArray(raw)) return raw;
    if (raw && typeof raw === "object" && raw.name) return [raw];
  }
  return [];
}

/**
 * @param {unknown} payload
 * @returns {{ name: string, childNames: string[] }[]}
 */
function collectMenuParents(payload) {
  const roots = unwrapFindPayload(payload);
  const byId = new Map();
  for (const menu of roots) {
    if (menu && menu.id != null) {
      byId.set(menu.id, menu);
    }
  }
  const childrenOf = new Map();
  function addChild(parentName, childName) {
    if (!parentName || !childName) return;
    const list = childrenOf.get(parentName) || [];
    list.push(childName);
    childrenOf.set(parentName, list);
  }
  for (const menu of roots) {
    if (!menu || !menu.name) continue;
    for (const child of unwrapChildren(menu.children)) {
      addChild(menu.name, child.name);
    }
    if (menu.parentId && byId.has(menu.parentId)) {
      addChild(byId.get(menu.parentId).name, menu.name);
    }
  }
  const parents = [];
  for (const menu of roots) {
    if (!menu || !menu.name) continue;
    const type = String(menu.menuType || "").toUpperCase();
    const kids = childrenOf.get(menu.name) || [];
    if (
      kids.length > 0 &&
      (type === "MENU" || type === "CONTEXTMENU" || type === "DYNAMICMENU")
    ) {
      parents.push({ name: menu.name, childNames: kids });
    }
  }
  return parents;
}

module.exports = {
  unwrapFindPayload,
  unwrapChildren,
  collectMenuParents,
};
