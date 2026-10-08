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

export type TemplateLabelWrite = { label: string };

export type TemplateLabelRejection = "unchanged";

const OMITTED_ON_LABEL_WRITE = [
  "name",
  "description",
  "templateSource",
  "assembler",
  "mimeType",
  "bindings",
  "slots",
  "associatedContentTypes",
] as const;

/**
 * Stored template label with surrounding space and line breaks removed.
 * A blank value is {@code ""}. The template name is not part of this value.
 */
export function storedTemplateLabel(value?: string | null): string {
  return (value ?? "").replace(/[\r\n]+/g, " ").trim();
}

function storedPlain(value?: string | null): string {
  return (value ?? "").replace(/[\r\n]+/g, " ").trim();
}

/**
 * Body for the existing template update that sets the label.
 *
 * <p>Name, description, source, assembler, bindings, slots, and content-type
 * associations are omitted. The update leaves an omitted field unchanged.
 * A blank label is an empty string. It does not send the name, so the name
 * stays. The same label is not a write.
 */
export function templateLabelWrite(
  baseline: Pick<TemplateDetail, "label">,
  nextLabel: string,
): TemplateLabelWrite | TemplateLabelRejection {
  const label = storedTemplateLabel(nextLabel);
  if (label === storedTemplateLabel(baseline.label)) {
    return "unchanged";
  }
  return { label };
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
 * Label to show after a label-only update, or null when the response must not
 * replace the previous label (name, description, source, bindings, slots, or
 * content-type associations changed, or the label is not the one sent).
 *
 * <p>A blank label does not clear the name. The assembly catalog returns the
 * name when the stored label is blank, so that echo is accepted and shown.
 * Any other label, or a missing name, is not success.
 */
export function savedTemplateLabel(
  sent: TemplateLabelWrite,
  previous: TemplateDetail,
  payload: unknown,
): string | null {
  for (const key of OMITTED_ON_LABEL_WRITE) {
    if (key in sent) return null;
  }
  if (!("label" in sent)) return null;
  const saved = unwrapDetail(payload);
  if (!saved || typeof saved !== "object") return null;
  const sentLabel = storedTemplateLabel(sent.label);
  const savedLabel = storedTemplateLabel(saved.label);
  const savedName = (saved.name || "").trim();
  const previousName = (previous.name || "").trim();
  if (!savedName || (previousName && savedName !== previousName)) return null;
  if (storedPlain(saved.description) !== storedPlain(previous.description)) return null;
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
  if (sentLabel === "") {
    if (savedLabel !== "" && savedLabel !== previousName) return null;
    return savedLabel;
  }
  if (savedLabel !== sentLabel) return null;
  return sentLabel;
}
