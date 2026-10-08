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

import { unwrapDisplayFormat } from "../api/displayFormatGuid";
import { normalizeColumns, type DisplayFormatWriteBody } from "../api/developer/displayFormatsApi";
import type { DisplayFormat } from "../api/developer/types";
import { normalizeAllowedCommunities } from "./displayFormatCommunities";
import { columnsEditEqual } from "./displayFormatColumns";

export type DisplayFormatDescriptionRejection = "unchanged";

/**
 * Stored display-format description with surrounding space and line breaks removed.
 * A blank value is {@code ""} and clears a stored description.
 */
export function storedDisplayFormatDescription(value?: string | null): string {
  return (value ?? "").replace(/[\r\n]+/g, " ").trim();
}

/**
 * Body for the existing display-format update that sets the description.
 *
 * <p>Name, label, columns, and communities are omitted. The update leaves a
 * missing column list and a missing {@code allowedCommunities} list unchanged.
 * An empty column list would replace columns with {@code sys_title}, and an
 * empty {@code allowedCommunities} list means all communities, so those lists
 * are not sent. A blank description is an empty string and clears a stored
 * description. The same description is not a write.
 */
export function displayFormatDescriptionWrite(
  baseline: Pick<DisplayFormat, "description">,
  nextDescription: string,
): DisplayFormatWriteBody | DisplayFormatDescriptionRejection {
  const description = storedDisplayFormatDescription(nextDescription);
  if (description === storedDisplayFormatDescription(baseline.description)) {
    return "unchanged";
  }
  return { description };
}

function storedName(format: Pick<DisplayFormat, "name" | "internalName">): string {
  return (format.name || format.internalName || "").trim().toLowerCase();
}

function storedLabel(format: Pick<DisplayFormat, "label" | "displayName">): string {
  return storedDisplayFormatDescription(format.label || format.displayName || "");
}

function communityNames(raw: DisplayFormat["allowedCommunities"]): string[] {
  const map = normalizeAllowedCommunities(raw);
  const names = Object.values(map)
    .map((name) => name.trim().toLowerCase())
    .filter(Boolean);
  return [...new Set(names)].sort();
}

function sameNames(left: string[], right: string[]): boolean {
  if (left.length !== right.length) {
    return false;
  }
  return left.every((name, index) => name === right[index]);
}

/**
 * True when the response still has the column sources that were loaded.
 * A missing response list after a loaded list is a wipe. No loaded list has
 * nothing to protect.
 */
function columnsStayed(previous: DisplayFormat, saved: DisplayFormat): boolean {
  const prev = normalizeColumns(previous.columns);
  if (prev.length === 0) {
    return true;
  }
  const next = normalizeColumns(saved.columns);
  if (next.length === 0) {
    return false;
  }
  return columnsEditEqual(prev, next);
}

/**
 * Description to show after a description-only update, or null when the
 * response must not replace the previous description (name, label, columns,
 * or communities changed, or the description is not the one sent). The sent
 * body must omit columns and allowed communities.
 */
export function savedDisplayFormatDescription(
  sent: DisplayFormatWriteBody,
  previous: DisplayFormat,
  payload: unknown,
): string | null {
  if ("columns" in sent || "allowedCommunities" in sent || "name" in sent || "label" in sent) {
    return null;
  }
  const saved = unwrapDisplayFormat(payload);
  if (!saved || typeof saved !== "object") {
    return null;
  }
  const sentDescription = storedDisplayFormatDescription(sent.description);
  if (storedDisplayFormatDescription(saved.description) !== sentDescription) {
    return null;
  }
  const savedName = storedName(saved);
  const previousName = storedName(previous);
  if (!savedName || (previousName && savedName !== previousName)) {
    return null;
  }
  if (storedLabel(saved) !== storedLabel(previous)) {
    return null;
  }
  if (!columnsStayed(previous, saved)) {
    return null;
  }
  if (!sameNames(communityNames(previous.allowedCommunities), communityNames(saved.allowedCommunities))) {
    return null;
  }
  return sentDescription;
}
