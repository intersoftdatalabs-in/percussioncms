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

/** Value written by PUT. Blank (including whitespace-only) clears the stored markup. */
export function headContentForSave(draft: string): string {
  const trimmed = draft.trim();
  return trimmed.length === 0 ? "" : trimmed;
}

/** True when a save would not change the stored markup (including both blank). */
export function headContentMatch(saved: string, draft: string): boolean {
  return headContentForSave(saved) === headContentForSave(draft);
}

export type SiteAdditionalHeadHttpFailure = "bad_request" | "forbidden" | "conflict";

/** HTTP statuses that must stay on the form and must not be treated as success. */
export function siteAdditionalHeadHttpFailure(
  status: number,
): SiteAdditionalHeadHttpFailure | null {
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
