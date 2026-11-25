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
import { numericMetaForSchema } from "./numericField";

export type EditorWidgetKind =
  | "text"
  | "longtext"
  | "html"
  | "file"
  | "image"
  | "keyword"
  | "community"
  | "date"
  | "datetime"
  | "number"
  | "yesno"
  | "link"
  | "table";

export interface EditorFieldRow extends ItemEditorField {
  label: string;
  readOnly: boolean;
  kind: EditorWidgetKind;
  required: boolean;
  /** Whole number ({@code integer} / {@code number} / {@code sys_Number}). */
  numericInteger?: boolean;
  numericMinimum?: string;
  numericMaximum?: string;
}

function norm(value: string | undefined | null): string {
  return (value ?? "").trim().toLowerCase();
}

/** Content-type catalog: {@code required: true} or occurrence required / oneOrMore. */
export function isEditorFieldRequired(
  schema: ContentTypeFieldSummary | undefined | null,
): boolean {
  if (!schema) {
    return false;
  }
  if (schema.required === true) {
    return true;
  }
  const occurrence = (schema.occurrence ?? "").trim().toLowerCase();
  return occurrence === "required" || occurrence === "oneormore";
}

/**
 * Map a content-type control / field name onto a React editor widget.
 * Control names come from {@code GET /services/contenttypes/{type}}.
 *
 * Precedence is fixed (community > keyword > image > file > datetime >
 * date > html > number > yesno > table > link > longtext > text) and each
 * family helper below decides one step, so no helper branches on another
 * family's signals. Pure refactor of the former 41-branch classifier (#7247);
 * behavior is pinned by {@code controlKinds.test.ts}.
 */
export function classifyEditorControl(
  schema: ContentTypeFieldSummary | undefined,
  fieldName: string,
): EditorWidgetKind {
  const signals = controlSignals(schema, fieldName);
  return (
    communityKind(signals) ??
    keywordKind(signals) ??
    imageKind(signals) ??
    fileKind(signals) ??
    dateTimeKind(signals) ??
    dateKind(signals) ??
    richTextKind(signals) ??
    numberKind(signals) ??
    yesNoKind(signals) ??
    tableKind(signals) ??
    linkKind(signals) ??
    longTextKind(signals) ??
    "text"
  );
}

interface ControlSignals {
  control: string;
  name: string;
  dataType: string;
  sibling: boolean;
}

function controlSignals(
  schema: ContentTypeFieldSummary | undefined,
  fieldName: string,
): ControlSignals {
  const name = norm(fieldName || schema?.name);
  return {
    control: norm(schema?.control),
    name,
    dataType: norm(schema?.dataType),
    sibling: /_(filename|ext|type|mime|size|width|height|alt)$/.test(name),
  };
}

function communityKind({ control, name }: ControlSignals): EditorWidgetKind | null {
  if (name === "sys_communityid" || name.includes("community") || control.includes("community")) {
    return "community";
  }
  return null;
}

function keywordKind({ control, name }: ControlSignals): EditorWidgetKind | null {
  if (control.includes("keyword") || name.includes("keyword")) {
    return "keyword";
  }
  return null;
}

function imageKind({ control, name, sibling }: ControlSignals): EditorWidgetKind | null {
  if (sibling) {
    return null;
  }
  if (
    control.includes("image") ||
    control.includes("webimage") ||
    name === "img" ||
    name.includes("image")
  ) {
    return "image";
  }
  return null;
}

function fileKind({ control, name, sibling }: ControlSignals): EditorWidgetKind | null {
  if (sibling) {
    return null;
  }
  if (
    control === "sys_file" ||
    control.includes("sys_file") ||
    control.endsWith("_file") ||
    name === "item_file_attachment" ||
    (name.includes("file") && !name.includes("profile"))
  ) {
    return "file";
  }
  return null;
}

function dateTimeKind({ control, dataType }: ControlSignals): EditorWidgetKind | null {
  if (
    control.includes("datetime") ||
    dataType === "datetime" ||
    dataType.includes("timestamp")
  ) {
    return "datetime";
  }
  return null;
}

