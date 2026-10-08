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

export type SlotDescriptionWrite = { description: string };

export type SlotDescriptionRejection = "unchanged";

const OMITTED_ON_DESCRIPTION_WRITE = [
  "name",
  "label",
  "slotType",
  "finderName",
  "relationshipName",
  "finderArguments",
  "associations",
  "slotLayout",
  "slotStyles",
] as const;

/**
 * Stored slot description with surrounding space and line breaks removed.
 * A blank value is {@code ""} and clears a stored description.
 */
export function storedSlotDescription(value?: string | null): string {
  return (value ?? "").replace(/[\r\n]+/g, " ").trim();
}

/**
 * Body for the existing slot update that sets the description.
 *
 * <p>Name, label, type, finder, relationship, arguments, and associations are
 * omitted. The update leaves an omitted field unchanged. A blank description
 * is an empty string and clears a stored description. The same description
 * is not a write.
 */
export function slotDescriptionWrite(
  baseline: Pick<SlotDetail, "description">,
  nextDescription: string,
): SlotDescriptionWrite | SlotDescriptionRejection {
  const description = storedSlotDescription(nextDescription);
  if (description === storedSlotDescription(baseline.description)) {
    return "unchanged";
  }
  return { description };
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
 * Description to show after a description-only update, or null when the
 * response must not replace the previous description (name, label, type, or
 * finder changed, or the description is not the one sent). The sent body
 * must omit those other fields.
 */
export function savedSlotDescription(
  sent: SlotUpdateBody,
  previous: SlotDetail,
  payload: unknown,
): string | null {
  for (const key of OMITTED_ON_DESCRIPTION_WRITE) {
    if (key in sent) return null;
  }
  if (!("description" in sent)) return null;
  const saved = unwrapSlotDetail(payload);
  if (!saved || typeof saved !== "object") return null;
  const sentDescription = storedSlotDescription(sent.description);
  if (storedSlotDescription(saved.description) !== sentDescription) return null;
  const savedName = (saved.name || "").trim();
  const previousName = (previous.name || "").trim();
  if (!savedName || (previousName && savedName !== previousName)) return null;
  if (storedSlotDescription(saved.label) !== storedSlotDescription(previous.label)) return null;
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
  return sentDescription;
}
