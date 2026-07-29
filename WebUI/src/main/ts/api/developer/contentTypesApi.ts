/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

import { get } from "../client";
import { PATHS } from "../paths";
import type {
  ContentTypeDetail,
  ContentTypeListEnvelope,
  ContentTypeSummary,
} from "./types";

/**
 * Normalize list responses that may be a bare array or a JAXB envelope.
 */
export function unwrapContentTypeList(payload: unknown): ContentTypeSummary[] {
  if (payload == null) {
    return [];
  }
  if (Array.isArray(payload)) {
    return payload as ContentTypeSummary[];
  }
  if (typeof payload === "object") {
    const env = payload as ContentTypeListEnvelope & {
      contentType?: ContentTypeSummary[] | ContentTypeSummary;
    };
    const raw = env.ContentType ?? env.contentType;
    if (raw == null) {
      return [];
    }
    return Array.isArray(raw) ? raw : [raw];
  }
  return [];
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
 * Load read-only design summary (fields) for one content type.
 *
 * <p>Server: {@code GET /services/contenttypes/{idOrName}} where idOrName is
 * uuid, guid string, or internal name.
 */
export async function getContentTypeDetail(
  idOrName: string,
): Promise<ContentTypeDetail> {
  const key = encodeURIComponent(idOrName);
  return get<ContentTypeDetail>(`${PATHS.CONTENT_TYPES}/${key}`);
}
