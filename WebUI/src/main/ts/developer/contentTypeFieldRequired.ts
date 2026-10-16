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

export type LocalFieldRequired = {
  name: string;
  required: boolean;
  label: string;
};

export type ContentTypeFieldRequiredWrite = {
  fields: [{ name: string; required: boolean }];
};

export type AcceptedFieldRequired = {
  name: string;
  required: boolean;
  occurrence: string;
};

export type ContentTypeFieldRequiredRejection = "unchanged" | "not-local" | "missing";

const OMITTED_ON_REQUIRED_WRITE = [
  "name",
  "label",
  "description",
  "enabled",
  "allowedWorkflows",
  "defaultWorkflow",
  "allowedTemplates",
] as const;

const REQUIRED_OCCURRENCES = new Set(["required", "oneOrMore"]);

/**
 * Local fields that can take a required editor. Shared and system fields are omitted.
 */
export function localContentTypeFields(
  detail: ContentTypeDetail | null | undefined,
): LocalFieldRequired[] {
  return normalizeContentTypeFields(detail?.fields)
    .filter((field) => (field.fieldType || "").toLowerCase() === "local")
    .map((field) => ({
      name: (field.name || "").trim(),
      required: field.required === true,
      label: (field.label || "").trim(),
    }))
    .filter((field) => field.name.length > 0);
}

/**
 * Body for the existing content-type field update that sets one local field required.
 *
 * <p>The body is only {@code fields: [{ name, required }]}. Label, description, enabled,
 * workflows, and templates are omitted, so those stay. Other fields are omitted, so they
 * stay. The same required flag is not a write. Shared and system fields are not a write.
 */
export function contentTypeFieldRequiredWrite(
  baseline: ContentTypeDetail | null | undefined,
  fieldName: string,
  nextRequired: boolean,
): ContentTypeFieldRequiredWrite | ContentTypeFieldRequiredRejection {
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
  if ((field.fieldType || "").toLowerCase() !== "local") {
    return "not-local";
  }
  if ((field.required === true) === nextRequired) {
    return "unchanged";
  }
  return { fields: [{ name, required: nextRequired }] };
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

function storedText(value: string | null | undefined): string {
  return storedContentTypeLabel(value);
}

type RequiredPatch = { name: string; required: boolean };

function singleRequiredPatch(sent: ContentTypeUpdateBody): RequiredPatch | null {
  if (OMITTED_ON_REQUIRED_WRITE.some((key) => key in sent)) {
    return null;
  }
  const patches = sent.fields;
  if (!patches || patches.length !== 1) {
    return null;
  }
  const patch = patches[0];
  if (!patch || typeof patch.name !== "string" || typeof patch.required !== "boolean") {
    return null;
  }
  if (
    patch.searchable !== undefined ||
    patch.label !== undefined ||
    patch.occurrence !== undefined ||
    patch.sequence !== undefined
  ) {
    return null;
  }
  return { name: patch.name.trim(), required: patch.required };
}

function contentTypeShellUnchanged(
  previous: ContentTypeDetail,
  saved: ContentTypeDetail,
): boolean {
  const previousName = storedText(previous.name);
  const savedName = storedText(saved.name);
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

function labelsAndKindsMatch(
  before: ContentTypeFieldSummary,
  after: ContentTypeFieldSummary,
): boolean {
  return (before.label || "").trim() === (after.label || "").trim() && fieldKind(before) === fieldKind(after);
}

function acceptTargetRequired(
  before: ContentTypeFieldSummary,
  after: ContentTypeFieldSummary,
  patch: RequiredPatch,
): AcceptedFieldRequired | null {
  if (fieldKind(before) !== "local" || !labelsAndKindsMatch(before, after)) {
    return null;
  }
  if (after.required !== patch.required) {
    return null;
  }
  if (after.occurrence && REQUIRED_OCCURRENCES.has(after.occurrence) !== patch.required) {
    return null;
  }
  return {
    name: patch.name,
    required: patch.required,
    occurrence: after.occurrence || (patch.required ? "required" : "optional"),
  };
}

function otherFieldRequiredStays(
  before: ContentTypeFieldSummary,
  after: ContentTypeFieldSummary,
): boolean {
  return (
    (before.required === true) === (after.required === true) && labelsAndKindsMatch(before, after)
  );
}

/**
 * Required flag to show after a required-only update, or null when the response must not
 * replace the previous flag. The sent body must be one local field's required flag. Label,
 * description, enabled, name, and every other field stay. A response that changes those,
 * or that does not echo the sent flag, is not success.
 */
export function savedContentTypeFieldRequired(
  sent: ContentTypeUpdateBody,
  previous: ContentTypeDetail,
  payload: unknown,
): AcceptedFieldRequired | null {
  const patch = singleRequiredPatch(sent);
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
  let accepted: AcceptedFieldRequired | null = null;
  for (let i = 0; i < previousFields.length; i++) {
    const before = previousFields[i];
    const after = savedFields[i];
    if ((before.name || "").trim() === patch.name) {
      const next = acceptTargetRequired(before, after, patch);
      if (!next) {
        return null;
      }
      accepted = next;
      continue;
    }
    if (!otherFieldRequiredStays(before, after)) {
      return null;
    }
  }
  return accepted;
}

/**
 * Detail with one field's required flag replaced. Other fields, the label, and the
 * description stay.
 */
export function contentTypeWithFieldRequired(
  detail: ContentTypeDetail,
  accepted: AcceptedFieldRequired,
): ContentTypeDetail {
  const fields = normalizeContentTypeFields(detail.fields).map((field) => {
    if ((field.name || "").trim() !== accepted.name) {
      return field;
    }
    return { ...field, required: accepted.required, occurrence: accepted.occurrence };
  });
  return { ...detail, fields };
}
