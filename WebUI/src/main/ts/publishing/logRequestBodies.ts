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

import type { SitePublishLogRequest } from "./types";

/**
 * Minuet / sitemanage JSON root wrappers for pubstatus POST bodies.
 * Field names match Java DTOs (jobid / jobids), not camelCase.
 *
 * @see com.percussion.sitemanage.data.PSSitePublishPurgeRequest
 * @see com.percussion.sitemanage.data.PSSitePublishLogDetailsRequest
 * @see com.percussion.sitemanage.data.PSSitePublishLogRequest
 * @see WebUI PercPublisherService purgeJob / getPublishingLogDetails
 */

function toJobIdNumber(id: string | number): number {
  if (typeof id === "number" && Number.isFinite(id)) {
    return id;
  }
  const n = Number(String(id).trim());
  if (!Number.isFinite(n)) {
    throw new Error(`Invalid publish job id: ${String(id)}`);
  }
  return n;
}

/** Body for POST /pubstatus/purge/ */
export function buildPurgeRequestBody(
  jobIds: Array<string | number>,
): Record<string, unknown> {
  const jobids = jobIds.map(toJobIdNumber);
  return {
    SitePublishPurgeRequest: {
      jobids,
    },
  };
}

/** Body for POST /pubstatus/details/ */
export function buildLogDetailsRequestBody(
  jobId: string | number,
): Record<string, unknown> {
  return {
    SitePublishLogDetailsRequest: {
      jobid: toJobIdNumber(jobId),
    },
  };
}

/** Body for POST /pubstatus/logs/ — wrap filter fields under DTO root name. */
export function buildLogListRequestBody(
  request: SitePublishLogRequest,
): Record<string, unknown> {
  return {
    SitePublishLogRequest: { ...request },
  };
}
