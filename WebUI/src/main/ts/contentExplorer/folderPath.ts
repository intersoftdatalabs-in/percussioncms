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
  // Root alone is not a useful subfolder copy source.
  if (p === "/") return null;
  return p;
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
