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

import type { ItemEditorField } from "./itemFieldsApi";

/**
 * Content-type fields marked read-only (#5345). The editor PUT omits them so a
 * client edit cannot replace the stored value. File and image widgets stay on
 * the binary API and are omitted here as well.
 */
export interface EditorSaveFieldRow {
  name: string;
  kind: string;
  readOnly?: boolean;
  value: string;
  numericInteger?: boolean;
  numericMinimum?: string;
  numericMaximum?: string;
}

export function schemaReadOnlyFieldNames(
  schema: readonly { name?: string; readOnly?: boolean }[] | undefined,
): ReadonlySet<string> {
  const names = new Set<string>();
  if (!schema) {
    return names;
  }
  for (const field of schema) {
    const name = (field.name ?? "").trim();
    if (name && field.readOnly === true) {
      names.add(name);
    }
  }
  return names;
}

export function fieldsForEditorSave(
  rows: readonly EditorSaveFieldRow[],
): ItemEditorField[] {
  const fields: ItemEditorField[] = [];
  for (const row of rows) {
    if (row.readOnly === true || row.kind === "file" || row.kind === "image") {
      continue;
    }
    if (row.kind === "number") {
      fields.push({
        name: row.name,
        value: row.value,
        dataType: row.numericInteger === false ? "float" : "integer",
        minimum: row.numericMinimum,
        maximum: row.numericMaximum,
      });
      continue;
    }
    if (row.kind === "link") {
      fields.push({ name: row.name, value: row.value, dataType: "link" });
      continue;
    }
    fields.push({ name: row.name, value: row.value });
  }
  return fields;
}
