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
 * Schedule publish/removal dates from the React Content Editor host
 * (#4917 / parent #4532).
 *
 * <p>Reuses itemmanagement get/set item dates. HTTP 400/403 and HTTP 200
 * {@code FORBIDDEN}/{@code INVALID}/{@code BADCONFIG} are failures — the
 * host must not show success.</p>
 */

import {
  fetchItemScheduleDates,
  saveItemScheduleDates,
} from "../api/publishing/itemScheduleDatesApi";
import {
  formatApiError,
  isApiError,
  isSessionRedirectError,
} from "../api/client";
import type { ItemScheduleDates } from "../contentExplorer/itemScheduleDates";
import { message } from "../i18n/message";
import { EDITOR_MSG } from "./messages";

export type { ItemScheduleDates };

export const loadEditorScheduleDates = fetchItemScheduleDates;
export const saveEditorScheduleDates = saveItemScheduleDates;

/**
 * User-visible failure for a schedule save/load. Empty when the session is
 * already redirecting to login (caller should not claim success or failure).
 */
export function editorScheduleFailureMessage(err: unknown): string {
  if (isSessionRedirectError(err)) {
    return "";
  }
  if (isApiError(err)) {
    if (err.status === 403) {
      return formatApiError(err, message(EDITOR_MSG.SCHEDULE_FORBIDDEN));
    }
    if (err.status === 400) {
      return formatApiError(err, message(EDITOR_MSG.SCHEDULE_INVALID));
    }
  }
  const text = formatApiError(err, message(EDITOR_MSG.SCHEDULE_FAILED));
  if (/\bFORBIDDEN\b/i.test(text)) {
    return message(EDITOR_MSG.SCHEDULE_FORBIDDEN);
  }
  if (/\bINVALID\b/i.test(text) || /\bBADCONFIG\b/i.test(text)) {
    return message(EDITOR_MSG.SCHEDULE_INVALID);
  }
  return text;
}
