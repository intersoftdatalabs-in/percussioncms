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

/**
 * PUT /services/sites stores a non-blank CMS folder path. A blank or unsafe
 * path must not be written. The value is the site record's folder root; it
 * does not move folder items.
 */
const MAX_LEN = 1024;

/** Trim and turn backslashes into slashes. Does not accept the value. */
export function normalizeFolderRoot(raw: string): string {
  return raw.trim().replace(/\\/g, "/");
}

export function isValidFolderRoot(raw: string): boolean {
  const path = normalizeFolderRoot(raw);
  if (!path.startsWith("/") || path.length > MAX_LEN) {
    return false;
  }
  for (let i = 0; i < path.length; i++) {
    const code = path.charCodeAt(i);
    if (code < 0x20 || code === 0x7f) {
      return false;
    }
  }
  const doubled = path.startsWith("//");
  const rest = doubled ? path.slice(2) : path.slice(1);
  if (!rest || rest.endsWith("/")) {
    return false;
  }
  return rest.split("/").every((part) => part.length > 0 && part !== "." && part !== "..");
}

/** True when a save would not change the stored folder root. */
export function folderRootsMatch(saved: string, draft: string): boolean {
  return normalizeFolderRoot(saved).toLowerCase() === normalizeFolderRoot(draft).toLowerCase();
}

export type SiteFolderRootHttpFailure = "bad_request" | "forbidden" | "conflict";

/** HTTP statuses that must stay on the form and must not be treated as success. */
export function siteFolderRootHttpFailure(status: number): SiteFolderRootHttpFailure | null {
  if (status === 400) {
    return "bad_request";
  }
  if (status === 403) {
    return "forbidden";
  }
  if (status === 409) {
    return "conflict";
  }
  return null;
}
