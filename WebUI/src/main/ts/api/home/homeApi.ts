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

import { del, get, post, put } from "../client";
import type { ApiError } from "../client";
import { PATHS } from "../paths";
import { searchExtended } from "../contentExplorer/searchApi";
import type { PSSearchCriteria } from "../contentExplorer/types";
import type {
  AssetTypeSummary,
  BlogSummary,
  ContentListItem,
  CreatePageRequest,
  FolderChild,
  SiteSummary,
  TemplateSummary,
} from "./types";
import {
  joinFolderAndName,
  normalizeCmsPath,
  toRepositoryCmsPath,
} from "../../home/create/filenameUtils";

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
  return normalizeList(data).map(normalizeContentItem);
}

/**
 * Bookmarked content (classic CUI "My Bookmarks" via getMyContent).
 */
export async function fetchMyContent(): Promise<ContentListItem[]> {
  const data = await get<unknown>(PATHS.MY_CONTENT);
  return normalizeList(data).map(normalizeContentItem);
}

/**
 * Add a page to the current user's favorites (classic addToMyPages / My Bookmarks).
 *
 * @param pageId - string page guid (same id shape as editor / mycontent rows)
 */
export async function addToMyPages(pageId: string): Promise<void> {
  const id = String(pageId ?? "").trim();
  if (!id) {
    throw new Error("pageId is required");
  }
  await put(`${PATHS.ADD_TO_MYPAGES}/${encodeURIComponent(id)}`);
}

/**
 * Remove a page from the current user's favorites (classic removeFromMyPages).
 *
 * @param pageId - string page guid
 */
export async function removeFromMyPages(pageId: string): Promise<void> {
  const id = String(pageId ?? "").trim();
  if (!id) {
    throw new Error("pageId is required");
  }
  await del(`${PATHS.REMOVE_FROM_MYPAGES}/${encodeURIComponent(id)}`);
}

/**
 * Whether the page is in the current user's favorites (internal isMyPage).
 * Expects a JSON boolean (or stringified true/false from the wire parser).
 */
export async function isMyPage(pageId: string): Promise<boolean> {
  const id = String(pageId ?? "").trim();
  if (!id) {
    return false;
  }
  const data = await get<unknown>(
    `${PATHS.IS_MY_PAGE}/${encodeURIComponent(id)}`,
  );
  if (typeof data === "boolean") {
    return data;
  }
  if (typeof data === "string") {
    return data.trim().toLowerCase() === "true";
  }
  return Boolean(data);
}

/**
 * Stable id for bookmark operations from a list row.
 * Prefers {@code id}, then {@code contentId}.
 */
export function contentItemId(item: ContentListItem): string | null {
  if (item.id != null && String(item.id).trim()) {
    return String(item.id).trim();
  }
  const raw = item as ContentListItem & { contentId?: unknown };
  if (raw.contentId != null && String(raw.contentId).trim()) {
    return String(raw.contentId).trim();
  }
  return null;
}

/**
 * True when the row looks like a bookmarkable page (has id, not a folder).
 * Classic My Pages only supports pages.
 */
