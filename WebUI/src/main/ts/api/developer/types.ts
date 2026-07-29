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

/** Guid DTO from public REST (subset used by Developer module). */
export interface RestGuid {
  stringValue?: string;
  uuid?: number;
  longValue?: number;
  type?: number;
  hostId?: number;
  untypedString?: string;
}

/** Content type summary from {@code GET /services/contenttypes}. */
export interface ContentTypeSummary {
  guid?: RestGuid;
  objectType?: RestGuid;
  name?: string;
  label?: string;
  description?: string;
  newRequest?: string;
  queryRequest?: string;
  updateRequest?: string;
  hideFromMenu?: boolean;
}

/** Jackson / JAXB list envelope sometimes used by legacy REST resources. */
export interface ContentTypeListEnvelope {
  ContentType?: ContentTypeSummary[] | ContentTypeSummary;
}
