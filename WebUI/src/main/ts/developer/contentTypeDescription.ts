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

export type ContentTypeDescriptionWrite = { description: string };

export type ContentTypeDescriptionRejection = "unchanged";

const OMITTED_ON_DESCRIPTION_WRITE = [
  "name",
  "label",
  "enabled",
  "fields",
  "allowedWorkflows",
  "defaultWorkflow",
  "allowedTemplates",
] as const;

/**
 * Stored content-type description with surrounding space and line breaks removed.
 * A blank value is {@code ""} and clears a stored description.
 */
export function storedContentTypeDescription(value?: string | null): string {
  return (value ?? "").replace(/[\r\n]+/g, " ").trim();
}

/**
 * Body for the existing content-type update that sets the description.
 *
 * <p>Name, label, enabled, fields, workflows, and templates are omitted. The
 * update leaves an omitted field unchanged. Enabled stays on its dedicated
 * PUT. A blank description is an empty string and clears a stored description.
 * The same description is not a write.
 */
export function contentTypeDescriptionWrite(
  baseline: Pick<ContentTypeDetail, "description">,
  nextDescription: string,
): ContentTypeDescriptionWrite | ContentTypeDescriptionRejection {
  const description = storedContentTypeDescription(nextDescription);
  if (description === storedContentTypeDescription(baseline.description)) {
    return "unchanged";
  }
  return { description };
}

function storedName(detail: Pick<ContentTypeDetail, "name">): string {
  return (detail.name || "").trim();
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
 * Description to show after a description-only update, or null when the
 * response must not replace the previous description (label, enabled, name,
 * or fields changed, or the description is not the one sent). The sent body
 * must omit label and enabled.
 */
export function savedContentTypeDescription(
  sent: ContentTypeUpdateBody,
  previous: ContentTypeDetail,
  payload: unknown,
): string | null {
  for (const key of OMITTED_ON_DESCRIPTION_WRITE) {
    if (key in sent) {
      return null;
    }
  }
  if (!("description" in sent)) {
    return null;
  }
  const saved = unwrapContentTypeDetail(payload);
  if (!saved || typeof saved !== "object") {
    return null;
  }
  const sentDescription = storedContentTypeDescription(sent.description);
  if (storedContentTypeDescription(saved.description) !== sentDescription) {
    return null;
  }
  const savedName = storedName(saved);
  const previousName = storedName(previous);
  if (!savedName || (previousName && savedName !== previousName)) {
    return null;
  }
  if (storedContentTypeDescription(saved.label) !== storedContentTypeDescription(previous.label)) {
    return null;
  }
  if ((previous.enabled !== false) !== (saved.enabled !== false)) {
    return null;
  }
  if (!sameNames(fieldNames(previous), fieldNames(saved))) {
    return null;
  }
  return sentDescription;
}