export function isBookmarkableItem(item: ContentListItem): boolean {
  if (!contentItemId(item)) {
    return false;
  }
  if (item.folder === true) {
    return false;
  }
  const type = item.type != null ? String(item.type).toLowerCase() : "";
  if (type === "folder" || type === "site") {
    return false;
  }
  return true;
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
  return normalizeList(data).map(normalizeContentItem) as FolderChild[];
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

/**
 * Widget definition ids for blog templates.
 * Blog List (index) → percBlogIndexPage; Blog Post → percBlogPost.
 */
export const BLOG_LIST_WIDGET_ID = "percBlogIndexPage";
export const BLOG_POST_WIDGET_ID = "percBlogPost";

/** Load full template (includes region → widget associations). */
export async function fetchTemplate(templateId: string): Promise<unknown> {
  return get<unknown>(
    `${PATHS.TEMPLATE_LOAD}/${encodeURIComponent(templateId)}`,
  );
}

/** Max walk depth for template JSON (guards pathological nesting / cycles). */
const TEMPLATE_WIDGET_WALK_MAX_DEPTH = 32;

/**
 * Collect widget definitionIds from a loaded template JSON (Template.regionTree).
 * Uses iterative DFS with a depth limit to avoid stack overflow on deep trees.
 */
export function extractTemplateWidgetDefinitionIds(raw: unknown): string[] {
  const ids = new Set<string>();
  const root =
    raw && typeof raw === "object" && "Template" in (raw as object)
      ? (raw as { Template: unknown }).Template
      : raw;

  type Frame = { node: unknown; depth: number };
  const stack: Frame[] = [];
  if (root && typeof root === "object") {
    const t = root as Record<string, unknown>;
    stack.push({ node: t.regionTree, depth: 0 });
    stack.push({ node: t.regionWidgetAssociations, depth: 0 });
    stack.push({ node: t.widgets, depth: 0 });
  } else {
    stack.push({ node: root, depth: 0 });
  }

  while (stack.length > 0) {
    const frame = stack.pop();
    if (!frame) break;
    const { node, depth } = frame;
    if (node == null || depth > TEMPLATE_WIDGET_WALK_MAX_DEPTH) continue;
    if (Array.isArray(node)) {
      for (const n of node) {
        stack.push({ node: n, depth: depth + 1 });
      }
      continue;
    }
    if (typeof node !== "object") continue;
    const o = node as Record<string, unknown>;
    if (o.definitionId != null && String(o.definitionId).trim()) {
      ids.add(String(o.definitionId).trim());
    }
    // Also check common alternate keys
    if (o.widgetDefinitionId != null && String(o.widgetDefinitionId).trim()) {
      ids.add(String(o.widgetDefinitionId).trim());
    }
    for (const v of Object.values(o)) {
      stack.push({ node: v, depth: depth + 1 });
    }
  }
  return Array.from(ids);
}

export function templateHasWidget(
  rawTemplate: unknown,
  widgetDefinitionId: string,
): boolean {
  const target = widgetDefinitionId.toLowerCase();
  return extractTemplateWidgetDefinitionIds(rawTemplate).some(
    (id) => id.toLowerCase() === target,
  );
}

/**
 * Site templates that contain the given widget definition (e.g. Blog List or Blog Post).
 * Loads each template fully — suitable for small site template catalogs.
 */
export async function fetchTemplatesWithWidget(
  siteName: string,
  widgetDefinitionId: string,
): Promise<TemplateSummary[]> {
  const summaries = await fetchTemplatesForSite(siteName);
  const out: TemplateSummary[] = [];
  for (const s of summaries) {
    if (!s.id) continue;
    try {
      const full = await fetchTemplate(s.id);
      if (templateHasWidget(full, widgetDefinitionId)) {
        out.push(s);
      }
    } catch (err: unknown) {
      // Skip templates that fail to load; log so network/500 failures are visible.
      console.warn(
        `fetchTemplatesWithWidget: failed to load template ${s.id}`,
        err,
      );
    }
  }
  return out;
}

/** Blog list (index) templates: must include Blog List widget. */
export async function fetchBlogListTemplates(
  siteName: string,
): Promise<TemplateSummary[]> {
  return fetchTemplatesWithWidget(siteName, BLOG_LIST_WIDGET_ID);
}

/** Blog post templates: must include Blog Post widget. */
export async function fetchBlogPostTemplates(
  siteName: string,
): Promise<TemplateSummary[]> {
  return fetchTemplatesWithWidget(siteName, BLOG_POST_WIDGET_ID);
}

/**
 * Map a WidgetContentType wire row to {@link AssetTypeSummary}.
 * Classic createAsset / editAsset require the string {@code widgetId}
 * (e.g. percImage), not the numeric contentTypeId.
 */
export function mapAssetType(raw: unknown): AssetTypeSummary {
  const o = (raw ?? {}) as Record<string, unknown>;
  const widgetId =
    o.widgetId != null && String(o.widgetId).trim()
      ? String(o.widgetId).trim()
      : o.id != null && String(o.id).trim()
        ? String(o.id).trim()
        : "";
  const label =
    o.widgetLabel != null
      ? String(o.widgetLabel)
      : o.label != null
        ? String(o.label)
        : undefined;
  const name =
    label ||
    (o.contentTypeName != null ? String(o.contentTypeName) : undefined) ||
    (o.name != null ? String(o.name) : undefined) ||
    widgetId;
  return {
    id: widgetId,
    name,
    label,
    contentTypeId:
      o.contentTypeId != null ? String(o.contentTypeId) : undefined,
    contentTypeName:
      o.contentTypeName != null ? String(o.contentTypeName) : undefined,
  };
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
  return list.map(mapAssetType).filter((t) => Boolean(t.id));
}

function mapBlogSummary(
  raw: unknown,
  siteFallback?: string,
): BlogSummary {
  const o = (raw ?? {}) as Record<string, unknown>;
  const path = String(o.path ?? "");
  // Wire path is typically the blog section/page path; posts go in that folder.
  let folderPath =
    o.folderPath != null && String(o.folderPath).trim()
      ? String(o.folderPath).trim()
      : path && path.includes("/")
        ? path.substring(0, path.lastIndexOf("/"))
        : path;
  folderPath = normalizeCmsPath(folderPath);
  // Site from path /Sites/{site}/... when not provided
  let site = siteFallback;
  if (!site && folderPath.toLowerCase().startsWith("/sites/")) {
    const parts = folderPath.split("/").filter(Boolean);
    if (parts.length >= 2) {
      site = parts[1];
    }
  }
  return {
    title: String(o.title ?? o.name ?? "Blog"),
    folderPath,
    templateId: String(o.blogPostTemplateId ?? o.templateId ?? ""),
    site,
    path: path || undefined,
  };
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
  return list
    .map((raw) => mapBlogSummary(raw, siteName))
    .filter((b) => Boolean(b.templateId && b.folderPath));
}

/**
 * All blogs across sites (single call — preferred for Home Create chooser).
 */
export async function fetchAllBlogs(): Promise<BlogSummary[]> {
  const data = await get<unknown>(PATHS.ALL_BLOGS);
  let list: unknown[] = [];
  if (data && typeof data === "object" && "SiteBlogProperties" in data) {
    const b = (data as { SiteBlogProperties: unknown }).SiteBlogProperties;
    list = Array.isArray(b) ? b : b ? [b] : [];
  } else if (Array.isArray(data)) {
    list = data;
  }
  return list
    .map((raw) => mapBlogSummary(raw))
    .filter((b) => Boolean(b.templateId && b.folderPath));
}

/**
 * Ensure page/blog file names end with {@code .html} when the user did not
 * supply an extension (classic CUI page names usually include it).
 */
export function ensurePageFileName(name: string): string {
  const n = String(name ?? "").trim();
  if (!n) {
    return n;
  }
  if (/\.[a-zA-Z0-9]{1,8}$/.test(n)) {
    return n;
  }
  return `${n}.html`;
}

/**
 * Create a page via page management REST (same JSON shape as perc_page_manager).
 * folderPath should be the parent folder (classic passes path with leading slash).
 */
export async function createPage(req: CreatePageRequest): Promise<unknown> {
  // Page REST validates via contentWs.getIdByPath, which requires //Sites/...
  const folderPath = toRepositoryCmsPath(req.folderPath);
  const name = ensurePageFileName(req.name);
  const body = {
    Page: {
      name,
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
  const name = ensurePageFileName(req.name);
  await createPage({ ...req, name });
  return joinFolderAndName(req.folderPath, name);
}

/**
 * Run extended finder search (same REST as Content Explorer US5).
 *
 * <p>Must wrap body under {@code SearchCriteria} and unwrap
 * {@code PagedItemPropertiesList.childrenInPage}. A bare criteria object
 * or flat array unwrap silently yields empty Home Search results.</p>
 */
export async function searchContent(
  criteria: PSSearchCriteria | Record<string, unknown>,
): Promise<ContentListItem[]> {
  const query =
    typeof criteria.query === "string"
      ? criteria.query
      : typeof (criteria as { searchText?: unknown }).searchText === "string"
        ? String((criteria as { searchText: string }).searchText)
        : "";
  const maxResults =
    typeof criteria.maxResults === "number" ? criteria.maxResults : 50;
  // Server rejects startIndex < 1 (IllegalArgumentException).
  const startIndex =
    typeof criteria.startIndex === "number" && criteria.startIndex >= 1
      ? criteria.startIndex
      : 1;
  const folderPath =
    typeof criteria.folderPath === "string" ? criteria.folderPath : undefined;

  // formatId is required by PSSearchService.searchForIds (classic finder uses
  // the active display format; Home defaults to system list format id 9).
  const formatId =
    typeof criteria.formatId === "number" ? criteria.formatId : 9;

  const searchCriteria: PSSearchCriteria = {
    query: query.trim(),
    maxResults,
    startIndex,
    folderPath,
    formatId,
    searchType:
      typeof criteria.searchType === "string" ? criteria.searchType : undefined,
    sortColumn:
      typeof criteria.sortColumn === "string" ? criteria.sortColumn : undefined,
    sortOrder:
      typeof criteria.sortOrder === "string" ? criteria.sortOrder : undefined,
  };

  const results = await searchExtended(searchCriteria);
  return results.children.map((row) =>
    normalizeContentItem({
      id: row.id,
      name: row.name ?? row.title,
      title: row.title ?? row.name,
      path: row.folderPath ?? (row as { path?: string }).path,
      type: row.type,
      ...row,
    }),
  );
}

/**
 * Normalize PSItemProperties / PathItem-style rows so Home sections always
 * have display {@code name} and openable {@code path}/{@code id}.
 */
export function normalizeContentItem(
  raw: ContentListItem | Record<string, unknown>,
): ContentListItem {
  const o = raw as ContentListItem & {
    contentId?: string;
    folderPath?: string;
    title?: string;
    name?: string;
    path?: string;
    id?: string;
  };
  const path =
    (o.path != null && String(o.path).trim()) ||
    (o.folderPath != null && String(o.folderPath).trim()) ||
    undefined;
  const id =
    (o.id != null && String(o.id)) ||
    (o.contentId != null && String(o.contentId)) ||
    undefined;
  const name =
    (o.name != null && String(o.name).trim()) ||
    (o.title != null && String(o.title).trim()) ||
    undefined;
  return {
    ...o,
    id,
    name,
    title: o.title != null ? String(o.title) : name,
    path,
  };
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
    // Search wire: { PagedItemPropertiesList: { childrenInPage: [...] } }
    const paged = obj.PagedItemPropertiesList;
    if (paged && typeof paged === "object") {
      const children = (paged as { childrenInPage?: unknown }).childrenInPage;
      if (Array.isArray(children)) {
        return children as ContentListItem[];
      }
    }
    for (const key of [
      "RecentItemList",
      "ItemProperties",
      "items",
      "ItemList",
      "results",
      "PathItem",
      "children",
      "childrenInPage",
      "resultPage",
      "FolderItem",
      "childFolders",
      "PagedItemList",
    ]) {
      const v = obj[key];
      if (Array.isArray(v)) {
        return v as ContentListItem[];
      }
      // Nested list wrappers (e.g. PagedItemList.childrenInPage)
      if (v && typeof v === "object") {
        const nested = v as Record<string, unknown>;
        for (const nk of ["childrenInPage", "children", "ItemProperties", "PathItem"]) {
          if (Array.isArray(nested[nk])) {
            return nested[nk] as ContentListItem[];
          }
        }
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
