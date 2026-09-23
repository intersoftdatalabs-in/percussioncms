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
 * Client-side long-text checks for EditorHost save (#4726).
 * A NUL cannot be stored in item field XML / JDBC text.
 */

import type { EditorWidgetKind } from "./controlKinds";

export interface EditorLongTextRow {
  name: string;
  kind: EditorWidgetKind;
  value: string;
}

export function longTextContainsNul(value: string): boolean {
  return (value ?? "").includes("\u0000");
}

export function collectInvalidLongTextFieldErrors(
  rows: readonly EditorLongTextRow[],
  invalidMessage: string,
): Record<string, string> {
  const out: Record<string, string> = {};
  for (const row of rows) {
    if (row.kind !== "longtext") {
      continue;
    }
    if (longTextContainsNul(row.value)) {
      out[row.name] = invalidMessage;
    }
  }
  return out;
}
