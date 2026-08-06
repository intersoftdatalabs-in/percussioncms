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

import {
  extractRestErrorMessage,
  isApiError,
  isSessionRedirectError,
} from "../api/client";
import { DEV_MSG } from "./messages";

/** Shared error message formatting for Developer catalog panels. */
export function panelErrMsg(err: unknown, fallback: string): string {
  if (isSessionRedirectError(err)) return DEV_MSG.SESSION_REDIRECT;
  if (isApiError(err)) {
    const fromBody = extractRestErrorMessage(err.body);
    if (fromBody) {
      return `${fallback} ${fromBody}`;
    }
    return `${fallback} (${err.status})`;
  }
  if (err instanceof Error && err.message) return `${fallback} ${err.message}`;
  return fallback;
}
