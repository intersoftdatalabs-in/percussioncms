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

import { normalizeDesignObjectGuid } from "../displayFormatGuid";
import { get, post, put } from "../client";
import { PATHS } from "../paths";
import type {
  ContentTypeDetail,
  ContentTypeFieldSummary,
  ContentTypeListEnvelope,
  ContentTypeSummary,
  NamedObjectRef,
} from "./types";

/** Jackson {@code WRAP_ROOT_VALUE} root for {@code ContentTypeDetail}. */
export const CONTENT_TYPE_DETAIL_ROOT = "ContentTypeDetail";

function asRecord(value: unknown): Record<string, unknown> | null {
  if (value != null && typeof value === "object" && !Array.isArray(value)) {
    return value as Record<string, unknown>;
  }
  return null;
}

function normalizeContentTypeSummary(item: ContentTypeSummary): ContentTypeSummary {
  return normalizeDesignObjectGuid(item);
}

/**
 * Normalize list responses that may be a bare array or a JAXB envelope.
 */
export function unwrapContentTypeList(payload: unknown): ContentTypeSummary[] {
  if (payload == null) {
    return [];
  }
  if (Array.isArray(payload)) {
    return (payload as ContentTypeSummary[]).map(normalizeContentTypeSummary);
  }
  if (typeof payload === "object") {
    const env = payload as ContentTypeListEnvelope & {
      contentType?: ContentTypeSummary[] | ContentTypeSummary;
    };
    const raw = env.ContentType ?? env.contentType;
    if (raw == null) {
      return [];
    }
    const list = Array.isArray(raw) ? raw : [raw];
    return list.map(normalizeContentTypeSummary);
  }
  return [];
}

/**
 * Normalize a content-type GET/PUT response to a flat {@link ContentTypeDetail}.
 *
 * <p>Prefers {@code { "ContentTypeDetail": { … } }} (Jackson WRAP_ROOT_VALUE);
 * also accepts a flat body. Fills {@code guid.stringValue} / {@code guidString}
 * from nested Guid parts (#3319).
 */
export function unwrapContentTypeDetail(payload: unknown): ContentTypeDetail {
  const root = asRecord(payload);
  if (!root) {
    return {};
  }
  const nested = asRecord(root[CONTENT_TYPE_DETAIL_ROOT] ?? root.contentTypeDetail);
  let body: ContentTypeDetail;
  if (nested) {
    body = nested as ContentTypeDetail;
  } else if (
    "name" in root ||
    "guid" in root ||
    "guidString" in root ||
    "fields" in root ||
    "label" in root
  ) {
    body = root as ContentTypeDetail;
  } else {
    return {};
  }
  return normalizeDesignObjectGuid(body);
}

/**
 * List content types available on the server.
 *
 * <p>Server: {@code GET /services/contenttypes} (ContentTypesResource).
 * Not a full design-object load — name/label/description/guid only.
 */
export async function listContentTypes(): Promise<ContentTypeSummary[]> {
  const payload = await get<unknown>(PATHS.CONTENT_TYPES);
  return unwrapContentTypeList(payload);
}

/**
 * Load design summary (fields) for one content type.
 *
 * <p>Server: {@code GET /services/contenttypes/{idOrName}} where idOrName is
 * uuid, guid string, or internal name.
 */
export async function getContentTypeDetail(
  idOrName: string,
): Promise<ContentTypeDetail> {
  const key = encodeURIComponent(idOrName);
  const payload = await get<unknown>(`${PATHS.CONTENT_TYPES}/${key}`);
  return unwrapContentTypeDetail(payload);
}

export type ContentTypeUpdateBody = {
  label?: string;
  description?: string;
  enabled?: boolean;
  fields?: Pick<ContentTypeFieldSummary, "name" | "searchable" | "required" | "occurrence">[];
  /** Omit to leave unchanged; non-null list is a full replace. */
  allowedWorkflows?: NamedObjectRef[];
  defaultWorkflow?: NamedObjectRef | null;
  /** Omit to leave unchanged; non-null list is a full replace. */
  allowedTemplates?: NamedObjectRef[];
};

/**
 * POST /services/contenttypes/{idOrName}/lock — self-only design-session lock.
 */
export async function lockContentType(idOrName: string): Promise<unknown> {
  const key = encodeURIComponent(idOrName);
  return post(`${PATHS.CONTENT_TYPES}/${key}/lock`);
}

/**
 * POST /services/contenttypes/{idOrName}/unlock — release a held design-session lock.
 */
export async function unlockContentType(idOrName: string): Promise<void> {
  const key = encodeURIComponent(idOrName);
  await post(`${PATHS.CONTENT_TYPES}/${key}/unlock`);
}

/**
 * PUT /services/contenttypes/{idOrName} — requires a held design lock; does not release it.
 *
 * <p>This client wraps lock → PUT → unlock so the Developer SPA save path keeps working until
 * lock-button chrome (#3744) lands. The server PUT is 409 unless the current user already holds
 * the lock.
 */
export async function updateContentTypeDetail(
  idOrName: string,
  body: ContentTypeUpdateBody,
): Promise<ContentTypeDetail> {
  await lockContentType(idOrName);
  try {
    const key = encodeURIComponent(idOrName);
    const payload = await put<unknown>(`${PATHS.CONTENT_TYPES}/${key}`, body);
    return unwrapContentTypeDetail(payload);
  } finally {
    await unlockContentType(idOrName).catch(() => undefined);
  }
}
