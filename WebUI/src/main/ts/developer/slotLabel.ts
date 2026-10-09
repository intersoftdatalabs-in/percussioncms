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

import { unwrapSlotDetail } from "../api/developer/assemblyApi";
import { normalizeSlotStringMap, type SlotUpdateBody } from "../api/developer/slotLists";
import type { SlotDetail } from "../api/developer/types";

export type SlotLabelWrite = { label: string };

export type SlotLabelRejection = "unchanged";

const OMITTED_ON_LABEL_WRITE = [
  "name",
  "description",
  "slotType",
  "finderName",
  "relationshipName",
  "finderArguments",
  "associations",
  "slotLayout",
  "slotStyles",
] as const;

/**
 * Stored slot label with surrounding space and line breaks removed.
 * A blank value is {@code ""}. It is not the slot name.
 */
export function storedSlotLabel(value?: string | null): string {
  return (value ?? "").replace(/[\r\n]+/g, " ").trim();
}

function storedPlain(value?: string | null): string {
  return (value ?? "").replace(/[\r\n]+/g, " ").trim();
}

/**
 * Body for the existing slot update that sets the label.
 *
 * <p>Name, description, type, finder, relationship, arguments, and associations
 * are omitted. The update leaves an omitted field unchanged. A blank label is
 * an empty string. It does not send the name, so the name stays. The same
 * label is not a write.
 */
export function slotLabelWrite(
  baseline: Pick<SlotDetail, "label">,
  nextLabel: string,
): SlotLabelWrite | SlotLabelRejection {
  const label = storedSlotLabel(nextLabel);
  if (label === storedSlotLabel(baseline.label)) {
    return "unchanged";
  }
  return { label };
}

function storedType(value?: string | null): string {
  return (value ?? "").trim().toUpperCase();
}

function sameStringMap(left: Record<string, string>, right: Record<string, string>): boolean {
  const leftKeys = Object.keys(left).sort();
  const rightKeys = Object.keys(right).sort();
  if (leftKeys.length !== rightKeys.length) {
    return false;
  }
  return leftKeys.every((key, index) => key === rightKeys[index] && left[key] === right[key]);
}

/**
 * Label to show after a label-only update, or null when the response must not
 * replace the previous label (name, description, type, or finder changed, or
 * the label is not the one sent). The sent body must omit those other fields.
 *
 * <p>A blank label does not clear the name. The slot catalog returns the name
 * when the stored label is blank, so that echo is accepted and shown. Any
 * other label, or a missing or different name, is not success.
 */
export function savedSlotLabel(
  sent: SlotUpdateBody,
  previous: SlotDetail,
  payload: unknown,
): string | null {
  for (const key of OMITTED_ON_LABEL_WRITE) {
    if (key in sent) return null;
  }
  if (!("label" in sent)) return null;
  const saved = unwrapSlotDetail(payload);
  if (!saved || typeof saved !== "object") return null;
  const sentLabel = storedSlotLabel(sent.label);
  const savedLabel = storedSlotLabel(saved.label);
  const savedName = (saved.name || "").trim();
  const previousName = (previous.name || "").trim();
  if (!savedName || (previousName && savedName !== previousName)) return null;
  if (storedPlain(saved.description) !== storedPlain(previous.description)) return null;
  const previousType = storedType(previous.slotType);
  if (previousType && storedType(saved.slotType) !== previousType) return null;
  if ((saved.finderName || "").trim() !== (previous.finderName || "").trim()) return null;
  if ((saved.relationshipName || "").trim() !== (previous.relationshipName || "").trim()) {
    return null;
  }
  if (
    !sameStringMap(
      normalizeSlotStringMap(previous.finderArguments),
      normalizeSlotStringMap(saved.finderArguments),
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
