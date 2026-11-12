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

/**
 * Paste Explorer clipboard items as links into a selected Active Assembly
 * slot (#6103 / parent #6103).
 *
 * <p>Classic Active Assembly offered {@code Paste_As_Link_To_Slot}: the
 * copied item becomes a new snippet relationship in the slot instead of a
 * folder copy. Confirm posts the existing slot add
 * ({@code POST /services/assembly/slot-relationships}) once per clipboard
 * item with the slot's first allowed snippet template. Cancel, an empty
 * clipboard, a cut clipboard, a selection that is not a slot, and HTTP
 * 400/403/409 must not be reported as pasted. This is not the
 * relationships-panel add, not create-in-slot, not link-existing (that
 * types an id instead of using the clipboard), and not a move or
 * template-only change.</p>
 */

import type { PSExplorerRelationshipEdge } from "../api/contentExplorer/relationship";
import type { SlotRelationship } from "../api/contentExplorer/slotRelationshipApi";
import { applyLinkedItemToSlot } from "./linkExistingItemToSlot";

export type PasteLinkBlock =
  | "empty_clipboard"
  | "cut"
  | "not_slot"
  | "no_template";

export type PasteLinkGate =
  | {
      ok: true;
      ownerId: number;
      slotId: number;
      templateId: number;
      dependentIds: number[];
    }
  | { ok: false; reason: PasteLinkBlock };

export interface PasteLinkClipboard {
  operation: "copy" | "cut";
  contentIds: readonly number[];
}

/** Positive clipboard content ids, in clipboard order, without repeats. */
export function pasteLinkDependentIds(
  contentIds: readonly number[] | null | undefined,
): number[] {
  const ids: number[] = [];
  for (const raw of contentIds ?? []) {
    const id = Number(raw);
    if (!Number.isFinite(id) || id <= 0 || ids.includes(id)) {
      continue;
    }
    ids.push(id);
  }
  return ids;
}

/**
 * Whether confirm may post one slot add per clipboard item. The target
 * must be a slot that already groups Active Assembly rows. A folder row, a
 * translation row, or no selection is not a slot. The snippet template is
 * the slot's first allowed template, matching the server-driven
 * {@code paste_as_link_to_slot} dispatch. Cut items cannot be pasted as
 * links — cutting moves the item, so only a copy clipboard applies.
 */
export function gatePasteLinkToSlot(input: {
  clipboard: PasteLinkClipboard | null | undefined;
  ownerId: number;
  slotId: number;
  knownSlotIds: readonly number[];
  allowedTemplateIds: readonly number[];
}): PasteLinkGate {
  const ownerId = Number(input.ownerId);
  const slotId = Number(input.slotId);
  if (
    !Number.isFinite(ownerId) ||
    ownerId <= 0 ||
    !Number.isFinite(slotId) ||
    slotId <= 0 ||
    !input.knownSlotIds.some((id) => Number(id) === slotId)
  ) {
    return { ok: false, reason: "not_slot" };
  }
  const dependentIds = pasteLinkDependentIds(input.clipboard?.contentIds);
  if (dependentIds.length === 0) {
    return { ok: false, reason: "empty_clipboard" };
  }
  if (input.clipboard?.operation === "cut") {
    return { ok: false, reason: "cut" };
  }
  const templateId = Number(input.allowedTemplateIds[0]);
  if (!Number.isFinite(templateId) || templateId <= 0) {
    return { ok: false, reason: "no_template" };
  }
  return { ok: true, ownerId, slotId, templateId, dependentIds };
}

/**
 * Append pasted rows after successful slot adds. Call this only with
 * server responses that returned the requested slot and dependent. Other
 * rows are copied unchanged. A response with a non-positive relationship
 * id leaves the list as it was.
 */
export function applyPastedLinksToSlot(
  edges: readonly PSExplorerRelationshipEdge[],
  created: readonly SlotRelationship[],
  templateName: string,
): PSExplorerRelationshipEdge[] {
  return created.reduce(
    (acc, row) => applyLinkedItemToSlot(acc, row, templateName),
    edges.slice(),
  );
}
