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
import {
  buildLogDetailsRequestBody,
  buildLogListRequestBody,
  buildPurgeRequestBody,
} from "../../publishing/logRequestBodies";
import type {
  PublishingJob,
  PublishingLogEntry,
  SitePublishLogRequest,
} from "./types";

function normalizeJobList(data: unknown): PublishingJob[] {
  if (data == null) {
    return [];
  }
  if (Array.isArray(data)) {
    return data as PublishingJob[];
  }
  if (typeof data === "object") {
    const obj = data as Record<string, unknown>;
    for (const key of ["SitePublishJob", "jobs", "job", "items"]) {
      const v = obj[key];
      if (Array.isArray(v)) {
        return v as PublishingJob[];
      }
      if (v && typeof v === "object") {
        return [v as PublishingJob];
      }
    }
  }
  return [];
}

/** Current publishing jobs (all sites). */
export async function fetchCurrentJobs(): Promise<PublishingJob[]> {
  const data = await get<unknown>(PATHS.PUBLISH_CURRENT_STATUS);
  return normalizeJobList(data);
}

/** Current publishing jobs for one site. */
export async function fetchCurrentJobsForSite(
  siteId: string | number,
): Promise<PublishingJob[]> {
  const data = await get<unknown>(
    `${PATHS.PUBLISH_CURRENT_STATUS}/${encodeURIComponent(String(siteId))}`,
  );
  return normalizeJobList(data);
}

/** Historical publish logs (POST body matches Minuet / PSSitePublishLogRequest). */
export async function fetchPublishingLogs(
  request: SitePublishLogRequest,
): Promise<PublishingLogEntry[]> {
  const data = await post<unknown>(
    `${PATHS.PUBLISH_LOGS}/`,
    buildLogListRequestBody(request),
  );
  if (Array.isArray(data)) {
    return data as PublishingLogEntry[];
  }
  if (data && typeof data === "object") {
    const obj = data as Record<string, unknown>;
    for (const key of ["SitePublishJob", "logs", "items"]) {
      const v = obj[key];
      if (Array.isArray(v)) {
        return v as PublishingLogEntry[];
      }
    }
  }
  return [];
}

/**
 * Job item details for a log row.
 * @param jobId publish job id (numeric string or number)
 */
export async function fetchLogDetails(
  jobId: string | number,
): Promise<unknown> {
  return post<unknown>(
    PATHS.PUBLISH_LOGS_DETAILS,
    buildLogDetailsRequestBody(jobId),
  );
}

/**
 * Purge selected log job ids.
 * @param jobIds list of job ids (numeric strings or numbers)
 */
export async function purgePublishingLogs(
  jobIds: Array<string | number>,
): Promise<unknown> {
  return post<unknown>(PATHS.PUBLISH_PURGE, buildPurgeRequestBody(jobIds));
}
