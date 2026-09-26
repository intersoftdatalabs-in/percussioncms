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

import { formatApiError } from "../api/client";
import { message, MSG } from "../i18n/message";
import {
  mapPublishError,
  mapPublishResponse,
  type PublishActionResult,
} from "./publishActions";
import type { PublishingJob } from "./types";

export type JobRetryKind = "full" | "incremental";

function named(value: unknown): string {
  return String(value ?? "").trim();
}

/** Server display name from either wire field the status payload uses. */
export function jobServerName(
  job: Pick<PublishingJob, "serverName" | "pubServerName">,
): string {
  const direct = named(job.serverName);
  if (direct !== "") {
    return direct;
  }
  return named(job.pubServerName);
}

/**
 * True when Status may offer retry: failed or completed-with-failures, and both
 * a site name and a server name are present.
 */
export function isJobRetryable(
  job: Pick<
    PublishingJob,
    "jobId" | "status" | "siteName" | "serverName" | "pubServerName"
  >,
): boolean {
  if (job.jobId === undefined || job.jobId === null || String(job.jobId) === "") {
    return false;
  }
  if (!named(job.status).toLowerCase().includes("fail")) {
    return false;
  }
  if (named(job.siteName) === "") {
    return false;
  }
  return jobServerName(job) !== "";
}

/**
 * Incremental when the payload says so (publishKind, pubType, or edition name).
 * Otherwise full site publish.
 */
export function jobRetryKind(
  job: Pick<PublishingJob, "publishKind" | "pubType" | "editionName">,
): JobRetryKind {
  const parts = [job.publishKind, job.pubType, job.editionName];
  for (const part of parts) {
    const text = named(part).toLowerCase();
    if (text.includes("incremental")) {
      return "incremental";
    }
  }
  return "full";
}

function retryMessage(result: PublishActionResult): string {
  if (result.message && result.message !== result.token) {
    return result.message;
  }
  if (result.state === "forbidden") {
    return message(MSG.PUBLISH_FORBIDDEN);
  }
  if (result.state === "badconfig") {
    return message(MSG.PUBLISH_BADCONFIG);
  }
  return result.message || message(MSG.PUBLISH_ERROR);
}

/**
 * Application-level FORBIDDEN / BADCONFIG on an HTTP 200 publish body.
 * Returns null when the response is a started job.
 */
export function mapJobRetryResponse(data: unknown): string | null {
  const preflight = mapPublishResponse(data);
  if (!preflight) {
    return null;
  }
  return retryMessage(preflight);
}

/** Thrown publish client errors, including HTTP 403. Never a success string. */
export function mapJobRetryError(err: unknown): string {
  return retryMessage(mapPublishError(err)) || formatApiError(err, message(MSG.PUBLISH_ERROR));
}
