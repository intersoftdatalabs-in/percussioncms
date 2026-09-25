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

import type { ContentTypeSummary } from "../api/developer/types";
import { isExplorerPageType } from "../editor/pageTemplates";

export type CreatePageFieldError = "missing_name" | "invalid_name" | "missing_type";

export interface PageContentTypeChoice {
  name: string;
  label: string;
}

/** Page content types only (percPage / Page). Order follows the catalog. */
export function pageContentTypeChoices(
  types: readonly ContentTypeSummary[] | null | undefined,
): PageContentTypeChoice[] {
  const out: PageContentTypeChoice[] = [];
  const seen = new Set<string>();
  for (const type of types ?? []) {
    const name = (type.name ?? "").trim();
    if (!name || seen.has(name) || !isExplorerPageType(name)) {
      continue;
    }
    seen.add(name);
    out.push({ name, label: (type.label ?? "").trim() || name });
  }
  return out;
}

/**
 * Client gate before POST. Blank name, path separators, or a blank content
 * type are not a create.
 */
export function validateCreatePageFields(
  name: string | null | undefined,
  contentType: string | null | undefined,
): CreatePageFieldError | null {
  const n = (name ?? "").trim();
  if (!n) {
    return "missing_name";
  }
  if (n.includes("/") || n.includes("\\")) {
    return "invalid_name";
  }
  if (!(contentType ?? "").trim()) {
    return "missing_type";
  }
  return null;
}
