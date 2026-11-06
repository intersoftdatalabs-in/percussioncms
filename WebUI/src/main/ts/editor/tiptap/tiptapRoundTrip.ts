/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * HTML round-trip proof for the #5622 spike-validation slice (#5650).
 *
 * <p>TinyMCE 6.8.6 and TipTap/StarterKit serialize the same document to
 * slightly different HTML (tag aliases, legacy presentational attributes,
 * empty-paragraph forms). {@link #normalizeHtmlForRoundTrip} canonicalizes
 * those known-equivalent forms so the corpus test can assert
 * {@code normalize(roundTrip(tinymce)) === normalize(tinymce)}: no content
 * drift, only representation drift the spike report already accepted.</p>
 *
 * <p>Out of scope (later slices): styled {@code <span>} runs, font colors,
 * anchor-only {@code <a name>} links, and script/event markup (still blocked
 * by {@code htmlField.ts} checks, never normalized away).</p>
 */

import { Editor } from "@tiptap/core";
import { createSpikeEditorExtensions } from "./tiptapSetup";

/** Tag aliases TinyMCE emits that StarterKit serializes under one name. */
const TAG_ALIASES: Record<string, string> = {
  b: "strong",
  i: "em",
  strike: "s",
  del: "s",
};

/** Legacy presentational table attributes TipTap does not persist. */
const DROPPED_TABLE_ATTRS = new Set(["border", "cellpadding", "cellspacing"]);

/**
 * Parse an HTML fragment through the spike TipTap schema and re-serialize it,
 * exactly as the editor host does before {@code onChange} fires.
 *
 * @param html a stored HTML field value (may be empty).
 * @returns the TipTap-serialized HTML for the same document.
 */
export function roundTripHtmlFragment(html: string): string {
  const editor = new Editor({
    extensions: createSpikeEditorExtensions(),
    content: html ?? "",
  });
  try {
    return editor.getHTML();
  } finally {
    editor.destroy();
  }
}

/**
 * Canonicalize an HTML fragment for round-trip comparison.
 *
 * <p>Normalization rules (all representation-only, no content change):</p>
 * <ul>
 *   <li>tag aliases ({@code b}/{@code i}/{@code strike}/{@code del}) mapped
 *       to the StarterKit form ({@code strong}/{@code em}/{@code s});</li>
 *   <li>TinyMCE {@code data-mce-*} bookkeeping attributes dropped;</li>
 *   <li>legacy presentational table attributes dropped;</li>
 *   <li>{@code style} declarations sorted so alignment output compares
 *       independent of declaration order;</li>
 *   <li>{@code <pre><code>…} unwrapped to {@code <pre>…} (StarterKit always
 *       serializes code blocks with the inner {@code code} element);</li>
 *   <li>bare {@code <div>} wrappers and attribute-free {@code <span>} runs
 *       unwrapped (ProseMirror keeps only their text content);</li>
 *   <li>{@code thead}/{@code tbody}/{@code tfoot} unwrapped (StarterKit
 *       serializes rows directly under {@code table});</li>
 *   <li>single-{@code <p>} item/cell bodies unwrapped (StarterKit wraps
 *       {@code li}/{@code th}/{@code td} text in paragraphs);</li>
 *   <li>TipTap table bookkeeping dropped ({@code colgroup} elements,
 *       {@code table}/{@code col} layout styles, default
 *       {@code colspan}/{@code rowspan} of 1);</li>
 *   <li>paragraphs containing only a {@code <br>} treated as empty;</li>
 *   <li>remaining attributes sorted by name for stable string comparison.</li>
 * </ul>
 *
 * @param html any HTML fragment (TinyMCE- or TipTap-serialized).
 * @returns canonical HTML for string comparison.
 */
export function normalizeHtmlForRoundTrip(html: string): string {
  const doc = new DOMParser().parseFromString(
    `<div data-root="1">${html ?? ""}</div>`,
    "text/html",
  );
  const root = doc.querySelector("div[data-root]");
  if (!root) {
    return "";
  }
  normalizeNode(root);
  return root.innerHTML;
}

function normalizeNode(parent: ParentNode): void {
  for (const child of Array.from(parent.childNodes)) {
    if (child.nodeType !== Node.ELEMENT_NODE) {
      continue;
    }
    const el = child as HTMLElement;
    if (isHoistableWrapper(el)) {
      // Hoisted grandchildren still need normalization: rescan the parent.
      // Every helper below is idempotent, so rescanning is safe.
      hoistChildren(el);
      normalizeNode(parent);
      return;
    }
    // normalizeElement may swap the node (alias mapping); recurse into the
    // node actually in the tree so nested aliases are canonicalized too.
    normalizeNode(normalizeElement(el));
  }
}

/**
 * Bare {@code div}/{@code span} wrappers and {@code thead}/{@code tbody}/
 * {@code tfoot} sections carry no attributes the TipTap schema persists, so
 * their children are hoisted in their place before comparison.
 */
function isHoistableWrapper(el: HTMLElement): boolean {
  const tag = el.tagName.toLowerCase();
  if ((tag === "div" || tag === "span") && el.attributes.length === 0) {
    return true;
  }
  return tag === "thead" || tag === "tbody" || tag === "tfoot";
}

function hoistChildren(el: HTMLElement): void {
  const parent = el.parentNode;
  if (!parent) {
    return;
  }
  while (el.firstChild) {
    parent.insertBefore(el.firstChild, el);
  }
  el.remove();
}

/**
 * Canonicalize one element (never its subtree) and return the node actually
 * in the tree — alias mapping swaps the node, so callers must recurse into
 * the return value, not the input.
 */
function normalizeElement(el: HTMLElement): HTMLElement {
  const canonical = aliasElement(el);
  unwrapPreCode(canonical);
  unwrapItemParagraph(canonical);
  unwrapLeadingItemParagraph(canonical);
  if (canonical.tagName.toLowerCase() === "colgroup") {
    // TipTap column bookkeeping; widths are not stored content.
    canonical.remove();
    return canonical;
  }
  dropEmptyBreakParagraph(canonical);
  sortAttributes(canonical);
  return canonical;
}

/** Map TinyMCE tag aliases to the StarterKit form, swapping the node. */
function aliasElement(el: HTMLElement): HTMLElement {
  const alias = TAG_ALIASES[el.tagName.toLowerCase()];
  if (alias === undefined) {
    return el;
  }
  const replacement = el.ownerDocument.createElement(alias);
  for (const attr of Array.from(el.attributes)) {
    replacement.setAttribute(attr.name, attr.value);
  }
  while (el.firstChild) {
    replacement.appendChild(el.firstChild);
  }
  el.replaceWith(replacement);
  return replacement;
}

/** {@code <pre><code>…} → {@code <pre>…} (StarterKit always nests code). */
function unwrapPreCode(el: HTMLElement): void {
  if (el.tagName.toLowerCase() !== "pre" || el.children.length !== 1) {
    return;
  }
  const only = el.children[0];
  if (only.tagName.toLowerCase() !== "code") {
    return;
  }
  while (only.firstChild) {
    el.appendChild(only.firstChild);
  }
  only.remove();
}

/**
 * Single-{@code <p>} item/cell bodies ({@code li}/{@code th}/{@code td}) hold
 * no extra structure — StarterKit wraps them, TinyMCE keeps them bare.
 */
function unwrapItemParagraph(el: HTMLElement): void {
  const tag = el.tagName.toLowerCase();
  if ((tag !== "li" && tag !== "th" && tag !== "td") || el.children.length !== 1) {
    return;
  }
  const only = el.children[0];
  if (
    only.tagName.toLowerCase() !== "p" ||
    (only.textContent ?? "").trim().length === 0
  ) {
    return;
  }
  while (only.firstChild) {
    el.insertBefore(only.firstChild, only);
  }
  only.remove();
}

/**
 * StarterKit wraps leading item text in a paragraph before nested lists;
 * TinyMCE keeps it bare. Unwrap the leading paragraph only.
 */
function unwrapLeadingItemParagraph(el: HTMLElement): void {
  if (el.tagName.toLowerCase() !== "li" || el.children.length <= 1) {
    return;
  }
  const first = el.children[0];
  if (first.tagName.toLowerCase() !== "p") {
    return;
  }
  while (first.firstChild) {
    el.insertBefore(first.firstChild, first);
  }
  first.remove();
}

/** Paragraphs containing only a {@code <br>} are empty. */
function dropEmptyBreakParagraph(el: HTMLElement): void {
  if (el.tagName.toLowerCase() !== "p" || el.children.length !== 1) {
    return;
  }
  const only = el.children[0];
  if (
    only.tagName.toLowerCase() === "br" &&
    (el.textContent ?? "").trim() === ""
  ) {
    only.remove();
  }
}
/** Drop bookkeeping attributes and sort the rest for stable comparison. */
function sortAttributes(el: HTMLElement): void {
  const tag = el.tagName.toLowerCase();
  const kept: Array<[string, string]> = [];
  for (const attr of Array.from(el.attributes)) {
    const name = attr.name.toLowerCase();
    if (name.startsWith("data-mce-")) {
      continue;
    }
    if (tag === "table" && DROPPED_TABLE_ATTRS.has(name)) {
      continue;
    }
    if (
      (tag === "table" || tag === "col") &&
      name === "style"
    ) {
      // TipTap column-layout hints (min-width); stored tables carry none.
      continue;
    }
    if (
      (name === "colspan" || name === "rowspan") &&
      attr.value.trim() === "1"
    ) {
      // TipTap always serializes default spans; TinyMCE omits them.
      continue;
    }
    kept.push([
      attr.name,
      name === "style" ? normalizeStyle(attr.value) : attr.value,
    ]);
  }
  kept.sort((a, b) => (a[0] < b[0] ? -1 : a[0] > b[0] ? 1 : 0));
  for (const attr of Array.from(el.attributes)) {
    el.removeAttribute(attr.name);
  }
  for (const [name, value] of kept) {
    el.setAttribute(name, value);
  }
}

function normalizeStyle(style: string): string {
  return style
    .split(";")
    .map((part) => part.trim())
    .filter((part) => part.length > 0)
    .sort()
    .join("; ");
}
