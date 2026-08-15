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
 * Item revisions and workflow audit (comments) for Explorer.
 *
 * <p>Server: {@code GET /services/itemmanagement/item/revisions/{id}} and
 * {@code GET /services/itemmanagement/item/restoreRevision/{revisionGuid}}.
 */

import { get } from "../client";
import { PATHS } from "../paths";

export interface ItemRevision {
  revId: number;
  lastModifiedDate: string;
  lastModifier: string;
  status: string;
}

export interface ItemAuditComment {
  comment: string;
  commenter: string;
  commentType: string;
  commentDate: string;
}

export interface ItemRevisionsSummary {
  restorable: boolean;
  revisions: ItemRevision[];
  comments: ItemAuditComment[];
}

function asRecord(value: unknown): Record<string, unknown> | null {
  if (value != null && typeof value === "object" && !Array.isArray(value)) {
    return value as Record<string, unknown>;
  }
  return null;
}

function unwrapRoot(payload: unknown): Record<string, unknown> | null {
  const obj = asRecord(payload);
  if (obj == null) {
    return null;
  }
  const inner = obj.RevisionsSummary;
  if (inner && typeof inner === "object" && !Array.isArray(inner)) {
    return inner as Record<string, unknown>;
  }
  return obj;
}

function asString(value: unknown): string {
  if (value == null) {
    return "";
  }
  if (typeof value === "string") {
    return value;
  }
  if (value instanceof Date) {
    return value.toISOString();
  }
  return String(value);
}

function parseRevision(raw: unknown): ItemRevision | null {
  const o = asRecord(raw);
  if (o == null) {
    return null;
  }
  const revId = Number(o.revId ?? o.RevId);
  if (!Number.isFinite(revId) || revId <= 0) {
    return null;
  }
  return {
    revId,
    lastModifiedDate: asString(o.lastModifiedDate ?? o.LastModifiedDate),
    lastModifier: asString(o.lastModifier ?? o.LastModifier),
    status: asString(o.status ?? o.Status),
  };
}

/** CXF/Jettison may emit a lone object instead of a one-element array. */
function asItemList(raw: unknown): unknown[] {
  if (raw == null) {
    return [];
  }
  if (Array.isArray(raw)) {
    return raw;
  }
  if (typeof raw === "object") {
    const rec = raw as Record<string, unknown>;
    const nested = rec.Revision ?? rec.revision ?? rec.Comment ?? rec.comment;
    if (Array.isArray(nested)) {
      return nested;
    }
    if (nested != null && typeof nested === "object") {
      return [nested];
    }
    return [raw];
  }
  return [];
}

function parseComment(raw: unknown): ItemAuditComment | null {
  const o = asRecord(raw);
  if (o == null) {
    return null;
  }
  return {
    comment: asString(o.comment ?? o.Comment),
    commenter: asString(o.commenter ?? o.Commenter),
    commentType: asString(o.commentType ?? o.CommentType),
    commentDate: asString(o.commentDate ?? o.CommentDate),
  };
}

export function unwrapRevisionsSummary(
  payload: unknown,
): ItemRevisionsSummary {
  const inner = unwrapRoot(payload);
  if (inner == null) {
    return { restorable: false, revisions: [], comments: [] };
  }
  const restorable = Boolean(inner.restorable ?? inner.Restorable);
  const revRaw = inner.revisions ?? inner.Revisions;
  const comRaw = inner.comments ?? inner.Comments;
  const revisions = asItemList(revRaw)
    .map(parseRevision)
    .filter((r): r is ItemRevision => r != null);
  const comments = asItemList(comRaw)
    .map(parseComment)
    .filter((c): c is ItemAuditComment => c != null);
  return { restorable, revisions, comments };
}

/**
 * Encode a restore-revision GUID the way CM1 does: first GUID segment is
 * the revision. Numeric content ids become {@code {rev}-101-{id}}.
 */
export function buildRestoreRevisionId(itemId: string, revId: number): string {
  const trimmed = String(itemId ?? "").trim();
  if (!trimmed || !Number.isFinite(revId) || revId <= 0) {
    throw new Error("item id and revision are required");
  }
  if (trimmed.includes("-")) {
    const parts = trimmed.split("-");
    parts[0] = String(revId);
    return parts.join("-");
  }
  return `${revId}-101-${trimmed}`;
}

export async function fetchItemRevisions(
  itemId: string,
): Promise<ItemRevisionsSummary> {
  const id = encodeURIComponent(String(itemId).trim());
  const res = await get<unknown>(`${PATHS.ITEM_REVISIONS}/${id}`);
  return unwrapRevisionsSummary(res);
}

export async function restoreItemRevision(
  itemId: string,
  revId: number,
): Promise<void> {
  const guid = buildRestoreRevisionId(itemId, revId);
  await get<unknown>(
    `${PATHS.ITEM_RESTORE_REVISION}/${encodeURIComponent(guid)}`,
  );
}
