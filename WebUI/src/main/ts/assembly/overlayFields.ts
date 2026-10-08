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
 * Map known text, long-text, HTML, link, whole-number, and calendar-date
 * itemmanagement fields onto assembled preview nodes and persist edits through
 * the same fields API as the React editor. Does not open leftover Active
 * Assembly or Content Editor HTML. Datetime stays on the Content Editor.
 */

import type { ContentTypeFieldSummary } from "../api/developer/types";
import { classifyEditorControl } from "../editor/controlKinds";
import { toWidgetValue } from "../editor/dateField";
import type { ItemEditorField, ItemEditorFields } from "../editor/itemFieldsApi";

export type OverlayFieldKind = "text" | "longtext" | "html" | "link" | "number" | "date";

/** Assembled HTML nodes store markup in innerHTML, not stripped text. */
export const ASSEMBLY_VALUE_HTML = "html";

/** Link edits send {@code dataType: link} on the existing item field save. */
export const ASSEMBLY_VALUE_LINK = "link";

/** Single-line text is one line. Long text uses {@link ASSEMBLY_VALUE_LONGTEXT}. */
export const ASSEMBLY_VALUE_TEXT = "text";

/** Long text keeps line breaks. Single-line text is not marked with this value. */
export const ASSEMBLY_VALUE_LONGTEXT = "longtext";

/** Whole-number edits send {@code dataType: integer} on the item field save. */
export const ASSEMBLY_VALUE_NUMBER = "number";

/** Calendar-date edits send {@code dataType: date} and {@code yyyy-MM-dd}. */
export const ASSEMBLY_VALUE_DATE = "date";

const WHOLE_NUMBER_RE = /^-?\d+$/;

/**
 * Trimmed whole number, or null when the text is blank, a decimal, or not numeric.
 * Range checks and optional clear stay out of this helper.
 */
export function wholeNumberText(value: string): string | null {
  const text = value.trim();
  if (!WHOLE_NUMBER_RE.test(text)) {
    return null;
  }
  return text;
}

/**
 * One calendar date ({@code yyyy-MM-dd}), or null when blank, invalid, or a datetime.
 * A blank result must not be written over a stored date.
 */
export function calendarDateText(value: string): string | null {
  const text = value.trim();
  if (!text) {
    return null;
  }
  const widget = toWidgetValue("date", text);
  return widget === text ? text : null;
}

/**
 * Collapse line breaks so a single-line text field cannot store a new line.
 * Long text does not use this.
 */
export function singleLineText(value: string): string {
  return value.replace(/\s*[\r\n]+\s*/g, " ").trim();
}

/**
 * Normalize long text to `\n` line breaks and trim only the ends.
 * Internal breaks stay. Single-line text does not use this.
 */
export function longTextValue(value: string): string {
  return value.replace(/\r\n/g, "\n").replace(/\r/g, "\n").trim();
}

const LONG_TEXT_BLOCK =
  /^(ADDRESS|ARTICLE|BLOCKQUOTE|DD|DIV|DL|DT|FIGCAPTION|FIGURE|H1|H2|H3|H4|H5|H6|LI|P|PRE|SECTION|TR)$/;

/**
 * Read long text from an assembled node. A content-editable break is a
 * `<br>` or a block element; those must survive the item field save.
 */
export function longTextFromElement(el: Element): string {
  if (el.childElementCount === 0) {
    return longTextValue(el.textContent ?? "");
  }
  const parts: string[] = [];
  const appendBreak = (): void => {
    if (parts.length > 0 && !parts[parts.length - 1].endsWith("\n")) {
      parts.push("\n");
    }
  };
  const walk = (node: Node): void => {
    if (node.nodeType === Node.TEXT_NODE) {
      parts.push(node.textContent ?? "");
      return;
    }
    if (node.nodeType !== Node.ELEMENT_NODE) {
      return;
    }
    const child = node as Element;
    if (child.tagName === "BR") {
      parts.push("\n");
      return;
    }
    const block = LONG_TEXT_BLOCK.test(child.tagName);
    if (block) {
      appendBreak();
    }
    child.childNodes.forEach((grand) => walk(grand));
    if (block) {
      appendBreak();
    }
  };
  el.childNodes.forEach((node) => walk(node));
  return longTextValue(parts.join(""));
}

export function overlayEditKey(
  edit: Pick<OverlayFieldEdit, "contentId" | "name">,
): string {
  return `${edit.contentId}\n${edit.name}`;
}

/**
 * Edits whose value differs from the post-paint snapshot.
 * A field missing from the snapshot is kept. Unchanged fields are dropped so
 * one field save does not rewrite the other fields on the page.
 */
