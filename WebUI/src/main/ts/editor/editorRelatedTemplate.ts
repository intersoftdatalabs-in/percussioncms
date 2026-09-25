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
 * Apply a snippet-template change on one related slot row.
 * Cancel never calls this. Inline rows and a missing template do not POST.
 */

import type { SlotRelationship } from "../api/contentExplorer/slotRelationshipApi";
import {
  canChangeRelatedSnippetTemplate,
  relatedInsertErrorReason,
  type RelatedContentRow,
  type RelatedInsertErrorReason,
} from "./editorRelatedContent";

export type RelatedTemplateSave =
  | { ok: true; posted: true; templateId: number }
  | {
      ok: false;
      posted: boolean;
      reason: RelatedInsertErrorReason | "not_slot" | "needs_template";
    };

export async function saveRelatedSnippetTemplate(input: {
  row: RelatedContentRow;
  templateId: number;
  change: (
    relationshipId: number,
    slotId: number,
    templateId: number,
  ) => Promise<SlotRelationship>;
}): Promise<RelatedTemplateSave> {
  if (!canChangeRelatedSnippetTemplate(input.row)) {
    return { ok: false, posted: false, reason: "not_slot" };
  }
  const templateId = Number(input.templateId);
  if (!Number.isFinite(templateId) || templateId <= 0) {
    return { ok: false, posted: false, reason: "needs_template" };
  }
  const relationshipId = Number(input.row.relationshipId);
  const slotId = Number(input.row.slotId);
  try {
    const updated = await input.change(relationshipId, slotId, templateId);
    const written =
      updated.templateId > 0 ? updated.templateId : templateId;
    return { ok: true, posted: true, templateId: written };
  } catch (err) {
    return {
      ok: false,
      posted: true,
      reason: relatedInsertErrorReason(err),
    };
  }
}
