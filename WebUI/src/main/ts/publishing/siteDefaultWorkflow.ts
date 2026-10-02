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

/**
 * PUT /services/sites stores an existing workflow catalog name on the site
 * folder. A blank choice is not a workflow and must not be written.
 */
export function normalizeWorkflowName(raw: string): string {
  return raw.trim();
}

/** True when a save would not change the stored workflow name. */
export function workflowNamesMatch(saved: string, draft: string): boolean {
  return normalizeWorkflowName(saved).toLowerCase() === normalizeWorkflowName(draft).toLowerCase();
}

/**
 * Catalog name to persist, or a reason the choice must not be saved.
 * Matching is case-insensitive; the returned name is the catalog spelling.
 */
export function resolveCatalogWorkflow(
  draft: string,
  catalog: readonly string[],
): string | "empty" | "unknown" {
  const next = normalizeWorkflowName(draft);
  if (!next) {
    return "empty";
  }
  for (const name of catalog) {
    const canonical = normalizeWorkflowName(name);
    if (canonical && canonical.toLowerCase() === next.toLowerCase()) {
      return canonical;
    }
  }
  return "unknown";
}

export type SiteDefaultWorkflowHttpFailure = "bad_request" | "forbidden" | "conflict";

/** HTTP statuses that must stay on the form and must not be treated as success. */
export function siteDefaultWorkflowHttpFailure(
  status: number,
): SiteDefaultWorkflowHttpFailure | null {
  if (status === 400) {
    return "bad_request";
  }
  if (status === 403) {
    return "forbidden";
  }
  if (status === 409) {
    return "conflict";
  }
  return null;
}
