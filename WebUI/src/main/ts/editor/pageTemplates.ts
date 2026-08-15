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

import { getContentTypeDetail } from "../api/developer/contentTypesApi";
import type { NamedObjectRef } from "../api/developer/types";
import { fetchTemplatesForSite } from "../api/home/homeApi";

export interface PageTemplateChoice {
  id: string;
  name: string;
}

export function isExplorerPageType(name: string | undefined | null): boolean {
  const n = (name ?? "").replace(/[\s_-]/g, "").toLowerCase();
  return n === "percpage" || n === "page";
}

export function siteNameFromFolderPath(folderPath: string | undefined | null): string | null {
  if (folderPath == null || !folderPath.trim()) {
    return null;
  }
  const parts = folderPath.replace(/\\/g, "/").split("/");
  for (let i = 0; i < parts.length; i++) {
    if (parts[i].toLowerCase() === "sites" && i + 1 < parts.length) {
      const site = parts[i + 1].trim();
      return site || null;
    }
  }
  return null;
}

function refId(ref: NamedObjectRef): string {
  return (
    ref.guid?.stringValue ||
    (ref.guid?.uuid != null ? String(ref.guid.uuid) : "") ||
    ref.name ||
    ""
  ).trim();
}

export function templatesFromContentType(
  allowed: NamedObjectRef[] | undefined,
): PageTemplateChoice[] {
  const out: PageTemplateChoice[] = [];
  const seen = new Set<string>();
  for (const ref of allowed ?? []) {
    const id = refId(ref);
    if (!id || seen.has(id)) {
      continue;
    }
    seen.add(id);
    out.push({ id, name: ref.label || ref.name || id });
  }
  return out;
}

export async function loadPageTemplates(
  folderPath: string,
  contentType: string,
): Promise<PageTemplateChoice[]> {
  try {
    const detail = await getContentTypeDetail(contentType);
    const fromType = templatesFromContentType(detail.allowedTemplates);
    if (fromType.length > 0) {
      return fromType;
    }
  } catch {
    /* fall through to site templates */
  }
  const site = siteNameFromFolderPath(folderPath);
  if (!site) {
    return [];
  }
  const siteTemplates = await fetchTemplatesForSite(site);
  return siteTemplates
    .filter((t) => t.id)
    .map((t) => ({ id: t.id, name: t.name || t.id }));
}
