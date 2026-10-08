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

import type {
  NamedObjectRef,
  TemplateBindingSummary,
  TemplateDetail,
  TemplateSlotSummary,
} from "../api/developer/types";
import { cloneNamedObjectRefs } from "./contentTypeWorkflows";

export type TemplateDescriptionWrite = { description: string };

export type TemplateDescriptionRejection = "unchanged";

const OMITTED_ON_DESCRIPTION_WRITE = [
  "name",
  "label",
  "templateSource",
  "assembler",
  "mimeType",
  "bindings",
  "slots",
  "associatedContentTypes",
] as const;

/**
 * Stored template description with surrounding space and line breaks removed.
 * A blank value is {@code ""} and clears a stored description.
 */
export function storedTemplateDescription(value?: string | null): string {
  return (value ?? "").replace(/[\r\n]+/g, " ").trim();
}

/**
 * Body for the existing template update that sets the description.
 *
 * <p>Name, label, source, assembler, bindings, slots, and content-type
 * associations are omitted. The update leaves an omitted field unchanged.
 * A blank description is an empty string and clears a stored description.
 * The same description is not a write.
 */
export function templateDescriptionWrite(
  baseline: Pick<TemplateDetail, "description">,
  nextDescription: string,
): TemplateDescriptionWrite | TemplateDescriptionRejection {
  const description = storedTemplateDescription(nextDescription);
  if (description === storedTemplateDescription(baseline.description)) {
    return "unchanged";
  }
  return { description };
}

function asRecord(value: unknown): Record<string, unknown> | null {
  if (value != null && typeof value === "object" && !Array.isArray(value)) {
    return value as Record<string, unknown>;
  }
  return null;
}

/**
 * Accept a flat detail or a {@code TemplateDetail} envelope. Kept local so
 * panel tests that mock {@code assemblyApi} still see the real response.
 */
function unwrapDetail(payload: unknown): TemplateDetail | null {
  const root = asRecord(payload);
  if (!root) return null;
  const nested = asRecord(root.TemplateDetail ?? root.templateDetail);
  if (nested) return nested as TemplateDetail;
  if (
    "name" in root ||
    "templateId" in root ||
    "templateSource" in root ||
    "label" in root ||
    "bindings" in root ||
    "description" in root ||
    "guid" in root ||
    "guidString" in root
  ) {
    return root as TemplateDetail;
  }
  return null;
}

function asBindingList(raw: unknown): TemplateBindingSummary[] {
  if (Array.isArray(raw)) return raw as TemplateBindingSummary[];
  const env = asRecord(raw);
  if (!env) return [];
  const nested = env.Binding ?? env.TemplateBinding;
  if (Array.isArray(nested)) return nested as TemplateBindingSummary[];
  if (nested && typeof nested === "object") return [nested as TemplateBindingSummary];
  // Jackson emits one binding as the object itself, not a one-element array.
  if ("variable" in env || "expression" in env || "executionOrder" in env) {
    return [env as TemplateBindingSummary];
  }
  return [];
}

function asSlotList(raw: unknown): TemplateSlotSummary[] {
  if (Array.isArray(raw)) return raw as TemplateSlotSummary[];
  const env = asRecord(raw);
  if (!env) return [];
  const nested = env.Slot ?? env.TemplateSlot;
  if (Array.isArray(nested)) return nested as TemplateSlotSummary[];
  if (nested && typeof nested === "object") return [nested as TemplateSlotSummary];
  return [];
}

function sourceText(raw: unknown): string {
  return typeof raw === "string" ? raw : raw == null ? "" : String(raw);
}

function sameSorted(left: string[], right: string[]): boolean {
  if (left.length !== right.length) return false;
  const a = [...left].sort();
  const b = [...right].sort();
  return a.every((value, index) => value === b[index]);
}

function bindingPrints(raw: unknown): string[] {
  return asBindingList(raw).map(
    (b) => `${b.executionOrder ?? ""}|${b.variable || ""}|${b.expression || ""}`,
  );
}

function slotPrints(raw: unknown): string[] {
  return asSlotList(raw)
    .map((s) => {
      const name = (s.name || "").trim();
      if (name) return `name:${name}`;
      if (s.guid?.stringValue) return `guid:${s.guid.stringValue}`;
      if (s.guid?.uuid != null) return `uuid:${s.guid.uuid}`;
      return "";
    })
    .filter(Boolean);
}

function contentTypePrints(raw: unknown): string[] {
  return cloneNamedObjectRefs(raw)
    .map((r: NamedObjectRef) => {
      if (r.name) return `name:${r.name.trim().toLowerCase()}`;
      if (r.guid?.stringValue) return `guid:${r.guid.stringValue}`;
      if (r.guid?.uuid != null) return `uuid:${r.guid.uuid}`;
      return "";
    })
    .filter(Boolean);
}

/**
 * Description to show after a description-only update, or null when the
 * response must not replace the previous description (name, label, source,
 * bindings, slots, or content-type associations changed, or the description
 * is not the one sent). The sent body must omit those other fields.
 */
export function savedTemplateDescription(
  sent: TemplateDescriptionWrite,
  previous: TemplateDetail,
  payload: unknown,
): string | null {
  for (const key of OMITTED_ON_DESCRIPTION_WRITE) {
    if (key in sent) return null;
  }
  if (!("description" in sent)) return null;
  const saved = unwrapDetail(payload);
  if (!saved || typeof saved !== "object") return null;
  const sentDescription = storedTemplateDescription(sent.description);
  if (storedTemplateDescription(saved.description) !== sentDescription) return null;
  const savedName = (saved.name || "").trim();
  const previousName = (previous.name || "").trim();
  if (!savedName || (previousName && savedName !== previousName)) return null;
  if (storedTemplateDescription(saved.label) !== storedTemplateDescription(previous.label)) {
    return null;
  }
  if ((saved.assembler || "").trim() !== (previous.assembler || "").trim()) return null;
  if (sourceText(saved.templateSource) !== sourceText(previous.templateSource)) return null;
  if (!sameSorted(bindingPrints(previous.bindings), bindingPrints(saved.bindings))) return null;
  if (!sameSorted(slotPrints(previous.slots), slotPrints(saved.slots))) return null;
  if (
    !sameSorted(
      contentTypePrints(previous.associatedContentTypes),
      contentTypePrints(saved.associatedContentTypes),
    )
  ) {
    return null;
  }
  return sentDescription;
}
