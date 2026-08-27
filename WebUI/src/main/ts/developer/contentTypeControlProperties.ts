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

import { normalizeContentTypeControlProperties } from "../api/developer/contentTypeLists";
import type { ContentTypeControlProperty } from "../api/developer/types";

export function cloneControlProperties(list: unknown): ContentTypeControlProperty[] {
  return normalizeContentTypeControlProperties(list).map((p) => ({
    name: p.name || "",
    value: p.value || "",
  }));
}

export function controlPropertiesEqual(
  a: ContentTypeControlProperty[],
  b: ContentTypeControlProperty[],
): boolean {
  if (a.length !== b.length) {
    return false;
  }
  for (let i = 0; i < a.length; i++) {
    if ((a[i].name || "") !== (b[i].name || "")) {
      return false;
    }
    if ((a[i].value || "") !== (b[i].value || "")) {
      return false;
    }
  }
  return true;
}

/** Strip blank names for CD-07 PUT .../controlProperties. */
export function toControlPropertyPayload(
  list: ContentTypeControlProperty[],
): ContentTypeControlProperty[] {
  return list
    .map((p) => ({
      name: (p.name || "").trim(),
      value: p.value == null ? "" : String(p.value),
    }))
    .filter((p) => p.name.length > 0);
}
