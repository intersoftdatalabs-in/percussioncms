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
import type {
  ContentTypeChoiceCatalog,
  ContentTypeChoiceDefaultSelected,
  ContentTypeChoiceEntry,
  ContentTypeChoiceFilter,
  ContentTypeChoiceFilterField,
  ContentTypeChoiceNullEntry,
  ContentTypeChoiceTable,
} from "./types";

export const CHOICE_CATALOG_TYPES = [
  "none",
  "local",
  "global",
  "lookup",
  "internalLookup",
  "tableinfo",
] as const;

export type ChoiceCatalogType = (typeof CHOICE_CATALOG_TYPES)[number];

function asRecord(value: unknown): Record<string, unknown> | null {
  if (value != null && typeof value === "object" && !Array.isArray(value)) {
    return value as Record<string, unknown>;
  }
  return null;
}

function asTrimmed(value: unknown): string | undefined {
  if (typeof value !== "string") {
    return undefined;
  }
  const t = value.trim();
  return t.length > 0 ? t : undefined;
}

function asInt(value: unknown): number | undefined {
  if (typeof value === "number" && Number.isFinite(value)) {
    return value;
  }
  if (typeof value === "string" && value.trim()) {
    const n = Number(value);
    if (Number.isFinite(n)) {
      return n;
    }
  }
  return undefined;
}

function parseEntries(raw: unknown): ContentTypeChoiceEntry[] {
  return asJacksonArray<ContentTypeChoiceEntry>(
    raw,
    ["ContentTypeChoiceEntry", "contentTypeChoiceEntry", "entries"],
    (o) => "value" in o || "label" in o,
  ).map((e) => ({
    value: typeof e.value === "string" ? e.value : e.value == null ? "" : String(e.value),
    label: typeof e.label === "string" ? e.label : e.label == null ? "" : String(e.label),
  }));
}

function parseFilterFields(raw: unknown): ContentTypeChoiceFilterField[] {
  return asJacksonArray<ContentTypeChoiceFilterField>(
    raw,
    ["ContentTypeChoiceFilterField", "contentTypeChoiceFilterField", "dependentFields"],
    (o) => "fieldRef" in o || "dependencyType" in o,
  ).map((f) => ({
    fieldRef: typeof f.fieldRef === "string" ? f.fieldRef : "",
    dependencyType: typeof f.dependencyType === "string" ? f.dependencyType : "",
  }));
}

function parseDefaultSelected(raw: unknown): ContentTypeChoiceDefaultSelected[] {
  return asJacksonArray<ContentTypeChoiceDefaultSelected>(
    raw,
    ["ContentTypeChoiceDefaultSelected", "contentTypeChoiceDefaultSelected", "defaultSelected"],
    (o) => "type" in o || "text" in o || "sequence" in o,
  ).map((d) => {
    const out: ContentTypeChoiceDefaultSelected = {};
    if (typeof d.type === "string") {
      out.type = d.type;
    }
    const seq = asInt(d.sequence);
    if (seq != null) {
      out.sequence = seq;
    }
    if (typeof d.text === "string") {
      out.text = d.text;
    }
    return out;
  });
}

function parseTable(raw: unknown): ContentTypeChoiceTable | undefined {
  const rec = asRecord(raw);
  if (!rec) {
    return undefined;
  }
  const out: ContentTypeChoiceTable = {};
  const dataSource = asTrimmed(rec.dataSource);
  const tableName = asTrimmed(rec.tableName);
  const labelColumn = asTrimmed(rec.labelColumn);
  const valueColumn = asTrimmed(rec.valueColumn);
  if (dataSource) {
    out.dataSource = dataSource;
  }
  if (tableName) {
    out.tableName = tableName;
  }
  if (labelColumn) {
    out.labelColumn = labelColumn;
  }
  if (valueColumn) {
    out.valueColumn = valueColumn;
  }
  return Object.keys(out).length > 0 ? out : undefined;
}

function parseNullEntry(raw: unknown): ContentTypeChoiceNullEntry | undefined {
  const rec = asRecord(raw);
  if (!rec) {
    return undefined;
  }
  const out: ContentTypeChoiceNullEntry = {};
  if (typeof rec.value === "string") {
    out.value = rec.value;
  } else if (rec.value != null) {
    out.value = String(rec.value);
  }
  const label = asTrimmed(rec.label);
  if (label) {
    out.label = label;
  }
  const includeWhen = asTrimmed(rec.includeWhen);
  if (includeWhen) {
    out.includeWhen = includeWhen;
  }
  const sortOrder = asTrimmed(rec.sortOrder);
  if (sortOrder) {
    out.sortOrder = sortOrder;
  }
  return out;
}

