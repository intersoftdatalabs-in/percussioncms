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
 * <p><strong>Wire-format notes (verified 2026-07-20 against the live
 * docker dev CMS at {@code http://localhost:9992}):</strong></p>
 * <ul>
 *   <li>{@code /actions/find} returns {@code {"ActionMenu": [...]}}.
 *     The resource method signature is {@code List<ActionMenu>} but
 *     Jackson honors {@code @XmlRootElement(name = "ActionMenu")} on
 *     the {@code ActionMenu} DTO and wraps the array under that key.</li>
 *   <li>{@code /actions/find/types} and {@code /actions/find/templates/{id}}
 *     both return {@code {"ActionMenuList": [...]}}. Their resource
 *     methods return the explicit {@code ActionMenuList} type (extends
 *     {@code ArrayList<ActionMenu>}, carries
 *     {@code @XmlRootElement(name = "ActionMenuList")}).</li>
 * </ul>
 * <p>The functions below unwrap these envelopes so callers receive the
 * typed {@link ActionMenu[]} surface. The wrapper keys derive from the
 * server DTOs (no invented field names).</p>
 */

import { get, post } from "../client";
import { PATHS } from "../paths";
import type {
  ActionMenu,
  ActionMenuListEnvelope,
  AllowedContentTypeMenusRequest,
  MenuAction,
} from "./types";

// ---------- Wire envelopes (internal) ----------

/**
 * Wire shape for {@code findActions} (path {@code /actions/find}). The
 * returned list is wrapped under the {@code ActionMenu} key by Jackson
 * honoring the DTO's {@code @XmlRootElement}.
 */
interface ActionMenuListResponse {
  ActionMenu?: ActionMenu[];
}

/** Internal alias for the {@code ActionMenuList} wire envelope. */
interface ActionMenuListEnvelopeResponse extends ActionMenuListEnvelope {}

/** Internal alias matching the {@code POST /actions/find/types} envelope. */
interface AllowedContentTypeMenusResponse extends ActionMenuListEnvelope {}

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
  const res = await get<ActionMenuListResponse>(
    `${PATHS.ACTIONS_ROOT}/find${qs ? `?${qs}` : ""}`,
  );
  return res?.ActionMenu ?? [];
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
  const res = await post<AllowedContentTypeMenusResponse>(
    `${PATHS.ACTIONS_ROOT}/find/types`,
    body,
  );
  return res?.ActionMenuList ?? [];
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
  const res = await get<ActionMenuListEnvelopeResponse>(
    `${PATHS.ACTIONS_ROOT}/find/templates/${encodeURIComponent(
      String(contentId),
    )}?isAA=${String(isAA)}`,
  );
  return res?.ActionMenuList ?? [];
}

// ---------- Client-side mapping ----------

/**
 * Wraps the {@link ActionMenu[]} envelope from the rest layer's
 * {@code find*()} responses into a flattened {@link MenuAction[]} for
 * the UI (US3 T053 / T054).
 *
 * <p>Children under {@link ActionMenu.children} (the {@code ActionMenuList}
 * envelope on cascading menus) are unwrapped recursively and sorted by
 * {@code sortRank}. The mapper is pure (no side effects, no fetch) and
 * is the natural unit-test surface.</p>
 */
export function mapActionMenusToMenuActions(
  menus: ActionMenu[],
): MenuAction[] {
  return (menus ?? [])
    .slice()
    .sort((a, b) => a.sortRank - b.sortRank)
    .map(mapSingleActionMenu);
}

function mapSingleActionMenu(menu: ActionMenu): MenuAction {
  const childActions: MenuAction[] = menu.children?.ActionMenuList
    ? mapActionMenusToMenuActions(menu.children.ActionMenuList)
    : [];
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
