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
import { storedDisplayFormatDescription } from "./displayFormatDescription";

export type DisplayFormatLabelWrite = { label: string };

export type DisplayFormatLabelRejection = "unchanged";

const OMITTED_ON_LABEL_WRITE = [
  "name",
  "internalName",
  "displayName",
  "description",
  "columns",
  "allowedCommunities",
  "sortedColumnNames",
  "ascendingSort",
  "descendingSort",
] as const;

/**
 * Stored display-format label with surrounding space and line breaks removed.
 * A blank value is {@code ""}. It is not the catalog name.
 */
export function storedDisplayFormatLabel(value?: string | null): string {
  return (value ?? "").replace(/[\r\n]+/g, " ").trim();
}

/**
 * Body for the existing display-format update that sets the label.
 *
 * <p>Name, description, columns, and communities are omitted. The update leaves
 * a missing column list and a missing {@code allowedCommunities} list unchanged.
 * An empty column list would replace columns with {@code sys_title}, and an
 * empty {@code allowedCommunities} list means all communities, so those lists
 * are not sent. A blank label is an empty string. It does not send the name,
 * so the name stays. The same label is not a write.
 */
export function displayFormatLabelWrite(
  baseline: Pick<DisplayFormat, "label" | "displayName">,
  nextLabel: string,
): DisplayFormatLabelWrite | DisplayFormatLabelRejection {
  const label = storedDisplayFormatLabel(nextLabel);
  const current = storedLabel(baseline);
  if (label === current) {
    return "unchanged";
  }
  return { label };
}

function storedName(format: Pick<DisplayFormat, "name" | "internalName">): string {
  return (format.name || format.internalName || "").trim();
}

function storedLabel(format: Pick<DisplayFormat, "label" | "displayName">): string {
  if (format.label != null) {
    return storedDisplayFormatLabel(format.label);
  }
  return storedDisplayFormatLabel(format.displayName);
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
 * Label to show after a label-only update, or null when the response must not
 * replace the previous label (name, description, columns, or communities
 * changed, or the label is not the one sent). The sent body must omit those
 * other fields.
 *
 * <p>A blank label does not clear the name. The catalog shows the name when
 * the stored label is blank, because a display format cannot persist an empty
 * display name. That echo is accepted and shown. Any other label, or a missing
 * or different name, is not success.
 */
export function savedDisplayFormatLabel(
  sent: DisplayFormatWriteBody,
  previous: DisplayFormat,
  payload: unknown,
): string | null {
  for (const key of OMITTED_ON_LABEL_WRITE) {
    if (key in sent) {
      return null;
    }
  }
  if (!("label" in sent)) {
    return null;
  }
  const saved = unwrapDisplayFormat(payload);
  if (!saved || typeof saved !== "object") {
    return null;
  }
  const sentLabel = storedDisplayFormatLabel(sent.label);
  const savedLabel = storedLabel(saved);
  const savedName = storedName(saved);
  const previousName = storedName(previous);
  if (!savedName || (previousName && savedName !== previousName)) {
    return null;
  }
  if (
    storedDisplayFormatDescription(saved.description) !==
    storedDisplayFormatDescription(previous.description)
  ) {
    return null;
  }
  if (!columnsStayed(previous, saved)) {
    return null;
  }
  if (
    !sameNames(communityNames(previous.allowedCommunities), communityNames(saved.allowedCommunities))
  ) {
    return null;
  }
  if (sentLabel === "") {
    if (savedLabel !== "" && savedLabel !== previousName) {
      return null;
    }
    return savedLabel;
  }
  if (savedLabel !== sentLabel) {
    return null;
  }
  return sentLabel;
}
