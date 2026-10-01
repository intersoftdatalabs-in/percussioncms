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
 * PUT /services/sites only applies a non-blank defaultFileExtention (blank is
 * ignored, not cleared, and is not a 400). An empty draft must not be written.
 * The wire field keeps the product spelling defaultFileExtention.
 */
const EXTENSION_RE = /^[A-Za-z0-9]{1,16}$/;

/** Trim and drop one leading dot. Does not accept the value. */
export function normalizeFileExtension(raw: string): string {
  const trimmed = raw.trim();
  if (trimmed.startsWith(".")) {
    return trimmed.slice(1);
  }
  return trimmed;
}

export function isValidFileExtension(raw: string): boolean {
  return EXTENSION_RE.test(normalizeFileExtension(raw));
}

/** True when a save would not change the stored extension. */
export function fileExtensionsMatch(saved: string, draft: string): boolean {
  return normalizeFileExtension(saved) === normalizeFileExtension(draft);
}

export type SiteFileExtensionHttpFailure = "bad_request" | "forbidden" | "conflict";

/** HTTP statuses that must stay on the form and must not be treated as success. */
export function siteFileExtensionHttpFailure(
  status: number,
): SiteFileExtensionHttpFailure | null {
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