function dateKind({ control, dataType }: ControlSignals): EditorWidgetKind | null {
  if (
    control.includes("calendar") ||
    control.includes("sys_date") ||
    (control.includes("date") && !control.includes("update")) ||
    dataType === "date"
  ) {
    return "date";
  }
  return null;
}

function richTextKind({ control, dataType }: ControlSignals): EditorWidgetKind | null {
  // #6834 rename: the new emitted name maps to html; the legacy tinymce
  // alias keeps working until the installer upgrade task migrates stored types.
  if (
    control === "sys_default_richtext_editor" ||
    control.includes("tinymce") ||
    control.includes("editlive") ||
    control.includes("html") ||
    dataType === "html"
  ) {
    return "html";
  }
  return null;
}

function numberKind({ control, dataType }: ControlSignals): EditorWidgetKind | null {
  // sys_EditBox stays a single-line input unless the catalog data type is maxtext
  // (CLOB / long text). sys_TextArea and other textarea controls are always long text.
  if (
    dataType === "integer" ||
    dataType === "number" ||
    dataType === "float" ||
    control === "sys_number"
  ) {
    return "number";
  }
  return null;
}

function yesNoKind({ control, dataType }: ControlSignals): EditorWidgetKind | null {
  if (isYesNoControl(control, dataType)) {
    return "yesno";
  }
  return null;
}

function tableKind({ control, dataType }: ControlSignals): EditorWidgetKind | null {
  if (isTableControl(control, dataType)) {
    return "table";
  }
  return null;
}

function linkKind({ control, name, dataType }: ControlSignals): EditorWidgetKind | null {
  if (isLinkControl(control, name, dataType)) {
    return "link";
  }
  return null;
}

function longTextKind({ control, dataType }: ControlSignals): EditorWidgetKind | null {
  if (control.includes("textarea") || dataType === "maxtext") {
    return "longtext";
  }
  return null;
}

/** {@code sys_SingleCheckBox}. Group and tree checkboxes stay off this widget. */
function isYesNoControl(control: string, dataType: string): boolean {
  if (dataType === "boolean") {
    return true;
  }
  if (control.includes("group") || control.includes("tree")) {
    return false;
  }
  return control === "sys_singlecheckbox" || control.includes("singlecheckbox");
}

/** {@code sys_Table} grid. Related-content tables stay off this widget. */
function isTableControl(control: string, dataType: string): boolean {
  if (control.includes("related")) {
    return false;
  }
  if (dataType === "table") {
    return true;
  }
  return control === "sys_table" || control.includes("sys_table");
}

function isLinkControl(control: string, name: string, dataType: string): boolean {
  if (control.includes("related")) {
    return false;
  }
  if (dataType === "link") {
    return true;
  }
  const tokens = ["pagelink", "managedlink", "rxhyperlink", "sys_link", "sys_url"];
  return tokens.some((token) => control === token || control.includes(token) || name === token);
}

function numericRowFields(
  schema: ContentTypeFieldSummary | undefined,
  kind: EditorWidgetKind,
): Pick<EditorFieldRow, "numericInteger" | "numericMinimum" | "numericMaximum"> {
  if (kind !== "number") {
    return {};
  }
  const meta = numericMetaForSchema(schema);
  if (!meta) {
    return { numericInteger: true };
  }
  return {
    numericInteger: meta.integer,
    numericMinimum: meta.minimum,
    numericMaximum: meta.maximum,
  };
}

export function isSchemaInjectedKind(kind: EditorWidgetKind): boolean {
  return (
    kind === "file" ||
    kind === "image" ||
    kind === "community" ||
    kind === "keyword" ||
    kind === "html" ||
    kind === "date" ||
    kind === "datetime"
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
      required: isEditorFieldRequired(schema),
      kind,
      ...numericRowFields(schema, kind),
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
      required: isEditorFieldRequired(schema),
      kind,
    });
  }
  return rows;
}
