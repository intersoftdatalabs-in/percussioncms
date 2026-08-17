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
 * Map known scalar itemmanagement fields onto assembled preview nodes
 * and persist edits through the same fields API as the React editor.
 * Does not open leftover Active Assembly or Content Editor HTML.
 */

import type { ContentTypeFieldSummary } from "../api/developer/types";
import { classifyEditorControl } from "../editor/controlKinds";
import type { ItemEditorField, ItemEditorFields } from "../editor/itemFieldsApi";

export type OverlayFieldKind = "text" | "longtext";

export interface OverlayField {
  name: string;
  value: string;
  label: string;
  kind: OverlayFieldKind;
  readOnly: boolean;
}

export interface OverlayFieldHit {
  contentId: string;
  name: string;
  element: Element;
  source: "marker" | "aa-object-id" | "value";
}

export interface OverlayFieldEdit {
  contentId: string;
  name: string;
  value: string;
}

/** PSAAObjectId JSON array: index 1 = content id, 11 = field name. */
export const AA_OBJECT_CONTENT_ID_INDEX = 1;
export const AA_OBJECT_FIELD_NAME_INDEX = 11;

const MARKER_ATTRS = [
  "data-perc-field",
  "data-field-name",
  "data-assembly-field",
  "data-field",
] as const;

const SKIP_VALUE_TAGS = new Set([
  "HTML",
  "HEAD",
  "BODY",
  "SCRIPT",
  "STYLE",
  "NOSCRIPT",
  "IFRAME",
  "SVG",
]);

export function isScalarOverlayKind(
  kind: string,
): kind is OverlayFieldKind {
  return kind === "text" || kind === "longtext";
}

/**
 * Scalar text / longtext rows from itemmanagement + content-type controls.
 * Rich / binary / keyword / community stay on the Content Editor host.
 */
export function scalarOverlayFields(
  payload: ItemEditorFields,
  schemaFields: ContentTypeFieldSummary[] = [],
): OverlayField[] {
  const byName = new Map(schemaFields.map((f) => [f.name ?? "", f]));
  const out: OverlayField[] = [];
  for (const field of payload.fields) {
    const schema = byName.get(field.name);
    const kind = classifyEditorControl(schema, field.name);
    if (!isScalarOverlayKind(kind) || schema?.readOnly === true) {
      continue;
    }
    out.push({
      name: field.name,
      value: field.value,
      label: schema?.label || field.name,
      kind,
      readOnly: false,
    });
  }
  return out;
}

