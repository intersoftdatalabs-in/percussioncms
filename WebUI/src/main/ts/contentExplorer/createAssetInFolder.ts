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

import type { AssetTypeSummary } from "../api/home/types";
import { isExplorerPageType } from "../editor/pageTemplates";
import { resolveAssetCreateContentType } from "../home/create/AssetWizard";

export type CreateAssetFieldError = "missing_name" | "invalid_name" | "missing_type";

export interface AssetContentTypeChoice {
  /** Widget id (stable option key). */
  id: string;
  /** Content type posted to itemmanagement create. */
  contentType: string;
  label: string;
}

/**
 * Asset widget types only. Page types are Create Page, not this dialog.
 * Order follows the asset-type catalog.
 */
export function assetContentTypeChoices(
  types: readonly AssetTypeSummary[] | null | undefined,
): AssetContentTypeChoice[] {
  const out: AssetContentTypeChoice[] = [];
  const seen = new Set<string>();
  for (const type of types ?? []) {
    const contentType = resolveAssetCreateContentType(type);
    const id = (type.id ?? "").trim() || contentType;
    if (!contentType || !id || seen.has(contentType) || isExplorerPageType(contentType)) {
      continue;
    }
    seen.add(contentType);
    const label = (type.label ?? "").trim() || (type.name ?? "").trim() || contentType;
    out.push({ id, contentType, label });
  }
  return out;
}

/**
 * Client gate before POST. Blank name, path separators, or a blank content
 * type are not a create.
 */
export function validateCreateAssetFields(
  name: string | null | undefined,
  contentType: string | null | undefined,
): CreateAssetFieldError | null {
  const n = (name ?? "").trim();
  if (!n) {
    return "missing_name";
  }
  if (n.includes("/") || n.includes("\\")) {
    return "invalid_name";
  }
  if (!(contentType ?? "").trim() || isExplorerPageType(contentType)) {
    return "missing_type";
  }
  return null;
}
