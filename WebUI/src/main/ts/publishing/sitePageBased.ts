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

/** Wire boolean, or null when the site payload omitted the flag. */
export function parsePageBased(raw: unknown): boolean | null {
  if (typeof raw === "boolean") {
    return raw;
  }
  if (typeof raw === "string") {
    const value = raw.trim().toLowerCase();
    if (value === "true" || value === "yes" || value === "t") {
      return true;
    }
    if (value === "false" || value === "no" || value === "f") {
      return false;
    }
  }
  return null;
}

/** Display text for the open-site control. */
export function pageBasedLabel(value: boolean | null): string {
  if (value === true) {
    return "yes";
  }
  if (value === false) {
    return "no";
  }
  return "—";
}

export type PageBasedHttpFailure = "bad_request" | "forbidden" | "conflict";

/** HTTP statuses that must stay on the form and must not be treated as success. */
export function pageBasedHttpFailure(status: number): PageBasedHttpFailure | null {
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
