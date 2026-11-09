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
 * Paste-from-Word sanitation for the #5622 toolbar-parity slice (#5651).
 *
 * <p>Ports the {@code paste_word_valid_elements} contract from
 * {@code rx_default_config.json}
 * ({@code b,strong,i,em,h1,h2,u,p,ol,ul,li,a[href],table,tr,td,style}, plus
 * {@code paste_data_images}): pasted Microsoft Word/Office HTML is reduced
 * to content the editor schema persists, and everything Word-specific
 * (namespaced {@code o:}/{@code w:}/{@code m:}/{@code v:} markup,
 * {@code mso-*} classes and styles, conditional comments, {@code <style>} /
 * {@code <xml>} metadata blocks) is dropped. Word list paragraphs
 * ({@code MsoListParagraph}, the {@code mso-list} style) are rebuilt into
 * real {@code <ul>}/{@code <ol>} items — like the TinyMCE paste filter —
 * with {@code mso-list} levels nested and ordered markers ({@code 1.},
 * {@code a)}) landing in {@code <ol>}.</p>
 *
 * <p>The sanitizer only rewrites HTML that smells like a Word/Office paste
 * (Office namespace, {@code mso-} markers, or {@code Mso*} classes).
 * Ordinary clipboard HTML — including copy/paste inside the editor itself —
 * passes through byte-identical so intra-editor fidelity never regresses.</p>
 *
 * <p>Known limit (same as the TinyMCE paste filter): table header
 * cells ({@code th}) are kept as {@code td}: the paste contract has no
 * {@code th}, and content is preserved.</p>
 */

/** Tags Word content may keep (element allowlist, local names). */
const KEPT_TAGS = new Set([
  "b",
  "strong",
  "i",
  "em",
  "h1",
  "h2",
  "u",
  "p",
  "ol",
  "ul",
  "li",
  "a",
  "table",
  "tr",
  "td",
  "th",
  "img",
  "br",
]);

/**
 * Elements dropped with their entire subtree (metadata, never content).
 * {@code style} is dropped rather than kept: the paste contract names the
 * element, but Word style blocks only feed classes the sanitizer strips, and
 * the editor schema drops them on parse either way.
 */
const DROPPED_SUBTREE_TAGS = new Set(["style", "xml", "script", "meta", "link"]);

/** Office/metadata namespace prefixes Word emits (local name kept or dropped). */
const OFFICE_PREFIXES = new Set(["o", "w", "m", "v", "st1", "st2"]);

function localName(tag: string): string {
  const lower = tag.toLowerCase();
  const colon = lower.lastIndexOf(":");
  return colon >= 0 ? lower.slice(colon + 1) : lower;
}

function namespacePrefix(tag: string): string | null {
  const lower = tag.toLowerCase();
  const colon = lower.indexOf(":");
  return colon > 0 ? lower.slice(0, colon) : null;
}

