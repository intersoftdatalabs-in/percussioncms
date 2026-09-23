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
 * Client-side managed-link / page-link checks for EditorHost save (#4753).
 * Blank clears the field. A target is a content id, a hyphenated content GUID,
 * or a site folder path. Slot relationships are not reordered here.
 */

import type { EditorWidgetKind } from "./controlKinds";

export interface EditorLinkRow {
  name: string;
  kind: EditorWidgetKind;
  value: string;
}

const CONTENT_ID = /^[0-9]{1,18}$/;
const CONTENT_GUID = /^[0-9]+-[0-9]+-[0-9]+$/;
const SITE_PATH = /^\/\/[A-Za-z0-9._~-]+(?:\/[A-Za-z0-9._~-]+)+$/;
const FOLDER_PATH = /^\/(?:[A-Za-z0-9._~-]+\/)*[A-Za-z0-9._~-]+$/;

export function linkFieldProblem(value: string): "invalid" | null {
  const text = (value ?? "").trim();
  if (!text) {
    return null;
  }
  if (text.includes("..") || text.includes("\\") || /\s/.test(text)) {
    return "invalid";
  }
  if (/^[a-z][a-z0-9+.-]*:/i.test(text)) {
    return "invalid";
  }
  if (CONTENT_ID.test(text) || CONTENT_GUID.test(text)) {
    return null;
  }
  if (text.startsWith("//")) {
    return SITE_PATH.test(text) ? null : "invalid";
  }
  if (text.startsWith("/")) {
    return FOLDER_PATH.test(text) ? null : "invalid";
  }
  return "invalid";
}

export function collectInvalidLinkFieldErrors(
  rows: readonly EditorLinkRow[],
  invalidMessage: string,
): Record<string, string> {
  const out: Record<string, string> = {};
  for (const row of rows) {
    if (row.kind !== "link") {
      continue;
    }
    if (linkFieldProblem(row.value) === "invalid") {
      out[row.name] = invalidMessage;
    }
  }
  return out;
}
