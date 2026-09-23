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

function jobSiteHaystack(job: PublishingJob): string {
  return [job.siteName, job.siteId]
    .filter((v) => v != null && String(v) !== "")
    .join(" ")
    .toLowerCase();
}

/**
 * Narrow already-loaded status jobs by site name or site id (case-insensitive
 * substring). Blank query returns the original list.
 */
export function filterJobsBySite(
  jobs: PublishingJob[],
  query: string,
): PublishingJob[] {
  const q = query.trim().toLowerCase();
  if (!q) {
    return jobs;
  }
  return jobs.filter((job) => jobSiteHaystack(job).includes(q));
}
