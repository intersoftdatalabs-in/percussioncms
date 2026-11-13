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
 * Create a brand-new item from the related-content panel and attach it
 * as a slot relationship in one flow (#6264). Cancel never calls either
 * endpoint. A failed create never reaches the attach POST, so no phantom
 * relation is recorded and the open item draft is untouched.
 */

import { isApiError } from "../api/client";
import type { SlotRelationship } from "../api/contentExplorer/slotRelationshipApi";
import {
  buildEditorCreateRequest,
  createFolderContainsNul,
  createNameContainsNul,
} from "./editorCreate";
import type { ItemCreateRequest, ItemCreateResult } from "./itemCreateApi";
import {
  relatedInsertErrorReason,
  type RelatedInsertErrorReason,
} from "./editorRelatedContent";

export type RelatedQuickAddStage = "create" | "attach";

export type RelatedQuickAddErrorReason =
  | "incomplete"
  | "name_nul"
  | "folder_nul"
  | "bad_request"
  | "forbidden"
  | "not_found"
  | "conflict"
  | "failed";

export type RelatedQuickAddSave =
  | { ok: true; posted: true; itemId: string }
  | {
      ok: false;
      posted: boolean;
      stage: RelatedQuickAddStage | "validate";
      reason: RelatedQuickAddErrorReason;
      /** Set when create succeeded but attach failed. */
      itemId?: string;
    };

function httpStatus(err: unknown): number | null {
  if (isApiError(err)) {
    return err.status;
  }
  if (
    err != null &&
    typeof err === "object" &&
    "status" in err &&
    typeof (err as { status: unknown }).status === "number"
  ) {
    return (err as { status: number }).status;
  }
  return null;
}

/** 400 / 403 / 404 / 409 are failures, not success. Anything else is generic. */
export function relatedQuickAddErrorReason(
  err: unknown,
): Exclude<RelatedQuickAddErrorReason, "incomplete" | "name_nul" | "folder_nul"> {
  const status = httpStatus(err);
  if (status === 400) {
    return "bad_request";
  }
  if (status === 403) {
    return "forbidden";
  }
  if (status === 404) {
    return "not_found";
  }
  if (status === 409) {
    return "conflict";
  }
  return "failed";
}

/** Attach uses the same status map as insert; 409 cannot succeed either. */
export function relatedQuickAddAttachReason(
  err: unknown,
): RelatedInsertErrorReason | "conflict" | "failed" {
  const status = httpStatus(err);
  if (status === 409) {
    return "conflict";
  }
  return relatedInsertErrorReason(err);
}

/**
 * Create the item, then attach it to the slot. Validation failures never
 * POST. A create failure never reaches the attach POST. An attach failure
 * keeps the created item id so the panel can say the item exists but the
 * relation was not recorded.
 */
export async function saveRelatedQuickAdd(input: {
  ownerId: number;
  slotId: number;
  templateId: number;
  contentType: string;
  folderPath: string;
  name?: string;
  create: (request: ItemCreateRequest) => Promise<ItemCreateResult>;
  insert: (request: {
    ownerId: number;
    dependentId: number;
    slotId: number;
    templateId: number;
  }) => Promise<SlotRelationship>;
}): Promise<RelatedQuickAddSave> {
  if (createNameContainsNul(input.name)) {
    return { ok: false, posted: false, stage: "validate", reason: "name_nul" };
  }
  if (createFolderContainsNul(input.folderPath)) {
    return {
      ok: false,
      posted: false,
      stage: "validate",
      reason: "folder_nul",
    };
  }
  const request = buildEditorCreateRequest(
    input.contentType,
    input.folderPath,
    input.name,
  );
  if (request == null) {
    return { ok: false, posted: false, stage: "validate", reason: "incomplete" };
  }
  if (
    !Number.isFinite(input.ownerId) ||
    input.ownerId <= 0 ||
    !Number.isFinite(input.slotId) ||
    input.slotId <= 0 ||
    !Number.isFinite(input.templateId) ||
    input.templateId <= 0
  ) {
    return { ok: false, posted: false, stage: "validate", reason: "incomplete" };
  }
  let created: ItemCreateResult;
  try {
    created = await input.create(request);
  } catch (err) {
    return {
      ok: false,
      posted: true,
      stage: "create",
      reason: relatedQuickAddErrorReason(err),
    };
  }
  const dependentId = Number(created.itemId);
  if (!Number.isFinite(dependentId) || dependentId <= 0) {
    return { ok: false, posted: true, stage: "create", reason: "failed" };
  }
  try {
    await input.insert({
      ownerId: input.ownerId,
      dependentId,
      slotId: input.slotId,
      templateId: input.templateId,
    });
  } catch (err) {
    return {
      ok: false,
      posted: true,
      stage: "attach",
      reason: relatedQuickAddAttachReason(err),
      itemId: created.itemId,
    };
  }
  return { ok: true, posted: true, itemId: created.itemId };
}
