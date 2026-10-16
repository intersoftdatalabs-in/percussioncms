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
import type { ContentTypeDetail } from "../api/developer/types";

export type ContentTypeLabelWrite = { label: string };

export type ContentTypeLabelRejection = "unchanged";

const OMITTED_ON_LABEL_WRITE = [
  "name",
  "description",
  "enabled",
  "fields",
  "allowedWorkflows",
  "defaultWorkflow",
  "allowedTemplates",
] as const;

/**
 * Stored content-type label with surrounding space and line breaks removed.
 * A blank value is {@code ""}. The content-type name is not part of this value.
 */
export function storedContentTypeLabel(value?: string | null): string {
  return (value ?? "").replace(/[\r\n]+/g, " ").trim();
}

/**
 * Label to show. A blank stored label shows the internal name and does not
 * mean the name was cleared.
 */
export function shownContentTypeLabel(label?: string | null, name?: string | null): string {
  const stored = storedContentTypeLabel(label);
  if (stored) {
    return stored;
  }
  return storedContentTypeLabel(name);
}

/**
 * Body for the existing content-type update that sets the label.
 *
 * <p>Name, description, enabled, fields, workflows, and templates are omitted.
 * The update leaves an omitted field unchanged. Enabled stays on its dedicated
 * PUT. A blank label is an empty string. It does not send the name, so the
 * name stays. The same label is not a write.
 */
export function contentTypeLabelWrite(
  baseline: Pick<ContentTypeDetail, "label">,
  nextLabel: string,
): ContentTypeLabelWrite | ContentTypeLabelRejection {
  const label = storedContentTypeLabel(nextLabel);
  if (label === storedContentTypeLabel(baseline.label)) {
    return "unchanged";
  }
  return { label };
}

function storedName(detail: Pick<ContentTypeDetail, "name">): string {
  return storedContentTypeLabel(detail.name);
}

function fieldNames(detail: ContentTypeDetail): string[] {
  return normalizeContentTypeFields(detail.fields)
    .map((field) => (field.name || "").trim())
    .filter(Boolean);
}

function sameNames(left: string[], right: string[]): boolean {
  if (left.length !== right.length) {
    return false;
  }
  return left.every((name, index) => name === right[index]);
}

/**
 * Label to show after a label-only update, or null when the response must not
 * replace the previous label (description, enabled, name, or fields changed,
 * or the label is not the one sent). The sent body must omit description and
 * enabled.
 *
 * <p>A blank label does not clear the name. The catalog shows the name when
 * the stored label is blank, so that echo is accepted and the name is shown.
 * Any other label, or a missing name, is not success.
 */
export function savedContentTypeLabel(
  sent: ContentTypeUpdateBody,
  previous: ContentTypeDetail,
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
  const saved = unwrapContentTypeDetail(payload);
  if (!saved || typeof saved !== "object") {
    return null;
  }
  const sentLabel = storedContentTypeLabel(sent.label);
  const savedLabel = storedContentTypeLabel(saved.label);
  const savedName = storedName(saved);
  const previousName = storedName(previous);
  if (!savedName || (previousName && savedName !== previousName)) {
    return null;
  }
  if (storedContentTypeLabel(saved.description) !== storedContentTypeLabel(previous.description)) {
    return null;
  }
  if ((previous.enabled !== false) !== (saved.enabled !== false)) {
    return null;
  }
  if (!sameNames(fieldNames(previous), fieldNames(saved))) {
    return null;
  }
  if (sentLabel === "") {
    if (savedLabel !== "" && savedLabel !== previousName) {
      return null;
    }
    return previousName || savedLabel;
  }
  if (savedLabel !== sentLabel) {
    return null;
  }
  return sentLabel;
}
