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

export const CONNECTION_TEST_SUCCESS = "Connection test succeeded.";

export const CONNECTION_TEST_FAILURE =
  "Connection test failed. Check the server settings and try again.";

export type ConnectionTestHttpFailure = "bad_request" | "forbidden" | "conflict";

/**
 * Read the connection-test outcome. The server answers
 * `{ connected: boolean }`; a bare boolean is accepted for tolerance.
 * Anything else is a failure so the notice never claims a success it
 * cannot prove.
 */
export function isConnectionTestSuccess(data: unknown): boolean {
  if (typeof data === "boolean") {
    return data;
  }
  if (data != null && typeof data === "object") {
    const value = (data as Record<string, unknown>).connected;
    return value === true;
  }
  return false;
}

/** HTTP statuses that must stay on the panel and must not be treated as success. */
export function connectionTestHttpFailure(status: number): ConnectionTestHttpFailure | null {
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