/** True when the pasted HTML carries Word/Office markers. */
export function looksLikeWordHtml(html: string): boolean {
  if (!html) {
    return false;
  }
  return (
    /schemas-microsoft-com:office/i.test(html) ||
    /<\s*o:p[\s>]/i.test(html) ||
    /\bmso-[a-z-]+\s*:/i.test(html) ||
    /class\s*=\s*["'][^"']*\bMso\w*/.test(html)
  );
}

/**
 * Reduce a Word/Office HTML paste to the paste-contract allowlist.
 *
 * <p>Non-Word HTML is returned unchanged. Attribute policy: {@code a} keeps
 * {@code href} only, {@code img} keeps {@code src}/{@code alt} only (ported
 * {@code paste_data_images}, data: URLs included), {@code td}/{@code th}
 * keep {@code colspan}/{@code rowspan} only, and every element keeps only a
 * {@code text-align} declaration from inline styles (the one alignment form
 * the editor schema persists). All other attributes — classes, ids, lang,
 * event handlers, and every {@code mso-*} style — are dropped.</p>
 *
 * @param html raw pasted HTML (may be a full Word document).
 * @returns sanitized HTML, or the input unchanged when it is not Word HTML.
 */
export function sanitizeWordPastedHtml(html: string): string {
  if (!html || !looksLikeWordHtml(html)) {
    return html;
  }
  const doc = new DOMParser().parseFromString(html, "text/html");
  const body = doc.body;
  if (!body) {
    return "";
  }
  rebuildWordLists(body);
  sanitizeNode(body);
  return body.innerHTML;
}

/**
 * Word list paragraph ({@code MsoListParagraph} / {@code mso-list} style)
 * seen as a body-level run, with its marker stripped and content kept.
 */
interface WordListItem {
  paragraph: HTMLElement;
  level: number;
  ordered: boolean;
  content: ChildNode[];
}

/** Bullet glyphs Word emits for unordered list paragraphs. */
const WORD_BULLETS = "·•◦○●■□▪▫►‣⁃–—*o§v";

/** True for a plain (unprefixed) {@code <p>} carrying Word list markers. */
function wordListItemInfo(p: HTMLElement): { level: number; ordered: boolean } | null {
  if (localName(p.tagName) !== "p" || namespacePrefix(p.tagName) !== null) {
    return null;
  }
  const cls = p.getAttribute("class") ?? "";
  const style = p.getAttribute("style") ?? "";
  const isListParagraph =
    /(^|\s)MsoListParagraph(\s|$)/i.test(cls) || /mso-list\s*:/i.test(style);
  if (!isListParagraph) {
    return null;
  }
  const levelMatch = /mso-list\s*:[^;]*?\blevel\s*0*([1-9]\d*)/i.exec(style);
  const level = levelMatch ? Math.min(Number(levelMatch[1]), 9) : 1;
  return { level, ordered: isOrderedWordListItem(p) };
}

/**
 * Ordered when the leading marker reads like {@code 1.} / {@code a)} /
 * {@code iv.}; anything else (Symbol bullets, {@code ·}, no marker) is an
 * unordered item, matching the TinyMCE paste filter default.
 */
function isOrderedWordListItem(p: HTMLElement): boolean {
  const marker = readWordListMarker(p);
  return marker !== null && marker.ordered;
}

/** Leading marker text of a list paragraph, without surrounding spacing. */
function readWordListMarker(
  p: HTMLElement,
): { ordered: boolean; node: ChildNode; element: boolean } | null {
  let node: ChildNode | null = p.firstChild;
  while (
    node !== null &&
    (node.nodeType === Node.COMMENT_NODE ||
      (node.nodeType === Node.TEXT_NODE && (node.textContent ?? "").trim() === ""))
  ) {
    node = node.nextSibling;
  }
  if (node === null) {
    return null;
  }
  if (node.nodeType === Node.ELEMENT_NODE) {
    const tag = localName((node as HTMLElement).tagName);
    if (tag !== "span" && tag !== "font") {
      return null;
    }
    return classifyWordMarker(node.textContent ?? "", node, true);
  }
  if (node.nodeType === Node.TEXT_NODE) {
    // A bare text marker ("1. Item", "· Item") classifies on its leading
    // token; anything else is content, not a marker.
    const leading = /^[\s\u00a0]*(\S+[.)])[\s\u00a0]+/.exec(node.textContent ?? "");
    if (!leading) {
      return null;
    }
    return classifyWordMarker(leading[1], node, false);
  }
  return null;
}

function classifyWordMarker(
  text: string,
  node: ChildNode,
  element: boolean,
): { ordered: boolean; node: ChildNode; element: boolean } | null {
  const compact = text.replace(/[\s\u00a0]/g, "");
  if (compact === "") {
    return null;
  }
  if (/^[0-9]+[.)]$|^[a-zA-Z][.)]$|^[ivxlcdmIVXLCDM]+[.)]$/.test(compact)) {
    return { ordered: true, node, element };
  }
  const bullets = new RegExp(`^[${escapeRegExp(WORD_BULLETS)}]+$`, "u");
  if (bullets.test(compact)) {
    return { ordered: false, node, element };
  }
  return null;
}

function escapeRegExp(text: string): string {
  return text.replace(/[.*+?^${}()|[\]\\-]/g, "\\$&");
}

/** Drop the leading marker so the item keeps only its text and inline markup. */
function stripWordListMarker(p: HTMLElement): void {
  const marker = readWordListMarker(p);
  if (marker === null) {
    return;
  }
  if (marker.element) {
    marker.node.remove();
    trimLeadingSpacing(p);
    return;
  }
  const text = marker.node.textContent ?? "";
  const match = /^([\s\u00a0]*\S+[.)][\s\u00a0]*)/.exec(text);
  marker.node.textContent = match ? text.slice(match[1].length) : text.trimStart();
}

