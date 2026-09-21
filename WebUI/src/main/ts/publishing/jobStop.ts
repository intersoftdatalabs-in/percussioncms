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

import { formatApiError, isApiError } from "../api/client";
import { message, MSG } from "../i18n/message";
import type { PublishingJob } from "./types";

/** True when Status may offer cancel/stop (in-flight job, not already stopping). */
export function isJobStoppable(
  job: Pick<PublishingJob, "jobId" | "status" | "isStopping">,
): boolean {
  if (job.jobId === undefined || job.jobId === null || String(job.jobId) === "") {
    return false;
  }
  if (job.isStopping) {
    return false;
  }
  return String(job.status ?? "")
    .toLowerCase()
    .includes("run");
}

/**
 * Map stopPublishing HTTP failures. 403/404/409 are never success chrome.
 */
export function mapJobStopError(err: unknown): string {
  if (isApiError(err)) {
    if (err.status === 403) {
      return formatApiError(err, message(MSG.PUBLISH_FORBIDDEN));
    }
    if (err.status === 404) {
      return formatApiError(err, message(MSG.PUBLISH_JOB_NOT_FOUND));
    }
    if (err.status === 409) {
      return formatApiError(err, message(MSG.PUBLISH_JOB_STOP_CONFLICT));
    }
  }
  return formatApiError(err, message(MSG.PUBLISH_ERROR));
}
