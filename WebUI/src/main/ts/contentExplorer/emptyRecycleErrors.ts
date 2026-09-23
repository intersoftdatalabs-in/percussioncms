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
import { message } from "../i18n/message";
import { EXPLORER_MSG } from "./messages";

/**
 * Maps empty-recycle-bin HTTP 403/404/409 so Explorer never treats a failed
 * purge as success (#4762).
 */
export function formatEmptyRecycleError(err: unknown): string {
  if (isApiError(err)) {
    if (err.status === 403) {
      return message(EXPLORER_MSG.PERMISSION_DENIED);
    }
    if (err.status === 404) {
      return message(EXPLORER_MSG.ACTION_EMPTY_RECYCLE_NOT_FOUND);
    }
    if (err.status === 409) {
      return message(EXPLORER_MSG.ACTION_EMPTY_RECYCLE_CONFLICT);
    }
  }
  return formatApiError(err, message(EXPLORER_MSG.ERROR_GENERIC));
}
