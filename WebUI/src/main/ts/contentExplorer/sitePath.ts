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

import type { PSPathItem } from "../api/contentExplorer/types";
import { normalizeExplorerFolderPath } from "./folderPath";

/**
 * CMS Explorer path helpers for site-scoped chrome (#2767 / parent #2400).
 *
 * <p>CMS folder paths always use {@code /} (not OS file separators). Site
 * content lives under {@code /Sites/&lt;siteName&gt;/…} (optional leading
 * double-slash legacy form {@code //Sites/…}).</p>
 */

/**
 * Extract the site name from a CMS path under {@code /Sites/…}.
 *
 * @returns site name, or {@code null} when the path is not site-scoped
 *   (e.g. {@code /Sites}, {@code /Assets/…}, empty).
 */
export function resolveSiteNameFromExplorerPath(
  path: string | null | undefined,
): string | null {
  if (path == null) return null;
  let p = path.trim().replace(/\\/g, "/");
  if (p.length === 0) return null;
  // Strip Windows drive letter (C:/Sites/Demo → /Sites/Demo after join).
  p = p.replace(/^[A-Za-z]:/, "");
  // Collapse leading double-slash (//Sites/Demo → /Sites/Demo; also UNC form).
  while (p.startsWith("//")) {
    p = p.slice(1);
  }
  if (!p.startsWith("/")) {
    p = `/${p}`;
  }
  // Prefer path-root /Sites/…; also allow /Sites/ after a prefix (UNC share).
  const match = /(?:^|\/)Sites\/([^/]+)/i.exec(p);
  if (!match) return null;
  const name = match[1]?.trim() ?? "";
  return name.length > 0 ? name : null;
}

/**
 * Prefer item path (selected row), then active folder path.
 */
export function resolveSiteNameFromSelection(
  folderPath: string | null | undefined,
  itemPath?: string | null,
): string | null {
  return (
    resolveSiteNameFromExplorerPath(itemPath) ??
    resolveSiteNameFromExplorerPath(folderPath)
  );
}

/**
 * Path used to list children of a site/folder PathItem.
 *
 * <p>Sample Rhythmyx sites use {@code SITENAME} {@code Corporate_Investments}
 * but {@code FOLDER_ROOT} {@code //Sites/CorporateInvestments} (#3326).
 * pathmanagement {@code PathItem.folderPath} is the repository folder;
 * {@code PathItem.path} is the finder id (often the site name). Prefer
 * {@code folderPath} so expand/list hits the seeded site folder.</p>
 */
export function resolveExplorerListPath(
  item: Pick<PSPathItem, "path" | "folderPath"> | null | undefined,
  fallback?: string | null,
): string | null {
  const fromFolder = normalizeExplorerFolderPath(item?.folderPath);
  if (fromFolder != null) {
    return fromFolder;
  }
  const fromPath = normalizeExplorerFolderPath(item?.path);
  if (fromPath != null) {
    return fromPath;
  }
  return normalizeExplorerFolderPath(fallback);
}

/**
 * Percussion content ids (revision-revision-content), not site names.
 * Sample site paths may still use this shape under {@code /Sites/{guid}/}
 * (#3001).
 */
const CONTENT_GUID_TOKEN = /^\d+-\d+-\d+$/;

function lastCmsPathSegment(raw: string | null | undefined): string {
  const normalized = normalizeExplorerFolderPath(raw);
  if (normalized == null) return "";
  const trimmed = normalized.replace(/\/+$/, "");
  const slash = trimmed.lastIndexOf("/");
  return (slash >= 0 ? trimmed.slice(slash + 1) : trimmed).trim();
}

function isContentGuidToken(value: string | null | undefined): boolean {
  const token = String(value ?? "").trim();
  return token.length > 0 && CONTENT_GUID_TOKEN.test(token);
}

/**
 * True when the path item is a site root (type=site or {@code /Sites/{name}}).
 */
export function isExplorerSiteRootItem(
  item: Pick<PSPathItem, "type" | "path" | "folderPath"> | null | undefined,
): boolean {
  if (!item) return false;
  const type = String(item.type ?? "")
    .trim()
    .toLowerCase();
  if (type === "site") return true;
  for (const raw of [item.path, item.folderPath]) {
    const normalized = normalizeExplorerFolderPath(raw);
    if (normalized == null) continue;
    const segs = normalized
      .replace(/^\/+/, "")
      .replace(/\/+$/, "")
      .split("/")
      .filter(Boolean);
    if (segs.length === 2 && segs[0].toLowerCase() === "sites") {
      return true;
    }
  }
  return false;
}

/**
 * Operator-visible site name for Explorer tree labels and list Name cells.
 *
 * <p>Prefers finder {@code SITENAME} ({@code Corporate_Investments}) and
 * repository folder leaf ({@code CorporateInvestments}) over a GUID
 * {@code sys_title} or {@code /Sites/{guid}/} path (#3684 / #3001). REST
 * listings and Playwright fold spaces/underscores so {@code Corporate
 * Investments} still matches.</p>
 */
export function explorerSiteDisplayName(
  item:
    | (Pick<PSPathItem, "name" | "path" | "folderPath" | "type"> & {
        title?: string;
      })
    | null
    | undefined,
): string {
  if (!item) return "";
  const candidates = [
    item.name,
    lastCmsPathSegment(item.folderPath),
    item.title,
    resolveSiteNameFromExplorerPath(item.folderPath),
    resolveSiteNameFromExplorerPath(item.path),
    lastCmsPathSegment(item.path),
  ];
  for (const candidate of candidates) {
    const token = String(candidate ?? "").trim();
    if (token.length > 0 && !isContentGuidToken(token)) {
      return token;
    }
  }
  return String(item.name || lastCmsPathSegment(item.path) || "").trim();
}
