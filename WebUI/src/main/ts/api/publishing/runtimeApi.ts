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
import { SERVICES_ROOT } from "../paths";
import { stopPublishing } from "./serversApi";

export interface RuntimeEditionStatus {
  editionId?: string;
  name?: string;
  siteId?: string;
  comment?: string;
  runningJobId?: number;
  jobStatus?: string;
}

export interface RuntimeJobResponse {
  jobId?: number;
  editionId?: string;
  status?: string;
  delivered?: number;
  failed?: number;
  requestId?: number;
}

export interface DemandPublishRequest {
  contentIds: string[];
  folderIds?: string[];
}

function runtimeRoot(): string {
  return `${SERVICES_ROOT}/sitemanage/publishingdesign/runtime`;
}

export async function listRuntimeEditions(
  siteId: string | number,
): Promise<RuntimeEditionStatus[]> {
  const data = await get<unknown>(
    `${runtimeRoot()}/editions?siteId=${encodeURIComponent(String(siteId))}`,
  );
  return normalizeArray(data);
}

export async function startEditionJob(
  editionId: string | number,
): Promise<RuntimeJobResponse> {
  return (await post<unknown>(
    `${runtimeRoot()}/editions/${encodeURIComponent(String(editionId))}/start`,
  )) as RuntimeJobResponse;
}

export async function stopRuntimeJob(
  jobId: string | number,
): Promise<RuntimeJobResponse> {
  try {
    return (await post<unknown>(
      `${runtimeRoot()}/jobs/${encodeURIComponent(String(jobId))}/stop`,
    )) as RuntimeJobResponse;
  } catch {
    // Fall back to ops stopPublishing path
    await stopPublishing(jobId);
    return { jobId: Number(jobId), status: "cancelled" };
  }
}

export async function getRuntimeJob(
  jobId: string | number,
): Promise<RuntimeJobResponse> {
  return (await get<unknown>(
    `${runtimeRoot()}/jobs/${encodeURIComponent(String(jobId))}`,
  )) as RuntimeJobResponse;
}

export async function demandPublish(
  editionId: string | number,
  request: DemandPublishRequest,
): Promise<RuntimeJobResponse> {
  return (await post<unknown>(
    `${runtimeRoot()}/editions/${encodeURIComponent(String(editionId))}/demand`,
    request,
  )) as RuntimeJobResponse;
}

export async function purgeRuntimeJobLog(jobId: string | number): Promise<void> {
  await post(
    `${runtimeRoot()}/logs/purge?jobId=${encodeURIComponent(String(jobId))}`,
  );
}

export async function clearSiteItems(siteId: string | number): Promise<void> {
  await post(
    `${runtimeRoot()}/sites/${encodeURIComponent(String(siteId))}/clearItems`,
  );
}

function normalizeArray<T>(data: unknown): T[] {
  if (Array.isArray(data)) {
    return data as T[];
  }
  if (data && typeof data === "object") {
    for (const key of Object.keys(data as object)) {
      const v = (data as Record<string, unknown>)[key];
      if (Array.isArray(v)) {
        return v as T[];
      }
    }
  }
  return [];
}
