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

import type { DesignGapWire } from "./designGaps";
import type { SlotAssociationSummary } from "./types";

/**
 * Coerce a Jackson list field to a real array.
 *
 * <p>Truthy non-arrays ({@code {empty:false}}, JAXB {@code {SlotAssociation:…}},
 * a single object) must not reach {@code (x || []).map} — that TypeError
 * unmounts Developer (#3554 / #2908).
 */
export function asJacksonArray<T>(
  raw: unknown,
  wrapKeys: string[],
  isItem?: (obj: Record<string, unknown>) => boolean,
): T[] {
  if (raw == null) {
    return [];
  }
  if (Array.isArray(raw)) {
    return raw as T[];
  }
  if (typeof raw !== "object") {
    return [];
  }
  const obj = raw as Record<string, unknown>;
  for (const k of wrapKeys) {
    if (!(k in obj)) {
      continue;
    }
    const inner = obj[k];
    if (inner == null) {
      continue;
    }
    if (Array.isArray(inner)) {
      return inner as T[];
    }
    if (typeof inner === "object") {
      return [inner as T];
    }
  }
  if ("empty" in obj && typeof obj.empty === "boolean") {
    const rest = Object.keys(obj).filter((k) => k !== "empty");
    if (rest.length === 0) {
      return [];
    }
  }
  if (isItem && isItem(obj)) {
    return [raw as T];
  }
  return [];
}

/** Slot {@code associations} — array, JAXB envelope, single object, or empty bean. */
export function normalizeSlotAssociations(raw: unknown): SlotAssociationSummary[] {
  return asJacksonArray<SlotAssociationSummary>(
    raw,
    ["SlotAssociation", "SlotAssociationSummary", "associations", "Association"],
    (o) =>
      "contentTypeGuid" in o ||
      "templateGuid" in o ||
      "contentTypeId" in o ||
      "templateId" in o,
  );
}

/** Slot {@code designGaps} — array, JAXB envelope, single {@code {code,message}}, or empty bean. */
export function normalizeSlotDesignGaps(raw: unknown): DesignGapWire[] {
  return asJacksonArray<DesignGapWire>(
    raw,
    ["DesignGap", "designGap", "designGaps"],
    (o) => "code" in o || "message" in o,
  );
}

function stringifyMapValue(val: unknown): string {
  if (val == null) {
    return "";
  }
  if (typeof val === "string" || typeof val === "number" || typeof val === "boolean") {
    return String(val);
  }
  if (typeof val === "object" && val !== null && "$" in val) {
    const inner = (val as { $?: unknown }).$;
    return inner == null ? "" : String(inner);
  }
  return "";
}

function isJaxbMapEntry(value: unknown): value is { key?: unknown; value?: unknown } {
  return value != null && typeof value === "object" && !Array.isArray(value) && "key" in value;
}

/**
 * Coerce {@code finderArguments} (and similar string maps) to {@code Record<string, string>}.
 *
 * <p>JAXB/Jackson often emits {@code { entry: [{key, value}, …] }} or a single
 * {@code { entry: {key, value} }}. Rendering those objects as React children
 * throws and the section boundary replaces Slots (#3554).
 */
export function normalizeSlotStringMap(raw: unknown): Record<string, string> {
  if (raw == null || typeof raw !== "object" || Array.isArray(raw)) {
    return {};
  }
  const obj = raw as Record<string, unknown>;
  if ("entry" in obj) {
    const rawEntries = obj.entry;
    const items = Array.isArray(rawEntries)
      ? rawEntries
      : isJaxbMapEntry(rawEntries)
        ? [rawEntries]
        : [];
    const out: Record<string, string> = {};
    for (const item of items) {
      if (!isJaxbMapEntry(item) || item.key == null) {
        continue;
      }
      out[String(item.key)] = stringifyMapValue(item.value);
    }
    return out;
  }
  const out: Record<string, string> = {};
  for (const [k, v] of Object.entries(obj)) {
    if (v != null && typeof v === "object") {
      continue;
    }
    if (v == null) {
      continue;
    }
    out[k] = String(v);
  }
  return out;
}
