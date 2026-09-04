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

import { get, put } from "../client";
import { PATHS } from "../paths";
import type { ApplicationFileSummary } from "./types";

/**
 * Catalog-level design gaps (REST-GAPS-02). Server omits these on list rows and may
 * still attach them on detail; SPA falls back when the wire array is missing/empty.
 */
export const APPLICATION_FILE_DESIGN_GAPS: string[] = [
  "Design locking / concurrent edit are not exposed on this Developer surface",
  "Binary files may not round-trip as UTF-8 text",
  "Create/delete folder and rename/move are not supported via this API",
  "Admin PUT may create a new file when the relative path does not yet exist under the application root",
  "Distinct from /serverconfigs (SY-02 fixed server configuration allow-list)",
];

export type ApplicationFileWriteBody = {
  /** File text to persist; empty string is allowed by the REST contract. */
  content: string;
};

/** Jackson / JAXB root for ApplicationFileSummary (UNWRAP_ROOT_VALUE on PUT). */
export const APPLICATION_FILE_ROOT = "ApplicationFile";

/** Wire envelope required by CXF UNWRAP_ROOT_VALUE — bare `{ content }` is HTTP 400. */
export function wrapApplicationFileForWire(body: ApplicationFileWriteBody): {
  ApplicationFile: ApplicationFileWriteBody;
} {
  return { [APPLICATION_FILE_ROOT]: body };
}

function parseList(payload: unknown): ApplicationFileSummary[] {
  if (payload == null) return [];
  if (Array.isArray(payload)) return payload as ApplicationFileSummary[];
  if (typeof payload === "object") {
    const obj = payload as Record<string, unknown>;
    for (const key of [
      "ApplicationFile",
      "applicationFile",
      "ApplicationFiles",
      "entries",
    ] as const) {
      const raw = obj[key];
      if (raw == null) continue;
      if (Array.isArray(raw)) return raw as ApplicationFileSummary[];
      if (typeof raw === "object") return [raw as ApplicationFileSummary];
    }
    throw new Error("Unexpected application file list payload");
  }
  throw new Error("Unexpected application file list payload type");
}

/**
 * Unwrap Jackson WRAP_ROOT_VALUE `{"ApplicationFile":{…}}` so GET/PUT payloads
 * bind the same as a flat ApplicationFileSummary.
 */
export function unwrapApplicationFile(payload: unknown): ApplicationFileSummary {
  if (payload == null || typeof payload !== "object" || Array.isArray(payload)) {
    throw new Error("Application file not found or empty response");
  }
  const root = payload as Record<string, unknown>;
  const nested = root.ApplicationFile ?? root.applicationFile;
  let body: ApplicationFileSummary;
  if (nested != null && typeof nested === "object" && !Array.isArray(nested)) {
    body = nested as ApplicationFileSummary;
  } else {
    body = root as ApplicationFileSummary;
  }
  if (!body.path || !String(body.path).trim()) {
    throw new Error("Application file response missing path");
  }
  return body;
}

function withGaps(f: ApplicationFileSummary): ApplicationFileSummary {
  return {
    ...f,
    designGaps:
      f.designGaps && f.designGaps.length > 0
        ? f.designGaps
        : [...APPLICATION_FILE_DESIGN_GAPS],
  };
}

function appKey(app: string): string {
  return encodeURIComponent(app.trim());
}

function contentUrl(app: string, relativePath: string): string {
  const params = new URLSearchParams();
  params.set("path", relativePath);
  return `${PATHS.APPLICATION_FILES}/${appKey(app)}/content?${params.toString()}`;
}

/** GET /services/applicationfiles/{app} — list omits designGaps on the wire. */
export async function listApplicationFiles(
  app: string,
): Promise<ApplicationFileSummary[]> {
  const name = (app || "").trim();
  if (!name) {
    throw new Error("application name is required");
  }
  return parseList(await get<unknown>(`${PATHS.APPLICATION_FILES}/${appKey(name)}`));
}

/** GET /services/applicationfiles/{app}/content?path= */
export async function getApplicationFileDetail(
  app: string,
  relativePath: string,
): Promise<ApplicationFileSummary> {
  const name = (app || "").trim();
  const path = (relativePath || "").trim();
  if (!name) {
    throw new Error("application name is required");
  }
  if (!path) {
    throw new Error("path is required");
  }
  return withGaps(unwrapApplicationFile(await get<unknown>(contentUrl(name, path))));
}

/**
 * PUT /services/applicationfiles/{app}/content?path= — Admin.
 * Query path selects the file; body path is ignored for persistence.
 */
export async function updateApplicationFile(
  app: string,
  relativePath: string,
  body: ApplicationFileWriteBody,
): Promise<ApplicationFileSummary> {
  const name = (app || "").trim();
  const path = (relativePath || "").trim();
  if (!name) {
    throw new Error("application name is required");
  }
  if (!path) {
    throw new Error("path is required");
  }
  if (body == null || body.content == null) {
    throw new Error("content is required");
  }
  const payload = await put<unknown>(
    contentUrl(name, path),
    wrapApplicationFileForWire({ content: body.content }),
  );
  return withGaps(unwrapApplicationFile(payload));
}
