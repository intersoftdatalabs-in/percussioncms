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
 * Typed client for the action-menu REST surface used by the modern React
 * Content Explorer (feature 992-react-content-explorer, US3 P-Menu).
 *
 * <p>Provider: {@code rest} module
 * {@code com.percussion.rest.actions.ActionMenuResource} at
 * {@code GET /Rhythmyx/rest/actions/*}. Contract: see
 * {@code specs/992-react-content-explorer/contracts/action-menu-api.md}.
 * Server DTOs: {@code ActionMenu}, {@code ActionMenuList},
 * {@code ActionMenuParameter}, {@code ActionMenuProperty},
 * {@code ActionMenuVisibilityContext}, {@code ActionMenuModeUIContext},
 * {@code AllowedContentTypeMenusRequest}.</p>
 *
 * <p>This module is intentionally thin: it maps the documented contract
 * to a typed TS surface and delegates transport to {@link get} /
 * {@link post} (CSRF + JSON + error normalization). It does
 * <em>not</em> invent fields — when a new server field is required,
 * align types to the live DTOs per constitution II (Evidence Over
 * Invention).</p>
 *
 * <p><strong>Wire-format notes (verified 2026-07-20 / rechecked 2026-08-11
 * on QA H2):</strong></p>
 * <ul>
 *   <li>{@code /actions/find} typically returns {@code {"ActionMenu": [...]}}
 *     (Jackson honors {@code @XmlRootElement(name = "ActionMenu")} on the
 *     DTO). Some serializers may emit a raw array — both are accepted.</li>
 *   <li>{@code /actions/find/types} and {@code /actions/find/templates/{id}}
 *     typically return {@code {"ActionMenuList": [...]}} (explicit
 *     {@code ActionMenuList} type). Raw arrays and the {@code ActionMenu}
 *     key are also accepted so the Explorer toolbar does not go empty on
 *     wire-shape drift (#2972).</li>
 * </ul>
 * <p>Use {@link unwrapActionMenuListPayload} for all list endpoints so
 * callers always receive a typed {@link ActionMenu[]} surface.</p>
 */

import { get, post } from "../client";
import { PATHS } from "../paths";
import type {
  ActionMenu,
  AllowedContentTypeMenusRequest,
  MenuAction,
} from "./types";

// ---------- Wire envelopes (internal) ----------

/**
 * Normalize Jackson list wire shapes for action-menu endpoints.
 *
 * <p>Live CMS ({@code GET /actions/find}) wraps under {@code ActionMenu}.
 * {@code ActionMenuList} subclasses and some serializers emit a raw array
 * or the {@code ActionMenuList} key. Accept all forms so the Explorer
 * toolbar never silently goes empty on a wire-shape drift (#2972).</p>
 */
export function unwrapActionMenuListPayload(payload: unknown): ActionMenu[] {
  if (payload == null) {
    return [];
  }
  if (Array.isArray(payload)) {
    return payload as ActionMenu[];
  }
  if (typeof payload === "object") {
    const obj = payload as Record<string, unknown>;
    const raw =
      obj.ActionMenu ??
      obj.ActionMenuList ??
      obj.actionMenu ??
      obj.actionMenuList;
    if (raw == null) {
      return [];
    }
    if (Array.isArray(raw)) {
      return raw as ActionMenu[];
    }
    // Single object under the envelope key.
    if (typeof raw === "object") {
      return [raw as ActionMenu];
    }
  }
  return [];
}

// ---------- Public API ----------

export interface FindActionsParams {
  name?: string;
  label?: string;
  dynamic?: boolean;
  item?: boolean;
  cascading?: boolean;
}

/**
 * List action menus that match the criteria.
 *
 * <p>Server: {@code GET /actions/find}. Returns the configured action
 * menu tree (cascading children appear under each returned
 * {@link ActionMenu.children} envelope).</p>
 */
export async function findActions(
  params: FindActionsParams = {},
): Promise<ActionMenu[]> {
  const q = new URLSearchParams();
  if (params.name !== undefined) q.set("name", params.name);
  if (params.label !== undefined) q.set("label", params.label);
  if (params.dynamic !== undefined) q.set("dynamic", String(params.dynamic));
  if (params.item !== undefined) q.set("item", String(params.item));
  if (params.cascading !== undefined)
    q.set("cascading", String(params.cascading));
  const qs = q.toString();
  const res = await get<unknown>(
    `${PATHS.ACTIONS_ROOT}/find${qs ? `?${qs}` : ""}`,
  );
  return unwrapActionMenuListPayload(res);
}

/**
 * List action menus allowed for the given content ids (used to populate
 * the workflow / per-item menu).
 *
 * <p>Server: {@code POST /actions/find/types} with body
 * {@link AllowedContentTypeMenusRequest}. Returns the wrappers list of
 * menus allowed for the supplied contentIds.</p>
 */
export async function findAllowedContentTypeMenus(
  contentIds: number[],
): Promise<ActionMenu[]> {
  const body: AllowedContentTypeMenusRequest = { contentIds };
  const res = await post<unknown>(`${PATHS.ACTIONS_ROOT}/find/types`, body);
  return unwrapActionMenuListPayload(res);
}

/**
 * List action menus for templates allowed for the given content id.
 *
 * <p>Server: {@code GET /actions/find/templates/{id}} with optional
 * {@code isAA} query parameter. Returns the wrappers list of template
 * menus.</p>
 */
export async function findAllowedTemplateMenus(
  contentId: number,
  isAA = false,
): Promise<ActionMenu[]> {
  const res = await get<unknown>(
    `${PATHS.ACTIONS_ROOT}/find/templates/${encodeURIComponent(
      String(contentId),
    )}?isAA=${String(isAA)}`,
  );
  return unwrapActionMenuListPayload(res);
}

// ---------- Client-side mapping ----------

/**
 * Wraps the {@link ActionMenu[]} envelope from the rest layer's
 * {@code find*()} responses into a client {@link MenuAction[]} for
 * the UI (US3 T053 / T054).
 *
 * <p>Children under {@link ActionMenu.children} are unwrapped recursively
 * and sorted by {@code sortRank}. Nested parents remain nested so
 * {@code ActionToolbar} can render MENU dropdowns (#2730) rather than a
 * flat button dump. The mapper is pure (no side effects, no fetch).</p>
 *
 * <p>Wire-shape note: Jackson typically serializes an {@code ActionMenuList}
 * field as a JSON <em>array</em>. Root list responses still wrap under
 * {@code ActionMenuList} / {@code ActionMenu}. Accept both nested forms.</p>
 */
export function mapActionMenusToMenuActions(
  menus: ActionMenu[],
): MenuAction[] {
  return (menus ?? [])
    .slice()
    .sort((a, b) => a.sortRank - b.sortRank)
    .map(mapSingleActionMenu);
}

/**
 * Unwrap nested children whether Jackson emitted a raw array or an
 * {@code ActionMenuList} / {@code ActionMenu} envelope object.
 */
export function unwrapActionMenuChildren(
  children: ActionMenu["children"] | unknown,
): ActionMenu[] {
  if (children == null) {
    return [];
  }
  if (Array.isArray(children)) {
    return children as ActionMenu[];
  }
  if (typeof children === "object") {
    const env = children as Record<string, unknown>;
    if (Array.isArray(env.ActionMenuList)) {
      return env.ActionMenuList as ActionMenu[];
    }
    if (Array.isArray(env.ActionMenu)) {
      return env.ActionMenu as ActionMenu[];
    }
  }
  return [];
}

function mapSingleActionMenu(menu: ActionMenu): MenuAction {
  const childActions: MenuAction[] = mapActionMenusToMenuActions(
    unwrapActionMenuChildren(menu.children),
  );
  const result: MenuAction = {
    name: menu.name,
    label: menu.label ?? menu.name,
    description: menu.description,
    url: menu.url,
    handler: menu.handler,
    sortRank: menu.sortRank,
    menuType: menu.menuType,
    parameters: menu.parameters,
  };
  if (childActions.length > 0) {
    result.children = childActions;
  }
  return result;
}
