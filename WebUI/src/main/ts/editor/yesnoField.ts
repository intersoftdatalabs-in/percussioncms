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
 * Client-side yes-no ({@code sys_SingleCheckBox}) checks for EditorHost save
 * (#5559 / #5587 / #4532). Required-blank yes-no stays in the required-field
 * gate ({@code collectRequiredFieldErrors}).
 */

import type { EditorWidgetKind } from "./controlKinds";

export interface EditorYesNoRow {
  name: string;
  kind: EditorWidgetKind | string;
  value: string;
}

/**
 * A NUL cannot be stored in item field XML / JDBC text (#5559).
 * Required-blank yes-no stays in the required-field gate.
 * Number and keyword NUL stay on those collectors.
 */
export function yesNoContainsNul(value: string): boolean {
  return (value ?? "").includes("\u0000");
}

/** NUL only, and only yes-no rows. Empty required yes-no is not this check. */
export function collectYesNoNulFieldErrors(
  rows: readonly EditorYesNoRow[],
  invalidMessage: string,
): Record<string, string> {
  const out: Record<string, string> = {};
  for (const row of rows) {
    if (row.kind !== "yesno") {
      continue;
    }
    if (yesNoContainsNul(row.value)) {
      out[row.name] = invalidMessage;
    }
  }
  return out;
}