export function decodeAaObjectIdText(raw: string): string {
  return raw
    .replace(/&quot;/gi, '"')
    .replace(/&#34;/g, '"')
    .replace(/&amp;/gi, "&");
}

/**
 * Parse a classic {@code PSAAObjectId} JSON-array id for content id + field.
 */
export function parseAaFieldObjectId(
  raw: string | null | undefined,
): { contentId: string; fieldName: string } | null {
  if (raw == null) {
    return null;
  }
  const text = decodeAaObjectIdText(raw).trim();
  if (!text.startsWith("[")) {
    return null;
  }
  try {
    const parsed: unknown = JSON.parse(text);
    if (!Array.isArray(parsed) || parsed.length <= AA_OBJECT_FIELD_NAME_INDEX) {
      return null;
    }
    const fieldName = String(parsed[AA_OBJECT_FIELD_NAME_INDEX] ?? "").trim();
    const contentId = String(parsed[AA_OBJECT_CONTENT_ID_INDEX] ?? "").trim();
    if (!fieldName || !contentId) {
      return null;
    }
    return { contentId, fieldName };
  } catch {
    return null;
  }
}

function knownFieldNames(fields: OverlayField[]): Set<string> {
  return new Set(fields.map((f) => f.name));
}

function markerFieldName(el: Element, known: Set<string>): string | null {
  for (const attr of MARKER_ATTRS) {
    const value = el.getAttribute(attr)?.trim() ?? "";
    if (value && known.has(value)) {
      return value;
    }
  }
  return null;
}

function ownerContentId(fields: OverlayField[], fallback: string): string {
  return fallback.trim();
}

/**
 * Drop leftover Dojo AA chrome (field images and {@code ps.aa} handlers)
 * so the overlay does not execute classic Active Assembly JS.
 */
export function stripLeftoverAaChrome(root: ParentNode): number {
  let removed = 0;
  const images = root.querySelectorAll("img.PsAaObjectImage");
  images.forEach((img) => {
    const host = img.closest("a") ?? img;
    host.remove();
    removed += 1;
  });
  const decorated = root.querySelectorAll("[onclick], [onmouseover], [onmouseout]");
  decorated.forEach((el) => {
    for (const attr of ["onclick", "onmouseover", "onmouseout"] as const) {
      const value = el.getAttribute(attr) ?? "";
      if (/ps\.(aa|DivActionHelper)/i.test(value)) {
        el.removeAttribute(attr);
      }
    }
  });
  return removed;
}

/**
 * Find assembled-page nodes that correspond to known scalar fields.
 * Prefers explicit markers and AA object ids, then unique text values.
 */
export function mapAssembledFieldElements(
  root: ParentNode,
  fields: OverlayField[],
  ownerId: string,
): OverlayFieldHit[] {
  const known = knownFieldNames(fields);
  if (known.size === 0) {
    return [];
  }
  const hits: OverlayFieldHit[] = [];
  const claimed = new Set<Element>();
  const owner = ownerContentId(fields, ownerId);

  const candidates = root.querySelectorAll("*");
  candidates.forEach((el) => {
    const marked = markerFieldName(el, known);
    if (marked) {
      claimed.add(el);
      hits.push({
        contentId: el.getAttribute("data-assembly-content-id")?.trim() || owner,
        name: marked,
        element: el,
        source: "marker",
      });
      return;
    }
    if (el.classList.contains("PsAaField")) {
      const parsed = parseAaFieldObjectId(el.getAttribute("id"));
      if (parsed && known.has(parsed.fieldName)) {
        claimed.add(el);
        hits.push({
          contentId: parsed.contentId,
          name: parsed.fieldName,
          element: el,
          source: "aa-object-id",
        });
      }
    }
  });

  for (const field of fields) {
    const value = field.value.trim();
    if (value.length < 2) {
      continue;
    }
    if (hits.some((h) => h.name === field.name && h.contentId === owner)) {
      continue;
    }
    const matches: Element[] = [];
    candidates.forEach((el) => {
      if (claimed.has(el) || SKIP_VALUE_TAGS.has(el.tagName)) {
        return;
      }
      if (el.childElementCount > 0) {
        return;
      }
      if ((el.textContent ?? "").trim() === value) {
        matches.push(el);
      }
    });
    if (matches.length === 1) {
      const el = matches[0];
      claimed.add(el);
      hits.push({
        contentId: owner,
        name: field.name,
        element: el,
        source: "value",
      });
    }
  }
  return hits;
}

export function applyFieldOverlay(
  root: ParentNode,
  fields: OverlayField[],
  ownerId: string,
): OverlayFieldHit[] {
  stripLeftoverAaChrome(root);
  const hits = mapAssembledFieldElements(root, fields, ownerId);
  for (const hit of hits) {
    const html = hit.element as HTMLElement;
    html.contentEditable = "true";
    html.setAttribute("data-assembly-field", hit.name);
    html.setAttribute("data-assembly-content-id", hit.contentId);
    html.setAttribute("data-testid", `assembly-inline-field-${hit.name}`);
    html.setAttribute("spellcheck", "false");
  }
  return hits;
}

export function readOverlayEdits(
  root: ParentNode,
  fallbackOwnerId: string,
): OverlayFieldEdit[] {
  const nodes = root.querySelectorAll("[data-assembly-field]");
  const edits: OverlayFieldEdit[] = [];
  nodes.forEach((el) => {
    const name = el.getAttribute("data-assembly-field")?.trim() ?? "";
    if (!name) {
      return;
    }
    edits.push({
      contentId:
        el.getAttribute("data-assembly-content-id")?.trim() || fallbackOwnerId,
      name,
      value: (el.textContent ?? "").trim(),
    });
  });
  return edits;
}

export function mergeOverlayEdits(
  payload: ItemEditorFields,
  edits: OverlayFieldEdit[],
): ItemEditorFields {
  const byName = new Map<string, string>();
  for (const edit of edits) {
    if (edit.contentId && edit.contentId !== String(payload.contentId)) {
      continue;
    }
    byName.set(edit.name, edit.value);
  }
  return {
    ...payload,
    fields: payload.fields.map((field: ItemEditorField) =>
      byName.has(field.name)
        ? { name: field.name, value: byName.get(field.name) ?? field.value }
        : field,
    ),
  };
}

export function groupOverlayEdits(
  edits: OverlayFieldEdit[],
): Map<string, OverlayFieldEdit[]> {
  const groups = new Map<string, OverlayFieldEdit[]>();
  for (const edit of edits) {
    const id = edit.contentId.trim();
    if (!id) {
      continue;
    }
    const list = groups.get(id) ?? [];
    list.push(edit);
    groups.set(id, list);
  }
  return groups;
}

export async function persistOverlayEdits(options: {
  ownerId: string;
  ownerPayload: ItemEditorFields;
  edits: OverlayFieldEdit[];
  loadFields: (itemId: string) => Promise<ItemEditorFields>;
  saveFields: (
    itemId: string,
    payload: ItemEditorFields,
  ) => Promise<ItemEditorFields>;
  checkout?: (itemId: string) => Promise<void>;
}): Promise<ItemEditorFields> {
  const groups = groupOverlayEdits(options.edits);
  if (groups.size === 0) {
    return options.saveFields(
      options.ownerId,
      options.ownerPayload,
    );
  }
  let ownerSaved = options.ownerPayload;
  for (const [itemId, group] of groups) {
    const isOwner = itemId === options.ownerId;
    const current = isOwner
      ? options.ownerPayload
      : await options.loadFields(itemId);
    if (!isOwner && options.checkout) {
      await options.checkout(itemId);
    }
    const merged = mergeOverlayEdits(current, group);
    const saved = await options.saveFields(itemId, merged);
    if (isOwner) {
      ownerSaved = saved;
    }
  }
  if (!groups.has(options.ownerId)) {
    return options.ownerPayload;
  }
  return ownerSaved;
}
