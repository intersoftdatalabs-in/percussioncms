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

/**
 * Allowed content types for Explorer New Item when the host is invoked
 * without a type leaf (#3513). Prefers action-menu children, then
 * {@code POST /actions/find/types}, then {@code GET /contenttypes}.
 */

import {
  findAllowedContentTypeMenus,
  mapActionMenusToMenuActions,
} from "../api/contentExplorer/actionMenuApi";
import type { MenuAction } from "../api/contentExplorer/types";
import { listContentTypes } from "../api/developer/contentTypesApi";
import { isNewItemHostName } from "./menuCatalogLoad";

export interface ContentTypeChoice {
  name: string;
  label: string;
}

export function isNewItemHostActionName(name: string | undefined | null): boolean {
  const n = (name ?? "").replace(/[\s-]/g, "_").toLowerCase();
  return n === "create_new_item" || isNewItemHostName(name);
}

export function contentTypeChoicesFromActions(
  actions: MenuAction[] | undefined | null,
): ContentTypeChoice[] {
  const out: ContentTypeChoice[] = [];
  const seen = new Set<string>();
  for (const action of actions ?? []) {
    const name = (action?.name ?? "").trim();
    if (!name || isNewItemHostActionName(name)) {
      continue;
    }
    if (seen.has(name)) {
      continue;
    }
    seen.add(name);
    out.push({
      name,
      label: (action.label ?? "").trim() || name,
    });
  }
  return out;
}

export async function loadAllowedContentTypes(): Promise<ContentTypeChoice[]> {
  try {
    const menus = await findAllowedContentTypeMenus([]);
    const fromMenus = contentTypeChoicesFromActions(
      mapActionMenusToMenuActions(menus),
    );
    if (fromMenus.length > 0) {
      return fromMenus;
    }
  } catch {
    /* fall through to the content-types catalog */
  }
  try {
    const types = await listContentTypes();
    const out: ContentTypeChoice[] = [];
    const seen = new Set<string>();
    for (const type of types) {
      const name = (type.name ?? "").trim();
      if (!name || seen.has(name)) {
        continue;
      }
      seen.add(name);
      out.push({
        name,
        label: (type.label ?? "").trim() || name,
      });
    }
    return out;
  } catch {
    return [];
  }
}
