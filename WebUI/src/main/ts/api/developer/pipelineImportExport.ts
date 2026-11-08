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
import { parseContentDispositionFilename } from "./contentTypeImportExport";
import { unwrapApplicationDetail } from "./pipelinesApi";
import type { ApplicationDetail } from "./types";

/** Result of {@code GET /services/pipelines/{idOrName}/export}. */
export type PipelineApplicationExportResult = {
  xml: string;
  filename: string;
};

/**
 * Basename matching rest {@code PipelinesResource.exportFilename}:
 * strip control chars, quotes, and Windows-invalid filename characters.
 * Not a filesystem path.
 */
export function pipelineApplicationExportFilename(appName: string): string {
  const raw = appName == null ? "" : String(appName).trim();
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
    base = "pipeline";
  }
  if (!base.toLowerCase().endsWith(".xml")) {
    base += ".xml";
  }
  return base;
}

/**
 * Application name from classic {@code PSXApplication} design XML
 * (first {@code <name>} element after the application root).
 * Regex only — do not feed operator XML into {@code DOMParser.parseFromString}
 * (CodeQL {@code js/xss-through-dom}).
 */
const APP_ROOT_AND_NAME = /(<PSXApplication\b[^>]*>[\s\S]*?<name>)([^<]*)(<\/name>)/i;

function escapeXmlText(value: string): string {
  return value.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
}

/** Internal application name from export/import {@code PSXApplication} XML. */
export function pipelineApplicationNameFromDesignXml(xml: string): string {
  const trimmed = xml == null ? "" : String(xml).trim();
  if (!trimmed || !/<PSXApplication\b/i.test(trimmed)) {
    return "";
  }
  const m = APP_ROOT_AND_NAME.exec(trimmed);
  return m ? m[2].trim() : "";
}

/**
 * Rewrite the application {@code <name>} so create-only import uses a unique name.
 * When the {@code <requestRoot>} still carries the old (default-derived) name it is
 * rewritten too; a custom request root is left untouched. Does not change the
 * application id (the server allocates a fresh one on create).
 */
export function rewritePipelineApplicationXmlName(xml: string, newName: string): string {
  const trimmed = xml == null ? "" : String(xml).trim();
  const name = newName == null ? "" : String(newName).trim();
  if (!trimmed) {
    throw new Error("pipeline application design XML is required");
  }
  if (!name) {
    throw new Error("name is required");
  }
  if (!/<PSXApplication\b/i.test(trimmed)) {
    throw new Error("invalid pipeline application design XML");
  }
  if (!APP_ROOT_AND_NAME.test(trimmed)) {
    throw new Error("pipeline application design XML is missing name");
  }
  const oldName = pipelineApplicationNameFromDesignXml(trimmed);
  const renamed = trimmed.replace(
    APP_ROOT_AND_NAME,
    (_all: string, open: string, _old: string, close: string) =>
      `${open}${escapeXmlText(name)}${close}`,
  );
  if (oldName) {
    const root = /(<requestRoot>)(\s*[^<]*?\s*)(<\/requestRoot>)/i.exec(renamed);
    if (root && root[2].trim() === oldName) {
      return renamed.replace(
        /(<requestRoot>)(\s*[^<]*?\s*)(<\/requestRoot>)/i,
        (_all: string, open: string, _old: string, close: string) =>
          `${open}${escapeXmlText(name)}${close}`,
      );
    }
  }
  return renamed;
}

/**
 * Client-side name rules matching rest import (no separators, traversal, or
 * Windows-invalid filename characters). Returns an error message, or
 * {@code null} when valid.
 */
export function invalidPipelineApplicationImportName(name: string): string | null {
  const trimmed = name == null ? "" : String(name).trim();
  if (!trimmed) {
    return "name is required";
  }
  if (
    trimmed.includes("..") ||
    trimmed.includes("/") ||
    trimmed.includes("\\") ||
    trimmed.includes("\0")
  ) {
    return "Application name must not contain path separators";
  }
  if (/["':*?<>|]/.test(trimmed)) {
    return "Application name contains an invalid character for portable installs";
  }
  for (let i = 0; i < trimmed.length; i++) {
    const code = trimmed.charCodeAt(i);
    if (code <= 31 || code === 127) {
      return "Application name contains an invalid character for portable installs";
    }
  }
  return null;
}

/** Reject non-XML payloads so operators never download `[object Object]`. */
export function asPipelineApplicationExportXml(data: unknown): string {
  if (typeof data !== "string") {
    throw new Error("pipeline application export did not return XML");
  }
  if (!data.trim().startsWith("<")) {
    throw new Error("pipeline application export did not return XML");
  }
  return data;
}

/**
 * GET /services/pipelines/{idOrName}/export — Admin classic XML download.
 * Read-only; does not acquire or steal a design lock. HTTP 404 unknown,
 * HTTP 400 hidden.
 */
export async function exportPipelineApplication(
  idOrName: string,
): Promise<PipelineApplicationExportResult> {
  const key = encodeURIComponent(idOrName);
  const { data, headers } = await getWithHeaders<string>(
    `${PATHS.PIPELINES}/${key}/export`,
    { Accept: "application/xml, text/xml, */*" },
  );
  const xml = asPipelineApplicationExportXml(data);
  const fromHeader = parseContentDispositionFilename(
    headers.get("Content-Disposition") || headers.get("content-disposition") || "",
  );
  const filename =
    fromHeader ||
    pipelineApplicationExportFilename(
      pipelineApplicationNameFromDesignXml(xml) || idOrName,
    );
  return { xml, filename };
}

/**
 * POST /services/pipelines/import — Admin create-only XML import.
 * HTTP 400 invalid XML; HTTP 409 duplicate name (no overwrite).
 */
export async function importPipelineApplication(
  xml: string,
): Promise<ApplicationDetail> {
  const payload = await postText<unknown>(
    `${PATHS.PIPELINES}/import`,
    xml ?? "",
    {
      "Content-Type": "application/xml",
      Accept: "application/json, */*",
    },
  );
  return unwrapApplicationDetail(payload);
}
