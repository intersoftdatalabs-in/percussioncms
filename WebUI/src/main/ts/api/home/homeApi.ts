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

import { get, post } from "../client";
import { PATHS } from "../paths";
import type {
  ContentListItem,
  CreatePageRequest,
  FolderChild,
  SiteSummary,
} from "./types";

/** Recent content items (type typically {@code item}). */
export async function fetchRecentItems(
  type = "item",
  site?: string,
): Promise<ContentListItem[]> {
  let url = `${PATHS.RECENT_ROOT}${encodeURIComponent(type)}`;
  if (site) {
    url += `/${encodeURIComponent(site)}`;
  }
  const data = await get<unknown>(url);
  return normalizeList(data);
}

/**
 * Bookmarked content (classic CUI "My Bookmarks" via getMyContent).
 * GET {@code /itemmanagement/item/mycontent}
 */
export async function fetchMyContent(): Promise<ContentListItem[]> {
  const data = await get<unknown>(PATHS.MY_CONTENT);
  return normalizeList(data);
}

/** All sites for Library root. */
export async function fetchSites(): Promise<SiteSummary[]> {
  const data = await get<unknown>(`${PATHS.SITES_ALL}/`);
  if (data && typeof data === "object" && "SiteSummary" in data) {
    const list = (data as { SiteSummary: unknown }).SiteSummary;
    if (Array.isArray(list)) {
      return list as SiteSummary[];
    }
    if (list && typeof list === "object") {
      return [list as SiteSummary];
    }
  }
  if (Array.isArray(data)) {
    return data as SiteSummary[];
  }
  return [];
}

/** Folder children under a CMS path. */
export async function fetchFolderChildren(
  path: string,
): Promise<FolderChild[]> {
  const normalized = path.startsWith("/") ? path : `/${path}`;
  const data = await get<unknown>(
    `${PATHS.PATH_FOLDER}${encodeURI(normalized)}`,
  );
  return normalizeList(data) as FolderChild[];
}

/** Run extended finder search. */
export async function searchContent(
  criteria: Record<string, unknown>,
): Promise<ContentListItem[]> {
  const data = await post<unknown>(PATHS.FINDER_SEARCH_EXTENDED, criteria);
  return normalizeList(data);
}

/**
 * Create a page via page management REST.
 * Payload shape may be refined as server contract is verified in UAT.
 */
export async function createPage(req: CreatePageRequest): Promise<unknown> {
  const body = {
    name: req.name,
    title: req.title,
    linkTitle: req.linkTitle,
    templateId: req.templateId,
    folderPath: req.folderPath,
  };
  return post(PATHS.PAGE_CREATE, body);
}

function normalizeList(data: unknown): ContentListItem[] {
  if (Array.isArray(data)) {
    return data as ContentListItem[];
  }
  if (data && typeof data === "object") {
    const obj = data as Record<string, unknown>;
    for (const key of [
      "RecentItemList",
      "ItemProperties",
      "items",
      "ItemList",
      "results",
      "PathItem",
      "children",
      "resultPage",
    ]) {
      const v = obj[key];
      if (Array.isArray(v)) {
        return v as ContentListItem[];
      }
    }
    // Single wrapper objects with nested arrays
    for (const v of Object.values(obj)) {
      if (Array.isArray(v) && v.length >= 0 && (v.length === 0 || typeof v[0] === "object")) {
        return v as ContentListItem[];
      }
    }
  }
  return [];
}