function parseFilter(raw: unknown): ContentTypeChoiceFilter | undefined {
  const rec = asRecord(raw);
  if (!rec) {
    return undefined;
  }
  const out: ContentTypeChoiceFilter = {};
  const dependentFields = parseFilterFields(rec.dependentFields);
  if (dependentFields.length > 0) {
    out.dependentFields = dependentFields;
  }
  const lookupHref = asTrimmed(rec.lookupHref);
  if (lookupHref) {
    out.lookupHref = lookupHref;
  }
  const lookupName = asTrimmed(rec.lookupName);
  if (lookupName) {
    out.lookupName = lookupName;
  }
  return out.dependentFields || out.lookupHref || out.lookupName ? out : undefined;
}

/** Flatten GET/PUT {@code choices} JSON, including JAXB envelopes. */
export function parseChoiceCatalog(raw: unknown): ContentTypeChoiceCatalog | null {
  const rec = asRecord(raw);
  if (!rec) {
    return null;
  }
  const out: ContentTypeChoiceCatalog = {};
  const type = asTrimmed(rec.type);
  if (type) {
    out.type = type;
  }
  const globalId = asInt(rec.globalId);
  if (globalId != null) {
    out.globalId = globalId;
  }
  const sortOrder = asTrimmed(rec.sortOrder);
  if (sortOrder) {
    out.sortOrder = sortOrder;
  }
  const lookupHref = asTrimmed(rec.lookupHref);
  if (lookupHref) {
    out.lookupHref = lookupHref;
  }
  const lookupName = asTrimmed(rec.lookupName);
  if (lookupName) {
    out.lookupName = lookupName;
  }
  const entries = parseEntries(rec.entries);
  if (entries.length > 0) {
    out.entries = entries;
  }
  const table = parseTable(rec.table);
  if (table) {
    out.table = table;
  }
  const filter = parseFilter(rec.filter);
  if (filter) {
    out.filter = filter;
  }
  const nullEntry = parseNullEntry(rec.nullEntry);
  if (nullEntry) {
    out.nullEntry = nullEntry;
  }
  const defaultSelected = parseDefaultSelected(rec.defaultSelected);
  if (defaultSelected.length > 0) {
    out.defaultSelected = defaultSelected;
  }
  return Object.keys(out).length > 0 ? out : null;
}

export function cloneChoiceCatalog(
  catalog: ContentTypeChoiceCatalog | null | undefined,
): ContentTypeChoiceCatalog | null {
  if (catalog == null) {
    return null;
  }
  return parseChoiceCatalog(JSON.parse(JSON.stringify(catalog)));
}

export function isChoiceCatalogNone(
  catalog: ContentTypeChoiceCatalog | null | undefined,
): boolean {
  const type = catalog?.type?.trim().toLowerCase();
  return catalog == null || !type || type === "none";
}

function payloadEntries(catalog: ContentTypeChoiceCatalog): ContentTypeChoiceEntry[] {
  return (catalog.entries || [])
    .map((e) => ({
      value: (e.value || "").trim(),
      label: (e.label || "").trim(),
    }))
    .filter((e) => e.value.length > 0)
    .map((e) => ({
      value: e.value,
      label: e.label || e.value,
    }));
}

function payloadFilter(catalog: ContentTypeChoiceCatalog): ContentTypeChoiceFilter | undefined {
  const filter = catalog.filter;
  if (!filter) {
    return undefined;
  }
  const lookupHref = (filter.lookupHref || "").trim();
  const dependentFields = (filter.dependentFields || [])
    .map((f) => ({
      fieldRef: (f.fieldRef || "").trim(),
      dependencyType: (f.dependencyType || "").trim() || "optional",
    }))
    .filter((f) => f.fieldRef.length > 0);
  if (!lookupHref || dependentFields.length === 0) {
    return undefined;
  }
  const out: ContentTypeChoiceFilter = { lookupHref, dependentFields };
  const lookupName = (filter.lookupName || "").trim();
  if (lookupName) {
    out.lookupName = lookupName;
  }
  return out;
}

function payloadNullEntry(
  catalog: ContentTypeChoiceCatalog,
): ContentTypeChoiceNullEntry | undefined {
  const n = catalog.nullEntry;
  if (!n) {
    return undefined;
  }
  const out: ContentTypeChoiceNullEntry = {
    value: n.value == null ? "" : String(n.value),
  };
  const label = (n.label || "").trim();
  if (label) {
    out.label = label;
  }
  const includeWhen = (n.includeWhen || "").trim();
  if (includeWhen) {
    out.includeWhen = includeWhen;
  }
  const sortOrder = (n.sortOrder || "").trim();
  if (sortOrder) {
    out.sortOrder = sortOrder;
  }
  return out;
}

