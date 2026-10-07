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
 * Item properties display title (#5246 / #5297 / parent #4530).
 *
 * <p>A title-only save posts the loaded item name so the name does not change.
 * An empty display title is a clear: the name stays, and the committed title
 * becomes empty only after save and reload. Cancel and HTTP 400/403/409 keep
 * the previous title. Folders are not offered the clear.</p>
 */

export type ItemPropertiesSavePlan =
  | { ok: false; reason: "blank-name" }
  | {
      ok: true;
      itemPath: string;
      name: string;
      displayTitle: string;
      /** True when the posted name differs from the loaded name. */
      nameChanged: boolean;
    };

export function planItemPropertiesSave(input: {
  itemPath: string;
  loadedName: string;
  draftName: string;
  draftDisplayTitle: string;
}): ItemPropertiesSavePlan {
  const name = String(input.draftName ?? "").trim();
  if (!name) {
    return { ok: false, reason: "blank-name" };
  }
  const loadedName = String(input.loadedName ?? "").trim();
  return {
    ok: true,
    itemPath: input.itemPath,
    name,
    displayTitle: input.draftDisplayTitle,
    nameChanged: name !== loadedName,
  };
}

/**
 * Path to read back after a successful save. A renamed item leaves the old
 * folder path, so a reload of that path is 404 and the folder list never
 * refreshes (#4701 / #5246).
 */
export function itemPropertiesPathAfterSave(
  itemPath: string,
  savedName: string,
  nameChanged: boolean,
): string {
  const path = String(itemPath ?? "").trim();
  if (!nameChanged) {
    return path;
  }
  const name = String(savedName ?? "").trim();
  if (!path || !name) {
    return path;
  }
  const slash = path.lastIndexOf("/");
  if (slash < 0) {
    return name;
  }
  return `${path.slice(0, slash + 1)}${name}`;
}

export type DisplayTitleAttempt =
  | { outcome: "saved"; reloadedTitle: string }
  | { outcome: "cancelled" }
  | { outcome: "http"; http: 400 | 403 | 409 | "other" }
  | { outcome: "reload-failed" };

/**
 * The panel shows a display title only after save and reload succeed.
 * Cancel, HTTP failures, and a failed reload keep the previous title.
 */
export function committedDisplayTitleAfterAttempt(
  previous: string,
  attempt: DisplayTitleAttempt,
): string {
  if (attempt.outcome !== "saved") {
    return previous;
  }
  return attempt.reloadedTitle;
}

/**
 * Clear is only for an editable page, file, or asset. Folders are not offered it.
 */
export function offerClearDisplayTitle(input: {
  isFolder: boolean;
  canEdit: boolean;
}): boolean {
  return !input.isFolder && input.canEdit;
}

/**
 * Empty the draft display title without writing. The name is unchanged.
 * Dirty stays true when the committed title was non-empty or the name draft differs.
 */
export function displayTitleDraftAfterClear(input: {
  committedName: string;
  draftName: string;
  committedDisplayTitle: string;
}): { displayTitle: string; dirty: boolean } {
  const nameDirty =
    String(input.draftName ?? "").trim() !==
    String(input.committedName ?? "").trim();
  const titleWillChange = String(input.committedDisplayTitle ?? "") !== "";
  return { displayTitle: "", dirty: nameDirty || titleWillChange };
}

/** HTTP 400, 403, and 409 must put the previous display title back in the field. */
export function displayTitleDraftAfterFailure(
  previous: string,
  draft: string,
  status: number | undefined,
): string {
  if (status === 400 || status === 403 || status === 409) {
    return previous;
  }
  return draft;
}
