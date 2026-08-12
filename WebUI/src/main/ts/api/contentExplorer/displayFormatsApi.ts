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
 * Typed client for the public REST display-format catalog used by the
 * modern Content Explorer list (#2400 / FR-027).
 *
 * <p>Provider: {@code rest} {@code DisplayFormatResource} at
 * {@code GET /Rhythmyx/rest/displayformats}. Optional query filters
 * {@code validForFolder} and {@code validForViewsAndSearches} are applied
 * server-side.</p>
 *
 * <p>GUID unwrap / synthesis is shared with the Developer API via
 * {@link ../displayFormatGuid} (#2689 / #2951).</p>
 */

import { get } from "../client";
import {
  normalizeDisplayFormatGuid,
  objectGuidString,
  resolveDisplayFormatObjectGuid,
  unwrapDisplayFormat,
  unwrapDisplayFormatList,
} from "../displayFormatGuid";
import { PATHS } from "../paths";
import type { DisplayFormat, DisplayFormatColumn } from "../developer/types";

export type { DisplayFormat, DisplayFormatColumn };

// Re-export shared GUID helpers so Content Explorer callers (and tests) share
// one implementation with the Developer API.
export {
  normalizeDisplayFormatGuid,
  objectGuidString,
  resolveDisplayFormatObjectGuid,
  unwrapDisplayFormat,
  unwrapDisplayFormatList,
};

function asArray(payload: unknown): DisplayFormat[] {
  return unwrapDisplayFormatList(payload);
}

export function normalizeDisplayFormatColumns(
  columns: DisplayFormat["columns"],
): DisplayFormatColumn[] {
  if (columns == null) return [];
  if (Array.isArray(columns)) return columns;
  const wrapped = columns.DisplayFormatColumn;
  if (wrapped == null) return [];
  return Array.isArray(wrapped) ? wrapped : [wrapped];
}

export interface ListDisplayFormatsParams {
  validForFolder?: boolean;
  validForViewsAndSearches?: boolean;
}

/** GET /services/displayformats with optional Explorer filters. */
export async function listDisplayFormats(
  params: ListDisplayFormatsParams = {},
): Promise<DisplayFormat[]> {
  const q = new URLSearchParams();
  if (params.validForFolder !== undefined) {
    q.set("validForFolder", String(params.validForFolder));
  }
  if (params.validForViewsAndSearches !== undefined) {
    q.set("validForViewsAndSearches", String(params.validForViewsAndSearches));
  }
  const qs = q.toString();
  const payload = await get<unknown>(
    `${PATHS.DISPLAY_FORMATS}${qs ? `?${qs}` : ""}`,
  );
  return asArray(payload);
}

/** GET /services/displayformats/{idOrName} */
export async function getDisplayFormatDetail(
  idOrName: string,
): Promise<DisplayFormat> {
  const key = encodeURIComponent(idOrName);
  const payload = await get<unknown>(`${PATHS.DISPLAY_FORMATS}/${key}`);
  return unwrapDisplayFormat(payload);
}
