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

/** Classic CUI page title → file name autofill rules (with .html). */
export function titleToPageFileName(title: string): string {
  const base = title
    .replace(/[ _]/g, "-")
    .replace(/[^a-zA-Z0-9\-_.]/g, "")
    .replace(/[-]+/g, "-")
    .toLowerCase();
  if (!base) {
    return base;
  }
  return base.endsWith(".html") ? base : `${base}.html`;
}

/** Classic CUI blog title → file name autofill (no dots in body; add .html). */
export function titleToBlogFileName(title: string): string {
  const base = title
    .replace(/[ _]/g, "-")
    .replace(/[^a-zA-Z0-9\-_]/g, "")
    .replace(/[-]+/g, "-")
    .toLowerCase();
  if (!base) {
    return base;
  }
  return `${base}.html`;
}

export function sanitizeFileNameInput(fileName: string): string {
  return fileName
    .replace(/[ ]/g, "-")
    .replace(/[\\/:*?"<>|#;%']/g, "");
}

/**
 * Ensure CMS path uses a single leading slash (classic CUI getNormalizedPath).
 * Used for UI navigation, path REST URLs under {@code /path/folder/...}, and
 * editor open links — not for page-create POST bodies (see
 * {@link toRepositoryCmsPath}).
 */
export function normalizeCmsPath(path: string): string {
  let p = path.trim();
  if (!p) {
    return p;
  }
  if (p.startsWith("//")) {
    p = p.substring(1);
  }
  if (!p.startsWith("/")) {
    p = `/${p}`;
  }
  return p.replace(/\/+/g, "/");
}

/**
 * Repository folder form required by content WS / page create
 * ({@code getIdByPath} rejects paths that do not start with {@code //}).
 *
 * <p>Classic CUI does the same: normalize to {@code /Sites/...} then
 * re-prefix as {@code "/" + folderPath} → {@code //Sites/...} before
 * {@code perc_page_manager.createPage}.
 */
export function toRepositoryCmsPath(path: string): string {
  const normalized = normalizeCmsPath(path);
  if (!normalized) {
    return normalized;
  }
  return normalized.startsWith("//") ? normalized : `/${normalized}`;
}

/**
 * Parent CMS path for Library navigation.
 *
 * <p>Site roots ({@code /Sites/{site}} or {@code /Assets}) have no parent folder
 * in the Home Library UI (return {@code null} → site list). Deeper folders return
 * the normalized parent path.
 */
export function parentCmsPath(path: string): string | null {
  const normalized = normalizeCmsPath(path);
  if (!normalized || normalized === "/") {
    return null;
  }
  const trimmed = normalized.replace(/\/+$/, "") || "/";
  // Site root: /Sites/{siteName} or /Assets (no trailing segments)
  const siteRoot = /^\/Sites\/[^/]+$/i;
  if (siteRoot.test(trimmed) || /^\/Assets$/i.test(trimmed)) {
    return null;
  }
  const slash = trimmed.lastIndexOf("/");
  if (slash <= 0) {
    return null;
  }
  const parent = trimmed.substring(0, slash);
  // Never leave orphan /Sites as a "folder" browse target
  if (parent === "/Sites" || parent === "/Assets" || parent === "/") {
    return null;
  }
  return parent;
}

/**
 * Breadcrumb segments for a CMS path (excluding empty leading slash).
 * Example: {@code /Sites/Demo/blog} → {@code ["Sites","Demo","blog"]}.
 */
export function cmsPathSegments(path: string): string[] {
  const normalized = normalizeCmsPath(path);
  if (!normalized || normalized === "/") {
    return [];
  }
  return normalized.split("/").filter(Boolean);
}

export function joinFolderAndName(folderPath: string, name: string): string {
  const folder = normalizeCmsPath(folderPath);
  if (folder.endsWith("/")) {
    return `${folder}${name}`;
  }
  return `${folder}/${name}`;
}

/**
 * Site-root folder for Home Create Page.
 *
 * <p>Prefers {@code SiteSummary.folderPath} (repository folder) over
 * {@code /Sites/{name}}. FastForward sample sites list as
 * {@code Corporate_Investments} but live at {@code //Sites/CorporateInvestments}
 * (#3726 / #3326). Constructing {@code /Sites/${name}} makes page create
 * {@code getIdByPath} miss, then {@code addItem} tries to create a sibling
 * folder under {@code //Sites} and Admin sees CREATE_NOT_AUTHORIZED.</p>
 */
export function siteRootFolderFromSummary(
  site:
    | {
        name?: string;
        folderPath?: string;
        folderPaths?: string[];
      }
    | string
    | null
    | undefined,
): string {
  if (site == null) {
    return "";
  }
  if (typeof site === "string") {
    const n = site.trim();
    return n ? `/Sites/${n}` : "";
  }
  const fromPaths = Array.isArray(site.folderPaths)
    ? site.folderPaths.find((p) => p != null && String(p).trim())
    : undefined;
  const fromFolder = String(site.folderPath ?? fromPaths ?? "").trim();
  if (fromFolder) {
    return normalizeCmsPath(fromFolder);
  }
  const n = String(site.name ?? "").trim();
  return n ? `/Sites/${n}` : "";
}

/**
 * Classic CUI {@code get_folder_path}: PathItem.folderPath is the repository
 * folder; PathItem.path is the finder id (often the site name).
 */
export function repositoryFolderFromPathItem(
  item:
    | { folderPath?: string; folderPaths?: string[] }
    | null
    | undefined,
  fallback: string,
): string {
  const fromPaths = Array.isArray(item?.folderPaths)
    ? item.folderPaths.find((p) => p != null && String(p).trim())
    : undefined;
  const fromFolder = String(item?.folderPath ?? fromPaths ?? "").trim();
  if (fromFolder) {
    return normalizeCmsPath(fromFolder);
  }
  return normalizeCmsPath(fallback);
}
