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

/** Single Explorer folder segment — no path separators or parent hops (#4637). */
export function isValidExplorerFolderName(name: string): boolean {
  const n = String(name ?? "").trim();
  if (!n || n === "." || n === "..") {
    return false;
  }
  if (n.includes("/") || n.includes("\\")) {
    return false;
  }
  if (n.length > 50) {
    return false;
  }
  return true;
}

/** Why a folder rename must not be sent. {@code unchanged} is not an error. */
export type FolderRenameFieldReason =
  | "blank"
  | "invalid"
  | "collision"
  | "unchanged";

/**
 * Client checks before folder rename. Collision is case-insensitive against
 * sibling names (the current name is excluded). Server HTTP 409 still applies
 * when the list of siblings is incomplete.
 */
export function folderRenameFieldReason(
  currentName: string,
  nextName: string,
  takenNames: readonly string[] = [],
): FolderRenameFieldReason | null {
  const next = String(nextName ?? "").trim();
  const current = String(currentName ?? "").trim();
  if (!next) {
    return "blank";
  }
  if (!isValidExplorerFolderName(next)) {
    return "invalid";
  }
  if (next.toLowerCase() === current.toLowerCase()) {
    return "unchanged";
  }
  const taken = new Set(
    takenNames
      .map((name) => String(name ?? "").trim().toLowerCase())
      .filter((name) => name.length > 0 && name !== current.toLowerCase()),
  );
  if (taken.has(next.toLowerCase())) {
    return "collision";
  }
  return null;
}