function payloadDefaultSelected(
  catalog: ContentTypeChoiceCatalog,
): ContentTypeChoiceDefaultSelected[] | undefined {
  const rows = (catalog.defaultSelected || [])
    .map((d) => {
      const type = (d.type || "").trim();
      if (!type) {
        return null;
      }
      const out: ContentTypeChoiceDefaultSelected = { type };
      if (type === "sequence" && d.sequence != null && Number.isFinite(d.sequence)) {
        out.sequence = d.sequence;
      }
      if (type === "text") {
        const text = (d.text || "").trim();
        if (!text) {
          return null;
        }
        out.text = text;
      }
      return out;
    })
    .filter((d): d is ContentTypeChoiceDefaultSelected => d != null);
  return rows.length > 0 ? rows : undefined;
}

/**
 * Wire {@code choices} for PUT .../controlProperties. {@code type: none} clears.
 * Callers omit this object entirely when the catalog is unchanged.
 */
export function toChoiceCatalogPayload(
  catalog: ContentTypeChoiceCatalog | null | undefined,
): ContentTypeChoiceCatalog {
  if (isChoiceCatalogNone(catalog)) {
    return { type: "none" };
  }
  const src = catalog as ContentTypeChoiceCatalog;
  const type = (src.type || "").trim();
  const out: ContentTypeChoiceCatalog = { type };
  const sortOrder = (src.sortOrder || "").trim() || "ascending";
  out.sortOrder = sortOrder;
  if (type.toLowerCase() === "global" && src.globalId != null) {
    out.globalId = src.globalId;
  }
  if (type.toLowerCase() === "local") {
    out.entries = payloadEntries(src);
  }
  if (type.toLowerCase() === "lookup" || type.toLowerCase() === "internallookup") {
    const href = (src.lookupHref || "").trim();
    if (href) {
      out.lookupHref = href;
    }
    const lookupName = (src.lookupName || "").trim();
    if (lookupName) {
      out.lookupName = lookupName;
    }
  }
  if (type.toLowerCase() === "tableinfo" && src.table) {
    const table: ContentTypeChoiceTable = {};
    const dataSource = (src.table.dataSource || "").trim();
    const tableName = (src.table.tableName || "").trim();
    const labelColumn = (src.table.labelColumn || "").trim();
    const valueColumn = (src.table.valueColumn || "").trim();
    if (dataSource) {
      table.dataSource = dataSource;
    }
    if (tableName) {
      table.tableName = tableName;
    }
    if (labelColumn) {
      table.labelColumn = labelColumn;
    }
    if (valueColumn) {
      table.valueColumn = valueColumn;
    }
    if (Object.keys(table).length > 0) {
      out.table = table;
    }
  }
  const nullEntry = payloadNullEntry(src);
  if (nullEntry) {
    out.nullEntry = nullEntry;
  }
  const defaultSelected = payloadDefaultSelected(src);
  if (defaultSelected) {
    out.defaultSelected = defaultSelected;
  }
  const filter = payloadFilter(src);
  if (filter) {
    out.filter = filter;
  }
  return out;
}

export function choiceCatalogsEqual(
  a: ContentTypeChoiceCatalog | null | undefined,
  b: ContentTypeChoiceCatalog | null | undefined,
): boolean {
  if (isChoiceCatalogNone(a) && isChoiceCatalogNone(b)) {
    return true;
  }
  if (isChoiceCatalogNone(a) || isChoiceCatalogNone(b)) {
    return false;
  }
  return JSON.stringify(toChoiceCatalogPayload(a)) === JSON.stringify(toChoiceCatalogPayload(b));
}

/** Client-side check before PUT so 400s from empty local/global catalogs stay in the SPA. */
export function choiceCatalogPayloadError(
  catalog: ContentTypeChoiceCatalog | null | undefined,
): string | null {
  if (isChoiceCatalogNone(catalog)) {
    return null;
  }
  const type = (catalog?.type || "").trim().toLowerCase();
  if (type === "local" && payloadEntries(catalog as ContentTypeChoiceCatalog).length === 0) {
    return "local-entries";
  }
  if (type === "global") {
    const id = catalog?.globalId;
    if (id == null || !Number.isFinite(id) || id < 0) {
      return "global-id";
    }
  }
  if (type === "lookup" || type === "internallookup") {
    if (!(catalog?.lookupHref || "").trim()) {
      return "lookup-href";
    }
  }
  if (type === "tableinfo") {
    const table = catalog?.table;
    if (
      !table ||
      !(table.tableName || "").trim() ||
      !(table.labelColumn || "").trim() ||
      !(table.valueColumn || "").trim()
    ) {
      return "table";
    }
  }
  return null;
}