function trimLeadingSpacing(p: HTMLElement): void {
  const first = p.firstChild;
  if (first !== null && first.nodeType === Node.TEXT_NODE) {
    first.textContent = (first.textContent ?? "").trimStart();
  }
}

/**
 * Rebuild body-level runs of Word list paragraphs into real
 * {@code <ul>}/{@code <ol>} lists before the allowlist pass (which keeps
 * {@code ul}/{@code ol}/{@code li} and sanitizes their content like the
 * TinyMCE paste filter). Non-list content splits runs; {@code mso-list}
 * levels nest; a type change at one level starts a sibling list.
 */
function rebuildWordLists(body: HTMLElement): void {
  let node: Element | null = body.firstElementChild;
  let run: HTMLElement[] = [];
  const flush = (): void => {
    if (run.length > 0) {
      replaceWordListRun(run);
      run = [];
    }
  };
  while (node !== null) {
    const next = node.nextElementSibling;
    const info =
      node instanceof HTMLElement ? wordListItemInfo(node) : null;
    if (node instanceof HTMLElement && info !== null) {
      run.push(node);
    } else {
      flush();
    }
    node = next;
  }
  flush();
}

function replaceWordListRun(run: HTMLElement[]): void {
  const doc = run[0].ownerDocument;
  const items: WordListItem[] = run.map((paragraph) => {
    const info = wordListItemInfo(paragraph) ?? { level: 1, ordered: false };
    stripWordListMarker(paragraph);
    const content = Array.from(paragraph.childNodes);
    return { paragraph, level: info.level, ordered: info.ordered, content };
  });
  const fragment = buildWordListLevel(doc, items, 0, items[0].level).fragment;
  run[0].before(fragment);
  for (const paragraph of run) {
    paragraph.remove();
  }
}

function buildWordListLevel(
  doc: Document,
  items: WordListItem[],
  start: number,
  level: number,
): { fragment: DocumentFragment; next: number } {
  const fragment = doc.createDocumentFragment();
  let current: HTMLElement | null = null;
  let currentOrdered: boolean | null = null;
  let lastItem: HTMLElement | null = null;
  let i = start;
  while (i < items.length && items[i].level >= level) {
    const item = items[i];
    if (item.level > level) {
      const nested = buildWordListLevel(doc, items, i, item.level);
      (lastItem ?? fragment).append(nested.fragment);
      i = nested.next;
      continue;
    }
    if (current === null || item.ordered !== currentOrdered) {
      current = doc.createElement(item.ordered ? "ol" : "ul");
      fragment.append(current);
      currentOrdered = item.ordered;
    }
    const listItem = doc.createElement("li");
    for (const child of item.content) {
      listItem.append(child);
    }
    current.append(listItem);
    lastItem = listItem;
    i += 1;
  }
  return { fragment, next: i };
}

function sanitizeNode(parent: ParentNode): void {
  for (const child of Array.from(parent.childNodes)) {
    // A restructured subtree rescans the parent: stop the stale snapshot.
    if (sanitizeChild(parent, child)) {
      return;
    }
  }
}

/**
 * Sanitize one child node. Returns true when the child restructured the
 * parent (unwrap) and the parent was rescanned, so the caller must stop.
 */
function sanitizeChild(parent: ParentNode, child: ChildNode): boolean {
  if (child.nodeType === Node.COMMENT_NODE) {
    // Word conditional comments (supportLists/supportFootnotes) are not content.
    child.remove();
    return false;
  }
  if (child.nodeType !== Node.ELEMENT_NODE) {
    return false;
  }
  const el = child as HTMLElement;
  const local = localName(el.tagName);
  if (DROPPED_SUBTREE_TAGS.has(local)) {
    el.remove();
    return false;
  }
  if (isOfficeElement(el.tagName, local)) {
    return sanitizeOfficeElement(parent, el, local);
  }
  if (!KEPT_TAGS.has(local)) {
    unwrapElement(el);
    // Unwrapped children still need sanitation: rescan the parent.
    sanitizeNode(parent);
    return true;
  }
  sanitizeElement(el, local);
  sanitizeNode(el);
  return false;
}

/**
 * Content-bearing office wrappers (o:p, smart-tag inline wrappers) keep
 * their text; anything else namespaced is metadata.
 */