export function changedOverlayEdits(
  edits: OverlayFieldEdit[],
  baseline: ReadonlyMap<string, string>,
): OverlayFieldEdit[] {
  return edits.filter((edit) => {
    const previous = baseline.get(overlayEditKey(edit));
    return previous === undefined || previous !== edit.value;
  });
}

/**
 * Required single-line text fields whose current value is blank or whitespace.
 * Long text, HTML, and link are not checked here. Later edits for the same
 * name win so the overlay strip and the assembled node agree.
 */
export function blankRequiredTextFieldNames(
  fields: readonly Pick<OverlayField, "name" | "kind" | "required" | "value">[],
  edits: readonly Pick<OverlayFieldEdit, "name" | "value">[],
): string[] {
  const values = new Map<string, string>();
  for (const edit of edits) {
    values.set(edit.name, edit.value);
  }
  const names: string[] = [];
  for (const field of fields) {
    if (field.kind !== "text" || field.required !== true) {
      continue;
    }
    const value = values.has(field.name) ? (values.get(field.name) ?? "") : field.value;
    if (value.trim().length === 0) {
      names.push(field.name);
    }
  }
  return names;
}

/**
 * Required whole-number fields whose current value is blank or whitespace.
 * Optional numbers stay on {@link invalidChangedNumberFieldNames} so a clear
 * is not written. Decimals and non-numeric text are not named here. Long text,
 * HTML, link, date, and single-line text are not checked here. Later edits for
 * the same name win so the overlay strip and the assembled node agree.
 */
export function blankRequiredNumberFieldNames(
  fields: readonly (Pick<OverlayField, "name" | "kind" | "value"> & {
    required?: boolean;
  })[],
  edits: readonly Pick<OverlayFieldEdit, "name" | "value">[],
): string[] {
  const values = new Map<string, string>();
  for (const edit of edits) {
    values.set(edit.name, edit.value);
  }
  const names: string[] = [];
  for (const field of fields) {
    if (field.kind !== "number" || field.required !== true) {
      continue;
    }
    const value = values.has(field.name) ? (values.get(field.name) ?? "") : field.value;
    if (value.trim().length === 0) {
      names.push(field.name);
    }
  }
  return names;
}

/**
 * Number fields the author changed to a decimal, a non-numeric value, or an
 * optional blank. A blank or whitespace required number is not listed; that
 * refusal is {@link blankRequiredNumberFieldNames}. Unchanged values, including
 * a blank that was already stored, are not listed. Long text, HTML, link, and
 * single-line text are not checked here.
 */
export function invalidChangedNumberFieldNames(
  fields: readonly (Pick<OverlayField, "name" | "kind" | "value"> & {
    required?: boolean;
  })[],
  edits: readonly Pick<OverlayFieldEdit, "contentId" | "name" | "value">[],
  baseline: ReadonlyMap<string, string>,
): string[] {
  const latest = new Map<string, (typeof edits)[number]>();
  for (const edit of edits) {
    latest.set(edit.name, edit);
  }
  const names: string[] = [];
  for (const field of fields) {
    if (field.kind !== "number") {
      continue;
    }
    const edit = latest.get(field.name);
    if (!edit) {
      continue;
    }
    const previous = baseline.has(overlayEditKey(edit))
      ? (baseline.get(overlayEditKey(edit)) ?? "")
      : field.value;
    if (edit.value.trim() === previous.trim()) {
      continue;
    }
    if (field.required === true && edit.value.trim().length === 0) {
      continue;
    }
    if (wholeNumberText(edit.value) == null) {
      names.push(field.name);
    }
  }
  return names;
}

/**
 * Date fields the author changed to blank or to something other than one calendar date.
 * Unchanged values, including a blank that was already stored, are not listed.
 * A blank change is named so the save can leave the stored date in place.
 * Datetime text is not a calendar date. Long text, HTML, link, and numbers are not checked.
 */
export function invalidChangedDateFieldNames(
  fields: readonly Pick<OverlayField, "name" | "kind" | "value">[],
  edits: readonly Pick<OverlayFieldEdit, "contentId" | "name" | "value">[],
  baseline: ReadonlyMap<string, string>,
): string[] {
  const latest = new Map<string, (typeof edits)[number]>();
  for (const edit of edits) {
    latest.set(edit.name, edit);
  }
  const names: string[] = [];
  for (const field of fields) {
    if (field.kind !== "date") {
      continue;
    }
    const edit = latest.get(field.name);
    if (!edit) {
      continue;
    }
    const previous = baseline.has(overlayEditKey(edit))
      ? (baseline.get(overlayEditKey(edit)) ?? "")
      : field.value;
    if (edit.value.trim() === previous.trim()) {
      continue;
    }
    if (calendarDateText(edit.value) == null) {
      names.push(field.name);
    }
  }
  return names;
}

