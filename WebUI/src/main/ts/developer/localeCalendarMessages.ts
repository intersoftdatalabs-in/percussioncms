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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { message } from "../i18n/message";

/**
 * Locale calendar chrome for Developer → Locales (#5681).
 *
 * <p>Kept out of {@code messages.ts} so sibling Developer slices can land without
 * editing that shared catalog. English after {@code @} is the offline fallback.
 */
export const LOC_CALENDAR_MSG_KEYS = {
  ACTION: "perc.ui.developer@Set locale calendar",
  HINT: "perc.ui.developer@Sets the locale calendar with the locale update. The new value shows only after that save succeeds. The language string stays. The label, description, enabled status, date/time patterns, separators, currency fields, text direction, first day of week, measurement system, default timezone, and numbering system stay because this update does not send them. A blank calendar clears the calendar. A calendar longer than 32 characters does not write. Cancel does not write.",
  FIELD: "perc.ui.developer@Calendar",
  SAVE: "perc.ui.developer@Save calendar",
  SAVED: "perc.ui.developer@Locale calendar saved.",
  CLEARED: "perc.ui.developer@Locale calendar cleared.",
  ERROR: "perc.ui.developer@Could not change the locale calendar.",
  FORBIDDEN:
    "perc.ui.developer@You need the Admin role to change a locale calendar.",
  INVALID: "perc.ui.developer@The locale calendar was not changed.",
  CONFLICT:
    "perc.ui.developer@The locale calendar was not changed. The previous calendar is unchanged.",
  CANCEL: "perc.ui.developer@Cancel",
} as const;

type LocaleCalendarMsgKey = keyof typeof LOC_CALENDAR_MSG_KEYS;

export const LOC_CALENDAR_MSG: {
  readonly [K in LocaleCalendarMsgKey]: string;
} = new Proxy(LOC_CALENDAR_MSG_KEYS, {
  get(target, prop: string | symbol) {
    if (typeof prop !== "string" || !(prop in target)) {
      return undefined;
    }
    return message(target[prop as LocaleCalendarMsgKey]);
  },
}) as { readonly [K in LocaleCalendarMsgKey]: string };
