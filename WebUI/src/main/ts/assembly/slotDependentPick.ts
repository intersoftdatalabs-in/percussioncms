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
 * Content Browser → slot-add pick. Hosts open the browser; dispatch posts
 * via {@code addSlotRelationship} after this helper returns ids.
 */

import type { SelectionResult } from "../api/contentExplorer/types";
import type { SlotAllowedChoice } from "../api/contentExplorer/slotRelationshipApi";
import { parseExplorerContentId } from "../contentExplorer/menuCatalogLoad";
import type { AssemblySlotContext } from "./slotContext";

export interface SlotDependentPick {
  contentId: number;
  templateId: number;
  folderId?: number;
}

export interface SlotDependentPickerSession {
  slot: AssemblySlotContext;
  resolve: (value: SlotDependentPick | null) => void;
}

export function replaceSlotDependentPickerSession(
  previous: SlotDependentPickerSession | null,
  next: SlotDependentPickerSession,
): SlotDependentPickerSession {
  if (previous && previous !== next) {
    previous.resolve(null);
  }
  return next;
}

export function settleSlotDependentPickerSession(
  session: SlotDependentPickerSession | null,
  value: SlotDependentPick | null,
): null {
  session?.resolve(value);
  return null;
}

/** Optional folder id from a browser path item or host folder field. */
export function parseOptionalFolderId(
  raw: string | number | undefined | null,
): number | undefined {
  const id = parseExplorerContentId(raw ?? undefined);
  return id != null && id > 0 ? id : undefined;
}

/**
 * Map a Content Browser selection to slot-add ids. Cancel is {@code null}
 * (empty selection). Missing allowed templates yield {@code templateId: 0}
 * so dispatch can show needs-template instead of posting.
 */
export async function resolveSlotDependentPick(
  selection: SelectionResult,
  slotId: number,
  loadAllowedTemplates: (
    slotId: number,
    contentTypeId?: number | null,
  ) => Promise<SlotAllowedChoice[]>,
  folderId?: number,
): Promise<SlotDependentPick | null> {
  const first = selection.items[0];
  if (!first) {
    return null;
  }
  const contentId = parseExplorerContentId(first.id) ?? 0;
  const typeHint = parseExplorerContentId(first.contentTypeIds?.[0]);
  let templateId = 0;
  try {
    const templates = await loadAllowedTemplates(
      slotId,
      typeHint != null && typeHint > 0 ? typeHint : null,
    );
    templateId = templates[0]?.id ?? 0;
  } catch {
    templateId = 0;
  }
  const resolvedFolder = parseOptionalFolderId(folderId);
  return resolvedFolder != null
    ? { contentId, templateId, folderId: resolvedFolder }
    : { contentId, templateId };
}
