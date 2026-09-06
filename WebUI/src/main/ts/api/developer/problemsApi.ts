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

import { get } from "../client";
import { asJsonRecord, asObjectArray } from "../jsonList";
import { PATHS } from "../paths";
import {
  DEVELOPER_SECTIONS,
  type DeveloperSection,
} from "../../app/deepLinks/allowlists";

/**
 * Developer session design/validation problem (GET /services/problems).
 * Catalog tokens only — never filesystem paths or JDBC URLs.
 */
export type DesignProblem = {
  id: string;
  severity: "ERROR" | "WARNING";
  code?: string;
  message: string;
  objectType?: string;
  objectId?: string;
  objectName?: string;
  location?: string;
  navigateSection?: DeveloperSection;
};

/** Matches REST ProblemsAdaptor SAFE_FIXTURE. */
export const PROBLEMS_FIXTURE_RE = /^[A-Za-z][A-Za-z0-9_-]{0,63}$/;

/** Known invalid open-editor/session fixture (Workbench §12.4 increment). */
export const INVALID_SESSION_FIXTURE = "invalid-session";

const NAVIGATE_SET = new Set<string>(DEVELOPER_SECTIONS);

export function isSafeProblemsFixture(fixture: string): boolean {
  return PROBLEMS_FIXTURE_RE.test(fixture);
}

export function isDeveloperNavigateSection(section: string): section is DeveloperSection {
  return NAVIGATE_SET.has(section);
}

function parseNamedList(payload: unknown, names: readonly string[]): unknown[] {
  if (payload == null) return [];
  if (Array.isArray(payload)) return payload;
  const obj = asJsonRecord(payload);
  if (!obj) {
    return [];
  }
  for (const key of names) {
    const raw = obj[key];
    if (raw == null) continue;
    if (Array.isArray(raw)) return raw;
    if (typeof raw === "object") return [raw];
  }
  if (typeof obj.id === "string" || typeof obj.message === "string") {
    return [obj];
  }
  return asObjectArray(payload);
}

function asOptionalString(value: unknown): string | undefined {
  if (typeof value !== "string") return undefined;
  const trimmed = value.trim();
  return trimmed ? trimmed : undefined;
}

export function unwrapDesignProblems(payload: unknown): DesignProblem[] {
  const raw = parseNamedList(payload, ["DesignProblem", "designProblem", "problems"]);
  const out: DesignProblem[] = [];
  for (const item of raw) {
    const rec = asJsonRecord(item);
    if (!rec) continue;
    const id = typeof rec.id === "string" ? rec.id.trim() : "";
    if (!id || !isSafeProblemsFixture(id)) continue;
    const message = typeof rec.message === "string" ? rec.message.trim() : "";
    if (!message) continue;
    const sevRaw = typeof rec.severity === "string" ? rec.severity.trim().toUpperCase() : "";
    const severity = sevRaw === "WARNING" ? "WARNING" : sevRaw === "ERROR" ? "ERROR" : null;
    if (!severity) continue;
    const navRaw = asOptionalString(rec.navigateSection);
    const navigateSection =
      navRaw && isDeveloperNavigateSection(navRaw) ? navRaw : undefined;
    out.push({
      id,
      severity,
      code: asOptionalString(rec.code),
      message,
      objectType: asOptionalString(rec.objectType),
      objectId: asOptionalString(rec.objectId),
      objectName: asOptionalString(rec.objectName),
      location: asOptionalString(rec.location),
      navigateSection,
    });
  }
  return out;
}

export function problemsListUrl(fixture?: string | null): string {
  const token = (fixture ?? "").trim();
  if (!token) return PATHS.PROBLEMS;
  if (!isSafeProblemsFixture(token)) {
    throw new Error("Invalid Problems fixture");
  }
  return `${PATHS.PROBLEMS}?fixture=${encodeURIComponent(token)}`;
}

/** GET /services/problems — Admin session design problems. */
export async function listDesignProblems(
  fixture?: string | null,
): Promise<DesignProblem[]> {
  return unwrapDesignProblems(await get<unknown>(problemsListUrl(fixture)));
}
