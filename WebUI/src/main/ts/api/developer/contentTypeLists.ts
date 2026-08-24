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

import { asJacksonArray } from "./slotLists";
import type { DesignGapWire } from "./designGaps";
import type { ContentTypeFieldSummary, NamedObjectRef } from "./types";

/**
 * Content-type {@code designGaps} — array, JAXB envelope, single
 * {@code {code,message}}, or empty-collection bean.
 *
 * <p>Truthy non-arrays must not reach {@code (x || []).map} — that TypeError
 * unmounts Developer Content Types (#3712 / #3554 / #2908).
 */
export function normalizeContentTypeDesignGaps(raw: unknown): DesignGapWire[] {
  if (typeof raw === "string") {
    return raw.trim() ? [raw] : [];
  }
  return asJacksonArray<DesignGapWire>(
    raw,
    ["DesignGap", "designGap", "designGaps"],
    (o) => "code" in o || "message" in o,
  );
}

/** Content-type {@code fields} — array, JAXB envelope, single field, or empty bean. */
export function normalizeContentTypeFields(raw: unknown): ContentTypeFieldSummary[] {
  return asJacksonArray<ContentTypeFieldSummary>(
    raw,
    ["ContentTypeField", "contentTypeField", "fields"],
    (o) => "name" in o || "fieldType" in o || "dataType" in o || "label" in o,
  );
}

/**
 * Workflow / template association lists — array, JAXB {@code NamedObjectRef}
 * envelope, single ref object, or empty bean.
 */
export function normalizeNamedObjectRefs(raw: unknown): NamedObjectRef[] {
  return asJacksonArray<NamedObjectRef>(
    raw,
    ["NamedObjectRef", "namedObjectRef"],
    (o) => "name" in o || "label" in o || "guid" in o || "isDefault" in o,
  );
}

/**
 * {@code childFieldSets} string list — array, lone string, JAXB wrapper, or empty bean.
 */
export function normalizeContentTypeStringList(raw: unknown): string[] {
  if (raw == null) {
    return [];
  }
  if (typeof raw === "string") {
    return raw.trim() ? [raw] : [];
  }
  if (Array.isArray(raw)) {
    return raw.filter((item): item is string => typeof item === "string");
  }
  if (typeof raw !== "object") {
    return [];
  }
  const obj = raw as Record<string, unknown>;
  for (const k of ["childFieldSet", "childFieldSets", "string"]) {
    if (!(k in obj)) {
      continue;
    }
    const inner = obj[k];
    if (typeof inner === "string") {
      return inner.trim() ? [inner] : [];
    }
    if (Array.isArray(inner)) {
      return inner.filter((item): item is string => typeof item === "string");
    }
  }
  if ("empty" in obj && typeof obj.empty === "boolean") {
    const rest = Object.keys(obj).filter((k) => k !== "empty");
    if (rest.length === 0) {
      return [];
    }
  }
  return [];
}
