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

import {
  unwrapContentTypeDetail,
  type ContentTypeUpdateBody,
} from "../api/developer/contentTypesApi";
import { normalizeContentTypeFields } from "../api/developer/contentTypeLists";
import type { ContentTypeDetail, ContentTypeFieldSummary } from "../api/developer/types";
import { storedContentTypeDescription } from "./contentTypeDescription";
import { storedContentTypeLabel } from "./contentTypeLabel";

export type ContentTypeSearchableField = {
  name: string;
  searchable: boolean;
  label: string;
  fieldType: string;
};

export type ContentTypeFieldSearchableWrite = {
  fields: [{ name: string; searchable: boolean }];
};

export type AcceptedFieldSearchable = {
  name: string;
  searchable: boolean;
};

export type ContentTypeFieldSearchableRejection = "unchanged" | "missing";

const OMITTED_ON_SEARCHABLE_WRITE = [
  "name",
  "label",
  "description",
  "enabled",
  "allowedWorkflows",
  "defaultWorkflow",
  "allowedTemplates",
] as const;

/**
 * Fields that can take a searchable editor. Local, shared, and system rows are included.
 * Type-level search indexing is a separate control and is not part of this list.
 */
export function contentTypeSearchableFields(
  detail: ContentTypeDetail | null | undefined,
): ContentTypeSearchableField[] {
  return normalizeContentTypeFields(detail?.fields)
    .map((field) => ({
      name: (field.name || "").trim(),
      searchable: field.searchable === true,
      label: (field.label || "").trim(),
      fieldType: (field.fieldType || "").trim(),
    }))
    .filter((field) => field.name.length > 0);
}

/**
 * Body for the existing content-type field update that sets one field searchable.
 *
 * <p>The body is only {@code fields: [{ name, searchable }]}. Label, description, enabled,
 * workflows, templates, and every other field are omitted, so they stay. Type-level search
 * indexing is not on this write. The same searchable flag is not a write.
 */
export function contentTypeFieldSearchableWrite(
  baseline: ContentTypeDetail | null | undefined,
  fieldName: string,
  nextSearchable: boolean,
): ContentTypeFieldSearchableWrite | ContentTypeFieldSearchableRejection {
  const name = (fieldName || "").trim();
  if (!name) {
    return "missing";
  }
  const field = normalizeContentTypeFields(baseline?.fields).find(
    (row) => (row.name || "").trim() === name,
  );
  if (!field) {
    return "missing";
  }
  if ((field.searchable === true) === nextSearchable) {
    return "unchanged";
  }
  return { fields: [{ name, searchable: nextSearchable }] };
}

function fieldIdentity(field: ContentTypeFieldSummary): string {
  return `${(field.fieldSet || "").trim()}\u0000${(field.name || "").trim()}`;
}

function sameFieldList(left: ContentTypeFieldSummary[], right: ContentTypeFieldSummary[]): boolean {
  if (left.length !== right.length) {
    return false;
  }
  return left.every((field, index) => fieldIdentity(field) === fieldIdentity(right[index]));
}

type SearchablePatch = { name: string; searchable: boolean };

function singleSearchablePatch(sent: ContentTypeUpdateBody): SearchablePatch | null {
  if (OMITTED_ON_SEARCHABLE_WRITE.some((key) => key in sent)) {
    return null;
  }
  const patches = sent.fields;
  if (!patches || patches.length !== 1) {
    return null;
  }
  const patch = patches[0];
  if (!patch || typeof patch.name !== "string" || typeof patch.searchable !== "boolean") {
    return null;
  }
  if (
    patch.required !== undefined ||
    patch.label !== undefined ||
    patch.occurrence !== undefined ||
    patch.sequence !== undefined
  ) {
    return null;
  }
  const name = patch.name.trim();
  if (!name) {
    return null;
  }
  return { name, searchable: patch.searchable };
}

function contentTypeShellUnchanged(
  previous: ContentTypeDetail,
  saved: ContentTypeDetail,
): boolean {
  const previousName = storedContentTypeLabel(previous.name);
  const savedName = storedContentTypeLabel(saved.name);
  if (!savedName || (previousName && savedName !== previousName)) {
    return false;
  }
  if (storedContentTypeLabel(saved.label) !== storedContentTypeLabel(previous.label)) {
    return false;
  }
  if (
    storedContentTypeDescription(saved.description) !==
    storedContentTypeDescription(previous.description)
  ) {
    return false;
  }
  return (previous.enabled !== false) === (saved.enabled !== false);
}

function fieldKind(field: ContentTypeFieldSummary): string {
  return (field.fieldType || "").toLowerCase();
}

function fieldShellStays(
  before: ContentTypeFieldSummary,
  after: ContentTypeFieldSummary,
): boolean {
  return (
    (before.label || "").trim() === (after.label || "").trim() &&
    fieldKind(before) === fieldKind(after) &&
    (before.required === true) === (after.required === true) &&
    (before.occurrence || "") === (after.occurrence || "")
  );
}

function acceptTargetSearchable(
  before: ContentTypeFieldSummary,
  after: ContentTypeFieldSummary,
  patch: SearchablePatch,
): AcceptedFieldSearchable | null {
  if (!fieldShellStays(before, after) || after.searchable !== patch.searchable) {
    return null;
  }
  return { name: patch.name, searchable: patch.searchable };
}

function otherFieldSearchableStays(
  before: ContentTypeFieldSummary,
  after: ContentTypeFieldSummary,
): boolean {
  return (before.searchable === true) === (after.searchable === true) && fieldShellStays(before, after);
}

/**
 * Searchable flag to show after a searchable-only update, or null when the response must
 * not replace the previous flag. The sent body must be one field's searchable flag. Label,
 * description, enabled, name, required, occurrence, and every other field stay. A response
 * that changes those, or that does not echo the sent flag, is not success.
 */
export function savedContentTypeFieldSearchable(
  sent: ContentTypeUpdateBody,
  previous: ContentTypeDetail,
  payload: unknown,
): AcceptedFieldSearchable | null {
  const patch = singleSearchablePatch(sent);
  if (!patch) {
    return null;
  }
  const saved = unwrapContentTypeDetail(payload);
  if (!saved || typeof saved !== "object" || !contentTypeShellUnchanged(previous, saved)) {
    return null;
  }
  const previousFields = normalizeContentTypeFields(previous.fields);
  const savedFields = normalizeContentTypeFields(saved.fields);
  if (!sameFieldList(previousFields, savedFields)) {
    return null;
  }
  let accepted: AcceptedFieldSearchable | null = null;
  for (let i = 0; i < previousFields.length; i++) {
    const before = previousFields[i];
    const after = savedFields[i];
    if ((before.name || "").trim() === patch.name) {
      const next = acceptTargetSearchable(before, after, patch);
      if (!next) {
        return null;
      }
      accepted = next;
      continue;
    }
    if (!otherFieldSearchableStays(before, after)) {
      return null;
    }
  }
  return accepted;
}

/**
 * Detail with one field's searchable flag replaced. Other fields, the label, the
 * description, and type-level search indexing (not stored on this detail) stay.
 */
export function contentTypeWithFieldSearchable(
  detail: ContentTypeDetail,
  accepted: AcceptedFieldSearchable,
): ContentTypeDetail {
  const fields = normalizeContentTypeFields(detail.fields).map((field) => {
    if ((field.name || "").trim() !== accepted.name) {
      return field;
    }
    return { ...field, searchable: accepted.searchable };
  });
  return { ...detail, fields };
}
