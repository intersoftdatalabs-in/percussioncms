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
 * Pure helpers for the React content-picker lookup behind the TipTap
 * {@code percadvlink} / {@code percadvimage} dialogs (#5900 / parent #5622).
 *
 * <p>The classic adv dialogs resolve a picked repository path in two steps —
 * {@code PercPathService.getPathItemForPath} then
 * {@code getInlineRenderLink(id)} — and persist the returned managed-link
 * reference ({@code sys_dependentid}, {@code sys_dependentvariantid},
 * {@code url}, {@code title}, plus the picked {@code data-jcrpath}) through
 * the same stored attribute shape the TipTap builders emit. These helpers
 * map the React browse/search rows onto that reference; the dialog component
 * ({@code ContentPickerDialog}) owns fetching and rendering.</p>
 */

import type { PSInlineRenderLinkWire } from "../../api/contentExplorer/renderLinkApi";
import { parseExplorerContentId } from "../../api/contentExplorer/pathItemId";
import type { PSItemProperties } from "../../api/contentExplorer/types";
import type { RelatedFolderRow } from "../editorRelatedFolderPick";

/** Browse roots the picker offers (peer of the classic content browser). */
export const CONTENT_PICKER_ROOTS = ["/Sites", "/Assets"] as const;

/** Default browse root when no path is set. */
export const CONTENT_PICKER_DEFAULT_ROOT = "/Sites";

/** Search page size for the picker lookup. */
export const CONTENT_PICKER_SEARCH_MAX_RESULTS = 25;

/**
 * Map search rows onto picker rows. Search results carry the item id plus
 * the owning folder ({@code folderPath}) and name — the full repository
 * path is rebuilt from those two, mirroring how the classic image dialog
 * derives {@code data-jcrpath} from {@code folderPaths + name}. Results
 * without a parseable content id are dropped so a title can never be
 * posted as an id (same rule as {@code relatedFolderRows}).
 */
export function contentPickerSearchRows(
  results: readonly PSItemProperties[] | null | undefined,
): RelatedFolderRow[] {
  if (results == null) {
    return [];
  }
  const rows: RelatedFolderRow[] = [];
  for (const result of results) {
    const contentId = parseExplorerContentId(result.id);
    if (contentId == null) {
      continue;
    }
    const name = String(result.title ?? result.name ?? "").trim();
    const folder = String(result.folderPath ?? "").trim();
    const leaf = String(result.name ?? "").trim();
    const path = joinPickerPath(folder, leaf || name);
    if (!path || !name) {
      continue;
    }
    rows.push({
      key: `item:${path}`,
      name,
      path,
      folder: false,
      contentId,
    });
  }
  return rows;
}

function joinPickerPath(folder: string, leaf: string): string {
  const folderPart = folder.replace(/\/+$/, "");
  const leafPart = leaf.replace(/^\/+/, "");
  if (!folderPart) {
    return leafPart ? `/${leafPart}` : "";
  }
  if (!leafPart) {
    return folderPart.startsWith("/") ? folderPart : `/${folderPart}`;
  }
  const joined = `${folderPart}/${leafPart}`;
  return joined.startsWith("/") ? joined : `/${joined}`;
}

/** Normalize a server dependent id to the string form the builders persist. */
export function normalizeManagedId(
  value: number | string | null | undefined,
): string | undefined {
  if (value == null) {
    return undefined;
  }
  const text = String(value).trim();
  return text ? text : undefined;
}

/** Managed-link reference the adv link dialog persists. */
export interface ManagedLinkRef {
  href: string;
  title?: string;
  dependentId?: string;
  dependentVariantId?: string;
  jcrPath?: string;
}

/** Managed-image reference the adv image dialog persists. */
export interface ManagedImageRef {
  src: string;
  alt?: string;
  title?: string;
  dependentId?: string;
  dependentVariantId?: string;
  jcrPath?: string;
}

/**
 * Resolve the picked item's render-link preview onto the managed-link
 * reference classic {@code percadvlink} persists: {@code href} from the
 * preview URL, the preview title, the dependent ids, and the picked
 * repository path as {@code data-jcrpath}.
 *
 * @returns the reference, or {@code null} when the preview carries no
 *   usable URL (callers keep manual-URL entry).
 */
export function resolveManagedLinkRef(
  renderLink: PSInlineRenderLinkWire | null | undefined,
  jcrPath: string | null | undefined,
): ManagedLinkRef | null {
  if (renderLink == null) {
    return null;
  }
  const href = String(renderLink.url ?? "").trim();
  if (!href) {
    return null;
  }
  const ref: ManagedLinkRef = { href };
  const title = String(renderLink.title ?? "").trim();
  if (title) {
    ref.title = title;
  }
  const dependentId = normalizeManagedId(renderLink.sys_dependentid);
  if (dependentId) {
    ref.dependentId = dependentId;
  }
  const dependentVariantId = normalizeManagedId(
    renderLink.sys_dependentvariantid,
  );
  if (dependentVariantId) {
    ref.dependentVariantId = dependentVariantId;
  }
  const path = String(jcrPath ?? "").trim();
  if (path) {
    ref.jcrPath = path;
  }
  return ref;
}

/**
 * Resolve the picked item's render-link preview onto the managed-image
 * reference classic {@code percadvimage} persists: {@code src} from the
 * preview URL with the preview alt/title prefill, the dependent id, and
 * the picked repository path as {@code data-jcrpath}.
 *
 * @returns the reference, or {@code null} when the preview carries no
 *   usable URL (callers keep manual-URL entry).
 */
export function resolveManagedImageRef(
  renderLink: PSInlineRenderLinkWire | null | undefined,
  jcrPath: string | null | undefined,
): ManagedImageRef | null {
  if (renderLink == null) {
    return null;
  }
  const src = String(renderLink.url ?? "").trim();
  if (!src) {
    return null;
  }
  const ref: ManagedImageRef = { src };
  const alt = String(renderLink.altText ?? "").trim();
  if (alt) {
    ref.alt = alt;
  }
  const title = String(renderLink.title ?? "").trim();
  if (title) {
    ref.title = title;
  }
  const dependentId = normalizeManagedId(renderLink.sys_dependentid);
  if (dependentId) {
    ref.dependentId = dependentId;
  }
  const dependentVariantId = normalizeManagedId(
    renderLink.sys_dependentvariantid,
  );
  if (dependentVariantId) {
    ref.dependentVariantId = dependentVariantId;
  }
  const path = String(jcrPath ?? "").trim();
  if (path) {
    ref.jcrPath = path;
  }
  return ref;
}
