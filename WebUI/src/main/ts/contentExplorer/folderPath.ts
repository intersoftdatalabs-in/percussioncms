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
 * CMS Explorer path helpers for folder-scoped chrome (#2792 / parent #2400).
 *
 * <p>CMS folder paths always use {@code /} (not OS file separators). Used by
 * Content → Subfolder Copy to seed {@link SubfolderCopyWizard} source.</p>
 */

/**
 * Normalize a CMS path for use as a copy source: trim, convert {@code \} to
 * {@code /}, strip a Windows drive letter, collapse leading double-slashes.
 *
 * @returns normalized path starting with {@code /}, or {@code null} when empty
 */
export function normalizeExplorerFolderPath(
  path: string | null | undefined,
): string | null {
  if (path == null) return null;
  let p = path.trim().replace(/\\/g, "/");
  if (p.length === 0) return null;
  p = p.replace(/^[A-Za-z]:/, "");
  while (p.startsWith("//")) {
    p = p.slice(1);
  }
  if (!p.startsWith("/")) {
    p = `/${p}`;
  }
  // Collapse accidental internal double-slashes (not repository // prefix).
  p = p.replace(/\/{2,}/g, "/");
  // Root alone is not a useful subfolder copy source.
  if (p === "/") return null;
  return p;
}

/**
 * Whether {@code childPath} is a strict descendant of {@code parentPath} in
 * the CMS path tree (URL-style {@code /} segments).
 *
 * <p>Used by ExplorerTree to drop self/ancestor cycles from a bad API payload.
 * Normalizes leading double-slash repository form ({@code //Sites/…}) and
 * trailing slashes so {@code /Sites} matches children {@code /Sites/id/} or
 * legacy {@code //Sites/Name} (#3001).</p>
 */
export function isStrictCmsPathDescendant(
  parentPath: string | null | undefined,
  childPath: string | null | undefined,
): boolean {
  if (childPath == null || String(childPath).trim().length === 0) {
    return false;
  }
  // Allow root "/" as parent even though normalizeExplorerFolderPath returns null.
  let parent = parentPath == null ? "" : String(parentPath).trim().replace(/\\/g, "/");
  parent = parent.replace(/^[A-Za-z]:/, "");
  while (parent.startsWith("//")) {
    parent = parent.slice(1);
  }
  parent = parent.replace(/\/{2,}/g, "/");
  if (parent.length === 0) {
    parent = "/";
  } else if (!parent.startsWith("/")) {
    parent = `/${parent}`;
  }

  let child = String(childPath).trim().replace(/\\/g, "/");
  child = child.replace(/^[A-Za-z]:/, "");
  while (child.startsWith("//")) {
    child = child.slice(1);
  }
  child = child.replace(/\/{2,}/g, "/");
  if (!child.startsWith("/")) {
    child = `/${child}`;
  }

  // Strip trailing slashes except for pure root.
  const stripTrail = (p: string): string =>
    p === "/" ? "/" : p.replace(/\/+$/, "");
  parent = stripTrail(parent);
  child = stripTrail(child);

  if (child === parent) {
    return false;
  }
  if (parent === "/") {
    // Any non-root path is a child of root.
    return child.startsWith("/");
  }
  return child.startsWith(`${parent}/`);
}

/**
 * Whether {@code childPath} is safe to render under a tree node.
 *
 * <p>Drops self/ancestor cycles. Accepts children whose finder path uses
 * the site <em>name</em> while the node stores {@code folderPath} (or the
 * reverse) so sample sites {@code Corporate_Investments} vs folder
 * {@code CorporateInvestments} are not filtered to empty (#3326).</p>
 */
export function isSafeExplorerTreeChild(
  parentPath: string | null | undefined,
  parentFolderPath: string | null | undefined,
  childPath: string | null | undefined,
): boolean {
  if (childPath == null || String(childPath).trim().length === 0) {
    return false;
  }
  const childNorm = normalizeExplorerFolderPath(childPath);
  const parentNorm = normalizeExplorerFolderPath(parentPath);
  const folderNorm = normalizeExplorerFolderPath(parentFolderPath);
  if (childNorm != null && parentNorm != null && childNorm === parentNorm) {
    return false;
  }
  if (childNorm != null && folderNorm != null && childNorm === folderNorm) {
    return false;
  }
  // Child that is an ancestor of the parent would recurse forever.
  if (isStrictCmsPathDescendant(childPath, parentPath)) {
    return false;
  }
  if (
    parentFolderPath != null &&
    isStrictCmsPathDescendant(childPath, parentFolderPath)
  ) {
    return false;
  }
  if (isStrictCmsPathDescendant(parentPath, childPath)) {
    return true;
  }
  if (
    parentFolderPath != null &&
    isStrictCmsPathDescendant(parentFolderPath, childPath)
  ) {
    return true;
  }
  // Name vs folder-root prefix mismatch — still render (not a cycle).
  return true;
}

/**
 * Resolve the source folder path for Subfolder Copy.
 *
 * <p>Prefer a selected folder row path; otherwise the active tree folder
 * path. Content items (pages/assets) do not become the source — only their
 * parent folder path (via {@code folderPath}) is used.</p>
 *
 * @param folderPath active Explorer folder path
 * @param itemPath selected row path (optional)
 * @param itemType selected row type (e.g. {@code "folder"})
 */
export function resolveFolderPathFromSelection(
  folderPath: string | null | undefined,
  itemPath?: string | null,
  itemType?: string | null,
): string | null {
  if (itemType === "folder") {
    const fromItem = normalizeExplorerFolderPath(itemPath);
    if (fromItem != null) return fromItem;
  }
  return normalizeExplorerFolderPath(folderPath);
}

/**
 * Repository form required by content WS / search execute
 * ({@code getIdByPath} rejects paths that do not start with {@code //}).
 *
 * <p>Explorer tree/list chrome uses a single leading slash ({@code /Sites}).
 * Free-text search and saved-search execute must send {@code //Sites} (or
 * {@code //Folders}, {@code //Assets}) or the server returns
 * {@code Path: /Sites - must start with '//'} (#3438 / #2799).</p>
 *
 * <p>CMS paths always use {@code /} — this is not OS file I/O. Drive
 * letters and backslashes are stripped only so a pasted Windows-style
 * path still becomes a valid repository path.</p>
 *
 * @returns {@code //Sites/...} form, or {@code undefined} when empty
 */
export function toRepositorySearchFolderPath(
  path: string | null | undefined,
): string | undefined {
  if (path == null) return undefined;
  let p = path.trim().replace(/\\/g, "/");
  if (p.length === 0) return undefined;
  p = p.replace(/^[A-Za-z]:/, "");
  if (p.length === 0) return undefined;
  // Collapse all slashes first so //Sites and /Sites share one form.
  p = p.replace(/\/{2,}/g, "/");
  if (!p.startsWith("/")) {
    p = `/${p}`;
  }
  return `/${p}`;
}
