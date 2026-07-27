/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

/** Normalize list type discriminator from API. */
export function normalizeListType(
  listType: string | null | undefined,
): "legacy" | "modern" | "unknown" {
  if (listType == null || listType === "") {
    return "unknown";
  }
  const n = listType.trim().toLowerCase();
  if (n === "legacy" || n === "old") {
    return "legacy";
  }
  if (n === "modern" || n === "new" || n === "generator") {
    return "modern";
  }
  return "unknown";
}

export function normalizeSchemeType(
  schemeType: string | null | undefined,
  generator?: string | null,
): "legacy" | "modern" | "unknown" {
  if (schemeType != null && schemeType !== "") {
    return normalizeListType(schemeType);
  }
  if (generator != null && generator.trim() !== "") {
    return "modern";
  }
  return "legacy";
}

/** Whether the editor should show legacy URL field vs generator. */
export function isLegacyContentList(listType?: string | null): boolean {
  return normalizeListType(listType) === "legacy";
}
