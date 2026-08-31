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

import { getWithHeaders, postText } from "../client";
import { PATHS } from "../paths";
import { unwrapContentTypeDetail } from "./contentTypesApi";
import type { ContentTypeDetail } from "./types";

/** Result of {@code GET /services/contenttypes/{idOrName}/export}. */
export type ContentTypeExportResult = {
  xml: string;
  filename: string;
};

/**
 * Basename matching rest {@code ContentTypesResource.exportFilename}:
 * strip control chars, quotes, and Windows-invalid filename characters.
 * Not a filesystem path.
 */
export function contentTypeExportFilename(contentTypeName: string): string {
  const raw = contentTypeName == null ? "" : String(contentTypeName).trim();
  let base = "";
  for (let i = 0; i < raw.length; i++) {
    const c = raw.charAt(i);
    const code = raw.charCodeAt(i);
    if (
      code <= 31 ||
      code === 127 ||
      c === '"' ||
      c === "\\" ||
      c === "/" ||
      c === ":" ||
      c === "*" ||
      c === "?" ||
      c === "<" ||
      c === ">" ||
      c === "|"
    ) {
      base += "_";
    } else {
      base += c;
    }
  }
  base = base.trim();
  if (!base) {
    base = "contenttype";
  }
  if (!base.toLowerCase().endsWith(".xml")) {
    base += ".xml";
  }
  return base;
}

/**
 * Filename from {@code Content-Disposition}. Prefers RFC 5987 {@code filename*}.
 */
export function parseContentDispositionFilename(header: string): string {
  const raw = header == null ? "" : String(header);
  if (!raw.trim()) {
    return "";
  }
  const star = /filename\*\s*=\s*(?:UTF-8'')?([^;]+)/i.exec(raw);
  if (star) {
    try {
      return decodeURIComponent(star[1].trim().replace(/^"+|"+$/g, ""));
    } catch {
      return star[1].trim().replace(/^"+|"+$/g, "");
    }
  }
  const quoted = /filename\s*=\s*"([^"]+)"/i.exec(raw);
  if (quoted) {
    return quoted[1];
  }
  const unquoted = /filename\s*=\s*([^;]+)/i.exec(raw);
  return unquoted ? unquoted[1].trim() : "";
}

/**
 * Double-quoted {@code PSXItemDefSummary@name}. Regex only — do not feed operator
 * XML into {@code DOMParser.parseFromString} (CodeQL {@code js/xss-through-dom}).
 */
const ITEM_DEF_SUMMARY_NAME =
  /(<PSXItemDefSummary\b[^>]*\bname\s*=\s*")([^"]*)(")/i;

function escapeXmlAttribute(value: string): string {
  return value
    .replace(/&/g, "&amp;")
    .replace(/"/g, "&quot;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");
}

/**
 * Internal name from Workbench / REST {@code ItemDefData} XML
 * ({@code PSXItemDefSummary@name}).
 */
export function contentTypeNameFromDesignXml(xml: string): string {
  const trimmed = xml == null ? "" : String(xml).trim();
  if (!trimmed || !/<ItemDefData\b/i.test(trimmed)) {
    return "";
  }
  const m = ITEM_DEF_SUMMARY_NAME.exec(trimmed);
  return m ? m[2] : "";
}

/**
 * Rewrite {@code PSXItemDefSummary@name} so create-only import uses a unique name.
 * Does not change type id / app name (server remaps those).
 */
export function rewriteContentTypeDesignXmlName(xml: string, newName: string): string {
  const trimmed = xml == null ? "" : String(xml).trim();
  const name = newName == null ? "" : String(newName).trim();
  if (!trimmed) {
    throw new Error("content-type design XML is required");
  }
  if (!name) {
    throw new Error("name is required");
  }
  if (!/<ItemDefData\b/i.test(trimmed) || !ITEM_DEF_SUMMARY_NAME.test(trimmed)) {
    if (!/<ItemDefData\b/i.test(trimmed)) {
      throw new Error("invalid content-type design XML");
    }
    throw new Error("content-type design XML is missing name");
  }
  return trimmed.replace(
    ITEM_DEF_SUMMARY_NAME,
    (_all, open: string, _old: string, close: string) =>
      `${open}${escapeXmlAttribute(name)}${close}`,
  );
}

/**
 * Client-side name rules matching rest create/import (no spaces/wildcards).
 * Returns an error key fragment, or {@code null} when valid.
 */
export function invalidContentTypeImportName(name: string): string | null {
  const trimmed = name == null ? "" : String(name).trim();
  if (!trimmed) {
    return "name is required";
  }
  if (/\s/.test(trimmed)) {
    return "Content type name must not contain spaces";
  }
  if (trimmed.includes("*") || trimmed.includes("%")) {
    return "Content type name must not contain wildcards";
  }
  return null;
}

/**
 * GET /services/contenttypes/{idOrName}/export — Admin CD-14 download.
 * Read-only; does not acquire or steal a design lock. HTTP 404 unknown.
 */
export async function exportContentType(
  idOrName: string,
): Promise<ContentTypeExportResult> {
  const key = encodeURIComponent(idOrName);
  const { data, headers } = await getWithHeaders<string>(
    `${PATHS.CONTENT_TYPES}/${key}/export`,
    { Accept: "application/xml, text/xml, */*" },
  );
  const xml = typeof data === "string" ? data : String(data ?? "");
  const fromHeader = parseContentDispositionFilename(
    headers.get("Content-Disposition") || headers.get("content-disposition") || "",
  );
  const filename =
    fromHeader ||
    contentTypeExportFilename(contentTypeNameFromDesignXml(xml) || idOrName);
  return { xml, filename };
}

/**
 * POST /services/contenttypes/import — Admin CD-14 create-only XML import.
 * HTTP 400 invalid XML; HTTP 409 duplicate name (no overwrite).
 */
export async function importContentType(xml: string): Promise<ContentTypeDetail> {
  const payload = await postText<unknown>(
    `${PATHS.CONTENT_TYPES}/import`,
    xml ?? "",
    {
      "Content-Type": "application/xml",
      Accept: "application/json, */*",
    },
  );
  return unwrapContentTypeDetail(payload);
}

/**
 * Trigger a browser download of exported design XML. Not a filesystem write.
 */
export function downloadXmlFile(xml: string, filename: string): void {
  if (typeof document === "undefined") {
    return;
  }
  const name = contentTypeExportFilename(filename || "contenttype.xml");
  const blob = new Blob([xml], { type: "application/xml" });
  let href = `data:application/xml;charset=utf-8,${encodeURIComponent(xml)}`;
  let objectUrl: string | null = null;
  if (typeof URL !== "undefined" && typeof URL.createObjectURL === "function") {
    objectUrl = URL.createObjectURL(blob);
    href = objectUrl;
  }
  const a = document.createElement("a");
  a.href = href;
  a.download = name;
  a.rel = "noopener";
  a.style.display = "none";
  document.body.appendChild(a);
  a.click();
  a.remove();
  if (objectUrl && typeof URL.revokeObjectURL === "function") {
    URL.revokeObjectURL(objectUrl);
  }
}