/** Mark overlay controls invalid when a required-text save was refused. */
export function markAssemblyFieldErrors(
  root: ParentNode | null,
  errors: Readonly<Record<string, string>>,
): void {
  if (root == null) {
    return;
  }
  root.querySelectorAll("[data-assembly-field]").forEach((el) => {
    const tag = el.tagName;
    if (tag !== "INPUT" && tag !== "TEXTAREA") {
      el.removeAttribute("aria-invalid");
      return;
    }
    const name = el.getAttribute("data-assembly-field")?.trim() ?? "";
    if (name && errors[name]) {
      el.setAttribute("aria-invalid", "true");
    } else {
      el.removeAttribute("aria-invalid");
    }
  });
}

const LINK_INPUT_ATTR = "data-assembly-link-input";
const DATE_INPUT_ATTR = "data-assembly-date-input";

export interface OverlayField {
  name: string;
  value: string;
  label: string;
  kind: OverlayFieldKind;
  readOnly: boolean;
  /** Content-type required flag. Single-line text and whole numbers are enforced on save. */
  required: boolean;
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
  /** Present for link fields so the item field save stores a link. */
  dataType?: string;
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
): kind is "text" | "longtext" {
  return kind === "text" || kind === "longtext";
}

export function isOverlayFieldKind(kind: string): kind is OverlayFieldKind {
  return (
    isScalarOverlayKind(kind) ||
    kind === "html" ||
    kind === "link" ||
    kind === "number" ||
    kind === "date"
  );
}

/**
 * Text, long-text, HTML, link, whole-number, and calendar-date rows from itemmanagement.
 * File, image, keyword, community, table, datetime, and float stay on the Content Editor.
 * Read-only rows are omitted so the overlay cannot write them.
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
    if (!isOverlayFieldKind(kind) || schema?.readOnly === true) {
      continue;
    }
    if (kind === "number" && (schema?.dataType ?? "").trim().toLowerCase() === "float") {
      continue;
    }
    const rawValue = field.value ?? "";
    out.push({
      name: field.name,
      value: kind === "date" ? toWidgetValue("date", rawValue) : rawValue,
      label: schema?.label || field.name,
      kind,
      readOnly: false,
      required: schema?.required === true,
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
    if (hits.some((h) => h.name === field.name && h.contentId === owner)) {
      continue;
    }
    if (field.kind === "html" && value.includes("<")) {
      const matches: Element[] = [];
      candidates.forEach((el) => {
        if (claimed.has(el) || SKIP_VALUE_TAGS.has(el.tagName)) {
          return;
        }
        if ((el.innerHTML ?? "").trim() === value) {
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
      continue;
    }
    if (field.kind === "link") {
      if (!value) {
        continue;
      }
      const matches: Element[] = [];
      candidates.forEach((el) => {
        if (claimed.has(el) || SKIP_VALUE_TAGS.has(el.tagName)) {
          return;
        }
        if (!linkValueMatches(el, value)) {
          return;
        }
        matches.push(el);
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
      continue;
    }
    if (value.length < 2) {
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

function linkValueMatches(el: Element, value: string): boolean {
  if (!value) {
    return false;
  }
  if (el.tagName === "A" && (el.getAttribute("href") ?? "").trim() === value) {
    return true;
  }
  if (el.childElementCount > 0) {
    return false;
  }
  return (el.textContent ?? "").trim() === value;
}

function mountLinkInput(
  host: HTMLElement,
  hit: OverlayFieldHit,
  field: OverlayField,
): void {
  host.contentEditable = "false";
  host.removeAttribute("contenteditable");
  const input = host.ownerDocument.createElement("input");
  input.type = "text";
  input.value = field.value;
  input.setAttribute("data-assembly-field", hit.name);
  input.setAttribute("data-assembly-content-id", hit.contentId);
  input.setAttribute("data-assembly-value", ASSEMBLY_VALUE_LINK);
  input.setAttribute("data-testid", `assembly-inline-field-${hit.name}`);
  input.setAttribute(LINK_INPUT_ATTR, hit.name);
  input.setAttribute("spellcheck", "false");
  input.setAttribute("aria-label", field.label || hit.name);
  input.autocomplete = "off";
  input.setAttribute(
    "style",
    "display:inline-block;min-width:12rem;margin-left:4px;color:#0f172a;background:#fff;border:1px solid #64748b;font:inherit;",
  );
  // An input inside an anchor is invalid and would navigate on click.
  if (host.tagName === "A") {
    host.insertAdjacentElement("afterend", input);
  } else {
    host.appendChild(input);
  }
}

function mountDateInput(
  host: HTMLElement,
  hit: OverlayFieldHit,
  field: OverlayField,
): void {
  host.contentEditable = "false";
  host.removeAttribute("contenteditable");
  const input = host.ownerDocument.createElement("input");
  input.type = "date";
  input.value = toWidgetValue("date", field.value);
  input.setAttribute("data-assembly-field", hit.name);
  input.setAttribute("data-assembly-content-id", hit.contentId);
  input.setAttribute("data-assembly-value", ASSEMBLY_VALUE_DATE);
  input.setAttribute("data-testid", `assembly-inline-field-${hit.name}`);
  input.setAttribute(DATE_INPUT_ATTR, hit.name);
  input.setAttribute("aria-label", field.label || hit.name);
  input.setAttribute(
    "style",
    "display:inline-block;margin-left:4px;color:#0f172a;background:#fff;border:1px solid #64748b;font:inherit;",
  );
  if (host.tagName === "A") {
    host.insertAdjacentElement("afterend", input);
  } else {
    host.textContent = "";
    host.appendChild(input);
  }
}

/** Drop markers from a previous paint so a field that is no longer editable cannot be saved. */
export function clearFieldOverlay(root: ParentNode): void {
  root.querySelectorAll(`input[${LINK_INPUT_ATTR}]`).forEach((el) => {
    el.remove();
  });
  root.querySelectorAll(`input[${DATE_INPUT_ATTR}]`).forEach((el) => {
    el.remove();
  });
  root.querySelectorAll("[data-assembly-field]").forEach((el) => {
    const html = el as HTMLElement;
    html.contentEditable = "false";
    html.removeAttribute("contenteditable");
    if (html.getAttribute("data-assembly-value") === ASSEMBLY_VALUE_LONGTEXT) {
      html.style.whiteSpace = "";
    }
    html.removeAttribute("data-assembly-field");
    html.removeAttribute("data-assembly-content-id");
    html.removeAttribute("data-assembly-value");
    html.removeAttribute("data-assembly-required");
    html.removeAttribute("spellcheck");
    const testId = html.getAttribute("data-testid") ?? "";
    if (testId.startsWith("assembly-inline-field-")) {
      html.removeAttribute("data-testid");
    }
  });
}

