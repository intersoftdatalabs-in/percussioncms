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
import { unwrapTemplateDetail } from "./assemblyApi";
import type { TemplateDetail } from "./types";

/** Result of {@code GET /services/templates/{idOrName}/export}. */
export type TemplateExportResult = {
  xml: string;
  filename: string;
};

/**
 * Basename matching rest {@code TemplatesResource.exportFilename}:
 * strip control chars, quotes, and path separators. Not a filesystem path.
 */
export function templateExportFilename(templateName: string): string {
  const raw = templateName == null ? "" : String(templateName).trim();
  let base = "";
  for (let i = 0; i < raw.length; i++) {
    const c = raw.charAt(i);
    const code = raw.charCodeAt(i);
    if (code <= 31 || code === 127 || c === '"' || c === "\\" || c === "/" || c === ":") {
      base += "_";
    } else {
      base += c;
    }
  }
  base = base.trim();
  if (!base) {
    base = "template";
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
 * First {@code <name>} element in Workbench {@code assembly-template} XML.
 * Regex only — do not feed operator XML into {@code DOMParser.parseFromString}
 * (CodeQL {@code js/xss-through-dom}).
 */
const TEMPLATE_NAME_ELEMENT = /(<name>)([^<]*)(<\/name>)/i;

function escapeXmlText(value: string): string {
  return value.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
}

/**
 * Internal name from Workbench / REST {@code assembly-template} XML ({@code <name>}).
 */
export function templateNameFromDesignXml(xml: string): string {
  const trimmed = xml == null ? "" : String(xml).trim();
  if (!trimmed || !/<assembly-template\b/i.test(trimmed)) {
    return "";
  }
  const m = TEMPLATE_NAME_ELEMENT.exec(trimmed);
  return m ? m[2] : "";
}

/**
 * Drop exported identity that collides on create-only import (binding
 * {@code <id>} PK on H2). Server assigns new ids. Regex only — do not parse
 * operator XML with {@code DOMParser}.
 */
export function stripImportedTemplateIdentity(xml: string): string {
  const trimmed = xml == null ? "" : String(xml);
  return trimmed.replace(/<id>-?\d+<\/id>/gi, "");
}

/**
 * Rewrite the first {@code <name>} so create-only import uses a unique name.
 * Does not change GUID (server keeps the newly created object id).
 */
export function rewriteTemplateDesignXmlName(xml: string, newName: string): string {
  const trimmed = xml == null ? "" : String(xml).trim();
  const name = newName == null ? "" : String(newName).trim();
  if (!trimmed) {
    throw new Error("assembly-template design XML is required");
  }
  if (!name) {
    throw new Error("name is required");
  }
  if (!/<assembly-template\b/i.test(trimmed)) {
    throw new Error("invalid assembly-template design XML");
  }
  TEMPLATE_NAME_ELEMENT.lastIndex = 0;
  if (!TEMPLATE_NAME_ELEMENT.test(trimmed)) {
    throw new Error("assembly-template design XML is missing name");
  }
  TEMPLATE_NAME_ELEMENT.lastIndex = 0;
  const rewritten = trimmed.replace(
    TEMPLATE_NAME_ELEMENT,
    (_all, open: string, _old: string, close: string) =>
      `${open}${escapeXmlText(name)}${close}`,
  );
  return stripImportedTemplateIdentity(rewritten);
}

/**
 * Client-side name rules matching rest {@code TemplateAdaptor.validateCreateName}.
 * Returns an error key fragment, or {@code null} when valid.
 */
export function invalidTemplateImportName(name: string): string | null {
  const trimmed = name == null ? "" : String(name).trim();
  if (!trimmed) {
    return "name is required";
  }
  if (/\s/.test(trimmed)) {
    return "name cannot contain spaces";
  }
  if (!/^[A-Za-z][A-Za-z0-9._-]*$/.test(trimmed)) {
    return "name must start with a letter and contain only letters, digits, '.', '_' or '-'";
  }
  return null;
}

/** Reject non-XML payloads so operators never download `[object Object]`. */
export function asTemplateExportXml(data: unknown): string {
  if (typeof data !== "string") {
    throw new Error("template export did not return XML");
  }
  if (!data.trim().startsWith("<")) {
    throw new Error("template export did not return XML");
  }
  return data;
}

/**
 * GET /services/templates/{idOrName}/export — Admin AS-08 download.
 * Read-only; does not acquire or steal a design lock. HTTP 404 unknown; 403 non-Admin.
 */
export async function exportTemplate(idOrName: string): Promise<TemplateExportResult> {
  const key = encodeURIComponent(idOrName);
  const { data, headers } = await getWithHeaders<string>(`${PATHS.TEMPLATES}/${key}/export`, {
    Accept: "application/xml, text/xml, */*",
  });
  const xml = asTemplateExportXml(data);
  const fromHeader = parseContentDispositionFilename(
    headers.get("Content-Disposition") || headers.get("content-disposition") || "",
  );
  const filename = fromHeader || templateExportFilename(templateNameFromDesignXml(xml) || idOrName);
  return { xml, filename };
}

/**
 * POST /services/templates/import — Admin AS-08 create-only XML import.
 * HTTP 400 invalid XML; HTTP 409 duplicate name (no overwrite); HTTP 403 non-Admin.
 */
export async function importTemplate(xml: string): Promise<TemplateDetail> {
  const payload = await postText<unknown>(`${PATHS.TEMPLATES}/import`, xml ?? "", {
    "Content-Type": "application/xml",
    Accept: "application/json, */*",
  });
  return unwrapTemplateDetail(payload);
}

/**
 * Trigger a browser download of exported design XML. Not a filesystem write.
 */
export function downloadXmlFile(xml: string, filename: string): void {
  if (typeof document === "undefined") {
    return;
  }
  const name = templateExportFilename(filename || "template.xml");
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
    const toRevoke = objectUrl;
    globalThis.setTimeout(() => {
      URL.revokeObjectURL(toRevoke);
    }, 1000);
  }
}
