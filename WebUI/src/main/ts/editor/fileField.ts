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
 * Client-side NUL checks for the editor file field (#5588 / #4532).
 *
 * <p>A NUL cannot be stored in item field XML / JDBC text, so a file name that
 * contains U+0000 is refused before the save. The stored file name arrives in
 * the field value; a newly picked file arrives in {@code File.name}. The
 * displayed name is sanitized ({@code displayBinaryFileName}), so the gate must
 * read the raw value, not the rendered text.
 */

import type { EditorWidgetKind } from "./controlKinds";

export interface EditorFileRow {
  name: string;
  kind: EditorWidgetKind;
  value: string;
}

/** The NUL character that item field XML / JDBC text cannot store. */
export const FILE_FIELD_NUL = "\u0000";

/** True when a file name (or other file-field text) contains a NUL. */
export function fileFieldContainsNul(value: string | undefined | null): boolean {
  return (value ?? "").includes(FILE_FIELD_NUL);
}

/**
 * True when the name of a picked file contains a NUL. The name is sent to the
 * server as the upload file name, so it must be refused too.
 */
export function pickedFileNameContainsNul(file: File | null | undefined): boolean {
  if (!file) {
    return false;
  }
  return fileFieldContainsNul(file.name);
}

/**
 * NUL errors for file rows. The stored value and the picked file name are both
 * checked; a NUL in either refuses the save before the item-field PUT.
 */
export function collectFileNulFieldErrors(
  rows: readonly EditorFileRow[],
  pendingFiles: Record<string, File>,
  nulMessage: string,
): Record<string, string> {
  const out: Record<string, string> = {};
  for (const row of rows) {
    if (row.kind !== "file") {
      continue;
    }
    const pending = pendingFiles[row.name];
    if (fileFieldContainsNul(row.value) || pickedFileNameContainsNul(pending)) {
      out[row.name] = nulMessage;
    }
  }
  return out;
}