export function applyFieldOverlay(
  root: ParentNode,
  fields: OverlayField[],
  ownerId: string,
): OverlayFieldHit[] {
  stripLeftoverAaChrome(root);
  clearFieldOverlay(root);
  const hits = mapAssembledFieldElements(root, fields, ownerId);
  for (const hit of hits) {
    const html = hit.element as HTMLElement;
    const field = fields.find((row) => row.name === hit.name);
    if (field?.kind === "link") {
      mountLinkInput(html, hit, field);
      continue;
    }
    if (field?.kind === "date") {
      mountDateInput(html, hit, field);
      continue;
    }
    html.contentEditable = "true";
    html.setAttribute("data-assembly-field", hit.name);
    html.setAttribute("data-assembly-content-id", hit.contentId);
    html.setAttribute("data-testid", `assembly-inline-field-${hit.name}`);
    html.setAttribute("spellcheck", "false");
    if (field?.kind === "html") {
      html.setAttribute("data-assembly-value", ASSEMBLY_VALUE_HTML);
    } else if (field?.kind === "text") {
      html.setAttribute("data-assembly-value", ASSEMBLY_VALUE_TEXT);
      if (field.required) {
        // A heading or other assembled node is contenteditable. aria-required
        // is not allowed on that role; the overlay input carries it instead.
        html.setAttribute("data-assembly-required", "true");
      }
      bindSingleLineGuard(html);
    } else if (field?.kind === "longtext") {
      html.setAttribute("data-assembly-value", ASSEMBLY_VALUE_LONGTEXT);
      html.style.whiteSpace = "pre-wrap";
    } else if (field?.kind === "number") {
      html.setAttribute("data-assembly-value", ASSEMBLY_VALUE_NUMBER);
      if (field.required) {
        // A heading or other assembled node is contenteditable. aria-required
        // is not allowed on that role; the overlay input carries it instead.
        html.setAttribute("data-assembly-required", "true");
      }
      bindSingleLineGuard(html);
    }
  }
  return hits;
}

const TEXT_GUARD_ATTR = "data-assembly-text-guard";

/**
 * Enter in an in-place single-line field must not insert a line break.
 * The flag stays on the node so a later paint does not stack listeners.
 */
