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

/** Classic CUI page title → file name autofill rules. */
export function titleToPageFileName(title: string): string {
  return title
    .replace(/[ _]/g, "-")
    .replace(/[^a-zA-Z0-9\-_.]/g, "")
    .replace(/[-]+/g, "-")
    .toLowerCase();
}

/** Classic CUI blog title → file name autofill (no dots in body). */
export function titleToBlogFileName(title: string): string {
  return title
    .replace(/[ _]/g, "-")
    .replace(/[^a-zA-Z0-9\-_]/g, "")
    .replace(/[-]+/g, "-")
    .toLowerCase();
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