function sanitizeOfficeElement(parent: ParentNode, el: HTMLElement, local: string): boolean {
  if (local === "p" || local === "a" || isInlineOfficeWrapper(local)) {
    unwrapElement(el);
    sanitizeNode(parent);
    return true;
  }
  el.remove();
  return false;
}

function isOfficeElement(tag: string, local: string): boolean {
  const prefix = namespacePrefix(tag);
  if (prefix !== null && OFFICE_PREFIXES.has(prefix)) {
    return true;
  }
  // Unprefixed but Word-only elements (Word's bookmark anchors, fields).
  return local === "o:p" || local === "w:sdt";
}

function isInlineOfficeWrapper(local: string): boolean {
  return (
    local === "span" ||
    local === "font" ||
    local === "personname" ||
    local === "placename" ||
    local === "country-region" ||
    local === "city" ||
    local === "place"
  );
}

/** Replace an element with its children (content kept, markup dropped). */
function unwrapElement(el: HTMLElement): void {
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
 * Canonicalize one kept element: map {@code th} to {@code td}, drop
 * disallowed attributes, keep only the {@code text-align} style.
 */
function sanitizeElement(el: HTMLElement, local: string): void {
  let target = el;
  if (local === "th") {
    target = renameElement(el, "td");
  }
  const kept: Array<[string, string]> = [];
  for (const attr of Array.from(target.attributes)) {
    const name = attr.name.toLowerCase();
    const value = keepAttribute(local, name, attr.value);
    if (value !== null) {
      kept.push([name, value]);
    }
  }
  for (const attr of Array.from(target.attributes)) {
    target.removeAttribute(attr.name);
  }
  for (const [name, value] of kept) {
    target.setAttribute(name, value);
  }
}

function renameElement(el: HTMLElement, tag: string): HTMLElement {
  const replacement = el.ownerDocument.createElement(tag);
  for (const attr of Array.from(el.attributes)) {
    replacement.setAttribute(attr.name, attr.value);
  }
  while (el.firstChild) {
    replacement.appendChild(el.firstChild);
  }
  el.replaceWith(replacement);
  return replacement;
}

/**
 * Attribute policy for kept elements. Returns the kept value or null.
 * {@code text-align} is the only inline style the editor schema persists,
 * so it is the only declaration salvaged from {@code style} attributes.
 */
function keepAttribute(
  local: string,
  name: string,
  value: string,
): string | null {
  if (name === "style") {
    return keepTextAlign(value);
  }
  if (name === "class" || name === "id" || name === "lang") {
    return null;
  }
  if (name.startsWith("on")) {
    return null;
  }
  if (local === "a") {
    return keepLinkAttribute(name, value);
  }
  if (local === "img") {
    return name === "src" || name === "alt" ? value : null;
  }
  if (local === "td" || local === "th") {
    return name === "colspan" || name === "rowspan" ? value : null;
  }
  if (local === "p") {
    return keepParagraphAttribute(name, value);
  }
  return null;
}

function keepLinkAttribute(name: string, value: string): string | null {
  if (name !== "href") {
    return null;
  }
  return isPastedLinkHrefSafe(value) ? value : null;
}

function keepParagraphAttribute(name: string, value: string): string | null {
  if (name !== "align") {
    return null;
  }
  const align = value.trim().toLowerCase();
  if (align === "left" || align === "center" || align === "right" || align === "justify") {
    return `text-align: ${align}`;
  }
  return null;
}

function keepTextAlign(style: string): string | null {
  for (const part of style.split(";")) {
    const declaration = part.trim();
    const match = /^text-align\s*:\s*(left|center|right|justify)\s*$/i.exec(declaration);
    if (match) {
      return `text-align: ${match[1].toLowerCase()}`;
    }
  }
  return null;
}

/**
 * Href policy shared by pasted links (and the toolbar link control):
 * block active schemes ({@code javascript:}, {@code data:},
 * {@code vbscript:}) and control characters; everything else — absolute,
 * site-relative, anchor, and mailto — passes through like the TinyMCE
 * link dialog.
 */
export function isPastedLinkHrefSafe(href: string): boolean {
  const trimmed = (href ?? "").trim();
  if (!trimmed) {
    return false;
  }
  // eslint-disable-next-line no-control-regex
  if (/[\u0000-\u001F\u007F]/.test(trimmed)) {
    return false;
  }
  return !/^\s*(javascript|data|vbscript)\s*:/i.test(trimmed);
}
