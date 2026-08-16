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

import type { ContentTypeFieldSummary } from "../api/developer/types";
import type { ItemEditorField, ItemEditorFields } from "./itemFieldsApi";

export type EditorWidgetKind =
  | "text"
  | "longtext"
  | "html"
  | "file"
  | "image"
  | "keyword"
  | "community";

export interface EditorFieldRow extends ItemEditorField {
  label: string;
  readOnly: boolean;
  kind: EditorWidgetKind;
}

function norm(value: string | undefined | null): string {
  return (value ?? "").trim().toLowerCase();
}

/**
 * Map a content-type control / field name onto a React editor widget.
 * Control names come from {@code GET /services/contenttypes/{type}}.
 */
export function classifyEditorControl(
  schema: ContentTypeFieldSummary | undefined,
  fieldName: string,
): EditorWidgetKind {
  const control = norm(schema?.control);
  const name = norm(fieldName || schema?.name);
  const dataType = norm(schema?.dataType);
  const sibling = /_(filename|ext|type|mime|size|width|height)$/.test(name);

  if (
    name === "sys_communityid" ||
    name.includes("community") ||
    control.includes("community")
  ) {
    return "community";
  }
  if (control.includes("keyword") || name.includes("keyword")) {
    return "keyword";
  }
  if (
    !sibling &&
    (control.includes("image") ||
      control.includes("webimage") ||
      name === "img" ||
      name.includes("image"))
  ) {
    return "image";
  }
  if (
    !sibling &&
    (control === "sys_file" ||
      control.includes("sys_file") ||
      control.endsWith("_file") ||
      name === "item_file_attachment" ||
      (name.includes("file") && !name.includes("profile")))
  ) {
    return "file";
  }
  if (
    control.includes("tinymce") ||
    control.includes("editlive") ||
    control.includes("html") ||
    dataType === "html"
  ) {
    return "html";
  }
  if (
    control.includes("textarea") ||
    dataType.includes("text") ||
    dataType === "maxtext"
  ) {
    return "longtext";
  }
  return "text";
}

export function isSchemaInjectedKind(kind: EditorWidgetKind): boolean {
  return (
    kind === "file" ||
    kind === "image" ||
    kind === "community" ||
    kind === "keyword" ||
    kind === "html"
  );
}

/**
 * Merge itemmanagement scalar fields with content-type controls.
 * Schema-only file/image/community/keyword/html rows are injected so
 * binary and sys_* community controls still render when omitted from GET fields.
 */
export function mergeEditorRows(
  payload: ItemEditorFields,
  schemaFields: ContentTypeFieldSummary[],
): EditorFieldRow[] {
  const byName = new Map(schemaFields.map((f) => [f.name ?? "", f]));
  const seen = new Set<string>();
  const rows: EditorFieldRow[] = [];

  for (const field of payload.fields) {
    const schema = byName.get(field.name);
    const kind = classifyEditorControl(schema, field.name);
    seen.add(field.name);
    rows.push({
      ...field,
      label: schema?.label || field.name,
      readOnly: schema?.readOnly === true,
      kind,
    });
  }

  for (const schema of schemaFields) {
    const name = (schema.name ?? "").trim();
    if (!name || seen.has(name)) {
      continue;
    }
    const kind = classifyEditorControl(schema, name);
    if (!isSchemaInjectedKind(kind)) {
      continue;
    }
    rows.push({
      name,
      value: "",
      label: schema.label || name,
      readOnly: schema.readOnly === true,
      kind,
    });
  }
  return rows;
}
