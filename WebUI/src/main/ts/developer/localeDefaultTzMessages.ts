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

import { message } from "../i18n/message";

/**
 * Locale default-timezone chrome for Developer → Locales (#5581).
 *
 * <p>Kept out of {@code messages.ts} so sibling Developer slices can land without
 * editing that shared catalog. English after {@code @} is the offline fallback.
 */
export const LOC_DEFAULT_TZ_MSG_KEYS = {
  ACTION: "perc.ui.developer@Set locale default timezone",
  HINT: "perc.ui.developer@Sets the locale default timezone with the locale update. The new value shows only after that save succeeds. The language string stays. The label, description, enabled status, date/time patterns, separators, currency fields, text direction, first day of week, measurement system, numbering system, and calendar stay because this update does not send them. A blank default timezone clears the default timezone. A default timezone longer than 64 characters does not write. Cancel does not write.",
  FIELD: "perc.ui.developer@Default timezone",
  SAVE: "perc.ui.developer@Save default timezone",
  SAVED: "perc.ui.developer@Locale default timezone saved.",
  CLEARED: "perc.ui.developer@Locale default timezone cleared.",
  ERROR: "perc.ui.developer@Could not change the locale default timezone.",
  FORBIDDEN:
    "perc.ui.developer@You need the Admin role to change a locale default timezone.",
  INVALID: "perc.ui.developer@The locale default timezone was not changed.",
  CONFLICT:
    "perc.ui.developer@The locale default timezone was not changed. The previous default timezone is unchanged.",
  CANCEL: "perc.ui.developer@Cancel",
} as const;

type LocaleDefaultTzMsgKey = keyof typeof LOC_DEFAULT_TZ_MSG_KEYS;

export const LOC_DEFAULT_TZ_MSG: {
  readonly [K in LocaleDefaultTzMsgKey]: string;
} = new Proxy(LOC_DEFAULT_TZ_MSG_KEYS, {
  get(target, prop: string | symbol) {
    if (typeof prop !== "string" || !(prop in target)) {
      return undefined;
    }
    return message(target[prop as LocaleDefaultTzMsgKey]);
  },
}) as { readonly [K in LocaleDefaultTzMsgKey]: string };