function bindSingleLineGuard(el: HTMLElement): void {
  if (el.getAttribute(TEXT_GUARD_ATTR) === "true") {
    return;
  }
  el.setAttribute(TEXT_GUARD_ATTR, "true");
  el.addEventListener("keydown", (event) => {
    if (event.key === "Enter") {
      event.preventDefault();
    }
  });
}

/**
 * Preview nodes live in the iframe realm, so {@code instanceof HTMLInputElement}
 * against the assembly host window is false. Tag name is realm-safe.
 */
function isFormValueElement(el: Element): boolean {
  return el.tagName === "INPUT" || el.tagName === "TEXTAREA";
}

function formValue(el: Element): string {
  return (el as HTMLInputElement).value;
}

function readNodeValue(el: Element): string {
  const valueKind = el.getAttribute("data-assembly-value");
  if (valueKind === ASSEMBLY_VALUE_LINK) {
    if (isFormValueElement(el)) {
      return formValue(el).trim();
    }
    return (el.textContent ?? "").trim();
  }
  if (isFormValueElement(el)) {
    const raw = formValue(el);
    if (valueKind === ASSEMBLY_VALUE_TEXT) {
      return singleLineText(raw);
    }
    if (valueKind === ASSEMBLY_VALUE_LONGTEXT) {
      return longTextValue(raw);
    }
    if (valueKind === ASSEMBLY_VALUE_NUMBER) {
      return raw.trim();
    }
    if (valueKind === ASSEMBLY_VALUE_DATE) {
      return raw.trim();
    }
    return raw;
  }
  if (valueKind === ASSEMBLY_VALUE_HTML) {
    return (el as HTMLElement).innerHTML.trim();
  }
  if (valueKind === ASSEMBLY_VALUE_LONGTEXT) {
    return longTextFromElement(el);
  }
  if (valueKind === ASSEMBLY_VALUE_NUMBER) {
    return (el.textContent ?? "").trim();
  }
  if (valueKind === ASSEMBLY_VALUE_DATE) {
    return (el.textContent ?? "").trim();
  }
  const text = (el.textContent ?? "").trim();
  return valueKind === ASSEMBLY_VALUE_TEXT ? singleLineText(text) : text;
}

function writeNodeValue(el: Element, value: string): void {
  const valueKind = el.getAttribute("data-assembly-value");
  if (isFormValueElement(el)) {
    (el as HTMLInputElement).value = value;
    return;
  }
  if (valueKind === ASSEMBLY_VALUE_HTML) {
    (el as HTMLElement).innerHTML = value;
    return;
  }
  if (valueKind === ASSEMBLY_VALUE_LONGTEXT) {
    (el as HTMLElement).style.whiteSpace = "pre-wrap";
  }
  el.textContent = value;
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
    const valueKind = el.getAttribute("data-assembly-value");
    edits.push({
      contentId:
        el.getAttribute("data-assembly-content-id")?.trim() || fallbackOwnerId,
      name,
      value: readNodeValue(el),
      ...(valueKind === ASSEMBLY_VALUE_LINK ? { dataType: "link" } : {}),
      ...(valueKind === ASSEMBLY_VALUE_NUMBER ? { dataType: "integer" } : {}),
      ...(valueKind === ASSEMBLY_VALUE_DATE ? { dataType: "date" } : {}),
    });
  });
  return edits;
}

/** Put the last saved field values back into overlay nodes after a failed save. */
export function restoreOverlayValues(
  root: ParentNode,
  fields: OverlayField[],
): void {
  const byName = new Map(fields.map((field) => [field.name, field]));
  root.querySelectorAll("[data-assembly-field]").forEach((el) => {
    const name = el.getAttribute("data-assembly-field")?.trim() ?? "";
    const field = byName.get(name);
    if (!field) {
      return;
    }
    writeNodeValue(el, field.value);
  });
}

export function mergeOverlayEdits(
  payload: ItemEditorFields,
  edits: OverlayFieldEdit[],
): ItemEditorFields {
  const byName = new Map<string, OverlayFieldEdit>();
  for (const edit of edits) {
    if (edit.contentId && edit.contentId !== String(payload.contentId)) {
      continue;
    }
    byName.set(edit.name, edit);
  }
  return {
    ...payload,
    fields: payload.fields.map((field: ItemEditorField) => {
      const edit = byName.get(field.name);
      if (!edit) {
        return field;
      }
      const next: ItemEditorField = { ...field, value: edit.value };
      if (edit.dataType) {
        next.dataType = edit.dataType;
      }
      return next;
    }),
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
  checkout?: (itemId: string) => Promise<unknown>;
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
