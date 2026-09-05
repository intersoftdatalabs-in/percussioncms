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

import { get, post } from "../client";
import { PATHS } from "../paths";
import type {
  ApplicationDetail,
  ApplicationSummary,
  ApplicationValidationResult,
  PipelineExecuteRequest,
  PipelineExecuteResult,
  PipelineIrDocument,
} from "./types";

export interface ListApplicationsOptions {
  name?: string;
  limit?: number;
  offset?: number;
}

/** Drop pre-lifecycle catalog strings once Admin start/stop ships (Slice B). */
const STALE_LIFECYCLE_GAP = /start\s*\/\s*stop|start\/stop.*not\s+supported/i;

function asArray<T>(payload: unknown): T[] {
  if (payload == null) return [];
  if (Array.isArray(payload)) return payload as T[];
  // Defensive: some CXF/XML-bridge shapes wrap a single element
  if (typeof payload === "object") {
    const obj = payload as Record<string, unknown>;
    const raw = obj.Application ?? obj.application;
    if (raw == null) return [];
    return Array.isArray(raw) ? (raw as T[]) : [raw as T];
  }
  return [];
}

/**
 * Unwrap Jackson WRAP_ROOT_VALUE {@code {"ApplicationDetail":{…}}} so GET/POST
 * payloads bind the same as a flat ApplicationDetail.
 */
export function unwrapApplicationDetail(payload: unknown): ApplicationDetail {
  if (payload == null || typeof payload !== "object" || Array.isArray(payload)) {
    throw new Error("Application detail not found or empty response");
  }
  const root = payload as Record<string, unknown>;
  const nested = root.ApplicationDetail ?? root.applicationDetail;
  const detail = (nested != null && typeof nested === "object" && !Array.isArray(nested)
    ? nested
    : root) as ApplicationDetail;
  return {
    ...detail,
    designGaps: withoutStalePipelineLifecycleGap(detail.designGaps),
  };
}

/** Drop stale REST start/stop gap strings now that Slice B lifecycle ships. */
export function withoutStalePipelineLifecycleGap(
  gaps: string[] | undefined | null,
): string[] {
  if (gaps == null || gaps.length === 0) return [];
  return gaps.filter((g) => !STALE_LIFECYCLE_GAP.test(g));
}

/** GET /services/pipelines?name=&limit=&offset= */
export async function listApplications(
  options: ListApplicationsOptions = {},
): Promise<ApplicationSummary[]> {
  const params = new URLSearchParams();
  if (options.name) params.set("name", options.name);
  if (options.limit != null) params.set("limit", String(options.limit));
  if (options.offset != null) params.set("offset", String(options.offset));
  const q = params.toString();
  const url = q ? `${PATHS.PIPELINES}?${q}` : PATHS.PIPELINES;
  const payload = await get<unknown>(url);
  return asArray<ApplicationSummary>(payload);
}

/** GET /services/pipelines/{idOrName} — name or numeric id */
export async function getApplicationDetail(
  idOrName: string,
): Promise<ApplicationDetail> {
  const key = encodeURIComponent(idOrName);
  const payload = await get<unknown>(`${PATHS.PIPELINES}/${key}`);
  return unwrapApplicationDetail(payload);
}

/**
 * POST /services/pipelines/{idOrName}/start — Admin. Idempotent when already running.
 * Returns refreshed ApplicationDetail with active=true.
 */
export async function startApplication(
  idOrName: string,
): Promise<ApplicationDetail> {
  const key = encodeURIComponent(idOrName);
  const payload = await post<unknown>(`${PATHS.PIPELINES}/${key}/start`);
  return unwrapApplicationDetail(payload);
}

/**
 * POST /services/pipelines/{idOrName}/stop — Admin. Idempotent when already stopped.
 * Returns refreshed ApplicationDetail with active=false.
 */
export async function stopApplication(
  idOrName: string,
): Promise<ApplicationDetail> {
  const key = encodeURIComponent(idOrName);
  const payload = await post<unknown>(`${PATHS.PIPELINES}/${key}/stop`);
  return unwrapApplicationDetail(payload);
}

/** GET /services/pipelines/{idOrName}/ir — read-only pipeline-ir-v1 document */
export async function getPipelineIr(
  idOrName: string,
): Promise<PipelineIrDocument> {
  const key = encodeURIComponent(idOrName);
  return get<PipelineIrDocument>(`${PATHS.PIPELINES}/${key}/ir`);
}

/**
 * Unwrap Jackson WRAP_ROOT_VALUE {@code {"PipelineExecuteResult":{…}}} so execute
 * responses bind the same as a flat result.
 */
export function unwrapPipelineExecuteResult(payload: unknown): PipelineExecuteResult {
  if (payload == null || typeof payload !== "object" || Array.isArray(payload)) {
    throw new Error("Pipeline execute result not found or empty response");
  }
  const root = payload as Record<string, unknown>;
  const nested = root.PipelineExecuteResult ?? root.pipelineExecuteResult;
  if (nested != null && typeof nested === "object" && !Array.isArray(nested)) {
    return nested as PipelineExecuteResult;
  }
  return root as PipelineExecuteResult;
}

/**
 * POST /services/pipelines/{app}/resources/{resource}/execute — native IR smoke invoke.
 * Body is {@link PipelineExecuteRequest} ({@code params}, {@code rows}, …).
 */
export async function executeResource(
  app: string,
  resource: string,
  body: PipelineExecuteRequest = {},
): Promise<PipelineExecuteResult> {
  const appKey = encodeURIComponent(app);
  const resourceKey = encodeURIComponent(resource);
  const payload = await post<unknown>(
    `${PATHS.PIPELINES}/${appKey}/resources/${resourceKey}/execute`,
    body,
  );
  return unwrapPipelineExecuteResult(payload);
}

/**
 * Unwrap Jackson WRAP_ROOT_VALUE {@code {"ApplicationValidationResult":{…}}}.
 */
export function unwrapApplicationValidationResult(
  payload: unknown,
): ApplicationValidationResult {
  if (payload == null || typeof payload !== "object" || Array.isArray(payload)) {
    throw new Error("Application validation result not found or empty response");
  }
  const root = payload as Record<string, unknown>;
  const nested =
    root.ApplicationValidationResult ?? root.applicationValidationResult;
  const result = (nested != null && typeof nested === "object" && !Array.isArray(nested)
    ? nested
    : root) as ApplicationValidationResult;
  const problems = result.problems;
  return {
    ...result,
    problems: Array.isArray(problems) ? problems : problems == null ? [] : [problems],
  };
}

/**
 * GET /services/pipelines/{idOrName}/validation — Admin problems summary (wave 3 REST).
 * Callers should feature-detect: treat HTTP 404 as “not deployed yet” soft-empty.
 */
export async function getApplicationValidation(
  idOrName: string,
): Promise<ApplicationValidationResult> {
  const key = encodeURIComponent(idOrName);
  const payload = await get<unknown>(`${PATHS.PIPELINES}/${key}/validation`);
  return unwrapApplicationValidationResult(payload);
}
