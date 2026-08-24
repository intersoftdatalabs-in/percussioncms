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
 * ContentBrowser client-side type / category filters (#3714).
 *
 * <p>Hosts such as the asset picker pass {@code allowedTypes: ['page', 'asset']}.
 * CMS search and path rows often expose the <em>content-type name</em>
 * ({@code Image}, {@code percPage}, {@code rffImage}) rather than those
 * host tokens. Exact string equality rejected valid hits and left Confirm
 * disabled. Folders and nav types stay excluded unless the host allows
 * {@code folder}.</p>
 */

import type { PSPathItem } from "../api/contentExplorer/types";
import {
  isAssetContentType,
  isFolder,
  isPageOrAssetContentType,
} from "../contentExplorer/selection";

/**
 * Host {@code page} token — stock / FastForward page content-type names.
 * Customer types are accepted via {@link isPageOrAssetContentType} when the
 * host allows both page and asset.
 */
const PAGE_KIND_ALIASES = new Set([
  "page",
  "percpage",
  "landing_page",
  "landingpage",
  "rffhome",
  "rffevent",
  "rffgeneric",
  "rffgenericword",
  "rffbrief",
  "rffcalendar",
  "rffcontacts",
  "rffpressrelease",
  "rffexternallink",
  "rffautoindex",
]);

/**
 * Host {@code asset} token — stock / FastForward asset names plus CMS
 * display names search returns ({@code Image}, {@code File}).
 */
const ASSET_KIND_ALIASES = new Set([
  "asset",
  "percasset",
  "image",
  "file",
  "percimage",
  "percfile",
  "rffimage",
  "rfffile",
  "rffnavimage",
]);

function tokenSet(values: ReadonlyArray<string> | null): Set<string> {
  const out = new Set<string>();
  if (!values) {
    return out;
  }
  for (const raw of values) {
    const t = String(raw ?? "")
      .trim()
      .toLowerCase();
    if (t) {
      out.add(t);
    }
  }
  return out;
}

function itemTypeTokens(item: PSPathItem): string[] {
  const type = (item.type ?? "").trim().toLowerCase();
  const category = (item.category ?? "").trim().toLowerCase();
  const tokens: string[] = [];
  if (type) {
    tokens.push(type);
  }
  if (category && category !== type) {
    tokens.push(category);
  }
  return tokens;
}

function hasAny(haystack: ReadonlySet<string>, needles: readonly string[]): boolean {
  return needles.some((n) => haystack.has(n));
}

function isStockAssetKind(item: PSPathItem): boolean {
  return hasAny(ASSET_KIND_ALIASES, itemTypeTokens(item));
}

function isStockPageKind(item: PSPathItem): boolean {
  return hasAny(PAGE_KIND_ALIASES, itemTypeTokens(item));
}

/**
 * Whether {@code item} matches a host {@code allowedTypes} list.
 *
 * <p>{@code page} / {@code asset} are object-class tokens. CMS content-type
 * names map onto those classes. Exact type/category match is still accepted
 * so a host that lists {@code Image} (or a customer type) keeps working.</p>
 */
export function matchesAllowedTypes(
  item: PSPathItem,
  allowedTypes: ReadonlyArray<string> | null,
): boolean {
  const allowed = tokenSet(allowedTypes);
  if (allowed.size === 0) {
    return true;
  }

  const itemTokens = itemTypeTokens(item);
  if (itemTokens.some((t) => allowed.has(t))) {
    return true;
  }

  const allowsFolder = allowed.has("folder");
  if (isFolder(item)) {
    return allowsFolder;
  }

  const allowsPage = allowed.has("page");
  const allowsAsset = allowed.has("asset");

  if (allowsPage && allowsAsset) {
    return isPageOrAssetContentType(item);
  }

  if (allowsAsset && (isStockAssetKind(item) || isAssetContentType(item))) {
    return true;
  }

  if (allowsPage) {
    if (isStockAssetKind(item) || isAssetContentType(item)) {
      return false;
    }
    return isStockPageKind(item) || isPageOrAssetContentType(item);
  }

  return false;
}

/**
 * Client-side selection filter used by ContentBrowser toggle / activate /
 * SearchPanel Open. Type mapping is in {@link matchesAllowedTypes}; category
 * remains an exact (case-insensitive) match when the host sets
 * {@code allowedCategories}.
 */
export function passesFilters(
  item: PSPathItem,
  allowedTypes: ReadonlyArray<string> | null,
  allowedCategories: ReadonlyArray<string> | null,
): boolean {
  if (!matchesAllowedTypes(item, allowedTypes)) {
    return false;
  }
  const categories = tokenSet(allowedCategories);
  if (categories.size === 0) {
    return true;
  }
  const c = (item.category ?? item.type ?? "").trim().toLowerCase();
  return categories.has(c);
}
