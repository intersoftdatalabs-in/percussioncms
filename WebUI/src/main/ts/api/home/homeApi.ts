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
import type { ApiError } from "../client";
import { PATHS } from "../paths";
import type {
  AssetTypeSummary,
  BlogSummary,
  ContentListItem,
  CreatePageRequest,
  FolderChild,
  SiteSummary,
  TemplateSummary,
} from "./types";
import { joinFolderAndName, normalizeCmsPath } from "../../home/create/filenameUtils";

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
  const normalized = normalizeCmsPath(path);
  const data = await get<unknown>(
    `${PATHS.PATH_FOLDER}${encodeURI(normalized)}`,
  );
  return normalizeList(data) as FolderChild[];
}

/** Templates for a site (classic getTemplates). */
export async function fetchTemplatesForSite(
  siteName: string,
): Promise<TemplateSummary[]> {
  const data = await get<unknown>(
    `${PATHS.TEMPLATES_BY_SITE}/${encodeURIComponent(siteName)}`,
  );
  if (data && typeof data === "object" && "TemplateSummary" in data) {
    const list = (data as { TemplateSummary: unknown }).TemplateSummary;
    const arr = Array.isArray(list) ? list : list ? [list] : [];
    return arr.map((t) => {
      const o = t as Record<string, unknown>;
      return {
        id: String(o.id ?? ""),
        name: String(o.name ?? o.label ?? o.id ?? ""),
        thumbPath: o.imageThumbPath
          ? String(o.imageThumbPath)
          : o.thumbPath
            ? String(o.thumbPath)
            : undefined,
      };
    });
  }
  return [];
}

/** Asset widget types (classic getAssetTypes). */
export async function fetchAssetTypes(
  filterDisabled = true,
): Promise<AssetTypeSummary[]> {
  let url = PATHS.ASSET_TYPES;
  if (filterDisabled) {
    url += "?filterDisabledWidgets=yes";
  }
  const data = await get<unknown>(url);
  let list: unknown[] = [];
  if (data && typeof data === "object" && "WidgetContentType" in data) {
    const w = (data as { WidgetContentType: unknown }).WidgetContentType;
    list = Array.isArray(w) ? w : w ? [w] : [];
  } else if (Array.isArray(data)) {
    list = data;
  }
  return list.map((t) => {
    const o = t as Record<string, unknown>;
    return {
      id: String(o.contentTypeId ?? o.widgetId ?? o.id ?? ""),
      name: String(o.name ?? o.label ?? o.id ?? ""),
      label: o.label ? String(o.label) : undefined,
    };
  });
}

/** Blogs for a site (classic getBlogsForSite). */
export async function fetchBlogsForSite(
  siteName: string,
): Promise<BlogSummary[]> {
  const data = await get<unknown>(
    `${PATHS.BLOGS_FOR_SITE}/${encodeURIComponent(siteName)}`,
  );
  let list: unknown[] = [];
  if (data && typeof data === "object" && "SiteBlogProperties" in data) {
    const b = (data as { SiteBlogProperties: unknown }).SiteBlogProperties;
    list = Array.isArray(b) ? b : b ? [b] : [];
  } else if (Array.isArray(data)) {
    list = data;
  }
  return list.map((raw) => {
    const o = raw as Record<string, unknown>;
    const path = String(o.path ?? "");
    const folderPath =
      path && path.includes("/")
        ? path.substring(0, path.lastIndexOf("/"))
        : String(o.folderPath ?? "");
    return {
      title: String(o.title ?? o.name ?? ""),
      folderPath,
      templateId: String(o.blogPostTemplateId ?? o.templateId ?? ""),
      site: siteName,
      path,
    };
  });
}

/**
 * Create a page via page management REST (same JSON shape as perc_page_manager).
 * folderPath should be the parent folder (classic passes path with leading slash).
 */
export async function createPage(req: CreatePageRequest): Promise<unknown> {
  const folderPath = normalizeCmsPath(req.folderPath);
  const body = {
    Page: {
      name: req.name,
      title: req.title,
      templateId: req.templateId,
      linkTitle: req.linkTitle,
      folderPath,
      addToRecent: true,
    },
  };
  return post(PATHS.PAGE_CREATE, body);
}

/**
 * Create page then return full item path for open (classic openPage after create).
 */
export async function createPageAndPath(
  req: CreatePageRequest,
): Promise<string> {
  await createPage(req);
  return joinFolderAndName(req.folderPath, req.name);
}

/** Run extended finder search. */
export async function searchContent(
  criteria: Record<string, unknown>,
): Promise<ContentListItem[]> {
  const data = await post<unknown>(PATHS.FINDER_SEARCH_EXTENDED, criteria);
  return normalizeList(data);
}

/**
 * Map a thrown API / network error to a user-facing string.
 *
 * <p>{@link ApiError} from {@code client.ts} is the common case (status + body).
 * HTTP 401/403 and bodies mentioning {@code NotAuthorized} use
 * {@code notAuthorizedMsg}; other body text is preferred when present.</p>
 */
export function formatApiError(err: unknown, notAuthorizedMsg: string): string {
  if (err && typeof err === "object" && ("status" in err || "body" in err)) {
    const apiErr = err as ApiError;
    if (apiErr.status === 401 || apiErr.status === 403) {
      return notAuthorizedMsg;
    }
    const body = apiErr.body;
    if (typeof body === "string" && body.trim()) {
      return body.includes("NotAuthorized") ? notAuthorizedMsg : body;
    }
    if (body && typeof body === "object") {
      const o = body as Record<string, unknown>;
      if (typeof o.message === "string" && o.message.trim()) {
        return o.message.includes("NotAuthorized")
          ? notAuthorizedMsg
          : o.message;
      }
    }
  }
  if (typeof err === "string" && err.includes("NotAuthorized")) {
    return notAuthorizedMsg;
  }
  if (err instanceof Error && err.message) {
    return err.message.includes("NotAuthorized")
      ? notAuthorizedMsg
      : err.message;
  }
  return notAuthorizedMsg;
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
      "FolderItem",
      "childFolders",
    ]) {
      const v = obj[key];
      if (Array.isArray(v)) {
        return v as ContentListItem[];
      }
    }
    for (const v of Object.values(obj)) {
      if (
        Array.isArray(v) &&
        (v.length === 0 || typeof v[0] === "object")
      ) {
        return v as ContentListItem[];
      }
    }
  }
  return [];
}
