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

import type { PublishingJob } from "./types";

/** Fields shown when a Status row is selected. Missing values are empty strings. */
export interface StatusJobDetailView {
  jobId: string;
  site: string;
  editionName: string;
  status: string;
  /** Non-empty only when the job failed and the API sent error text. */
  errorText: string;
  showError: boolean;
}

function asText(value: unknown): string {
  if (typeof value === "string") {
    return value;
  }
  if (typeof value === "number" && Number.isFinite(value)) {
    return String(value);
  }
  return "";
}

function firstText(record: Record<string, unknown>, keys: string[]): string {
  for (const key of keys) {
    const text = asText(record[key]);
    if (text !== "") {
      return text;
    }
  }
  return "";
}

/** True when status text describes a failure, including completed-with-failures. */
export function isFailedJobStatus(status: string | null | undefined): boolean {
  if (status == null || status.trim() === "") {
    return false;
  }
  return status.toLowerCase().includes("fail");
}

/**
 * Normalize a current-job row for the detail panel. Non-string optional fields
 * become empty text and never throw.
 */
export function statusJobDetail(job: PublishingJob | null | undefined): StatusJobDetailView {
  const record =
    job != null && typeof job === "object" ? (job as Record<string, unknown>) : {};
  const status = asText(record.status);
  const failed = isFailedJobStatus(status);
  const errorText = failed
    ? firstText(record, ["errorMessage", "error", "errorText"])
    : "";
  return {
    jobId: asText(record.jobId),
    site: asText(record.siteName),
    editionName: firstText(record, ["editionName", "edition"]),
    status,
    errorText,
    showError: failed,
  };
}
