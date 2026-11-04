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
 * Locale first-day-of-week chrome for Developer → Locales (#5573).
 *
 * <p>Kept out of {@code messages.ts} so sibling Developer slices can land without
 * editing that shared catalog. English after {@code @} is the offline fallback.
 */
export const LOC_FIRST_DAY_MSG_KEYS = {
  ACTION: "perc.ui.developer@Set locale first day of week",
  HINT: "perc.ui.developer@Sets the locale first day of week (1=Monday through 7=Sunday) with the locale update. The new value shows only after that save succeeds. The language string stays. The label, description, enabled status, date/time patterns, separators, currency fields, text direction, and other format fields stay because this update does not send them. A blank first day of week clears the first day of week. Values outside 1–7 do not write. Cancel does not write.",
  FIELD: "perc.ui.developer@First day of week",
  SAVE: "perc.ui.developer@Save first day of week",
  SAVED: "perc.ui.developer@Locale first day of week saved.",
  CLEARED: "perc.ui.developer@Locale first day of week cleared.",
  ERROR: "perc.ui.developer@Could not change the locale first day of week.",
  FORBIDDEN: "perc.ui.developer@You need the Admin role to change a locale first day of week.",
  INVALID: "perc.ui.developer@The locale first day of week was not changed.",
  CONFLICT:
    "perc.ui.developer@The locale first day of week was not changed. The previous first day of week is unchanged.",
  CANCEL: "perc.ui.developer@Cancel",
} as const;

type LocaleFirstDayMsgKey = keyof typeof LOC_FIRST_DAY_MSG_KEYS;

export const LOC_FIRST_DAY_MSG: { readonly [K in LocaleFirstDayMsgKey]: string } = new Proxy(
  LOC_FIRST_DAY_MSG_KEYS,
  {
    get(target, prop: string | symbol) {
      if (typeof prop !== "string" || !(prop in target)) {
        return undefined;
      }
      return message(target[prop as LocaleFirstDayMsgKey]);
    },
  },
) as { readonly [K in LocaleFirstDayMsgKey]: string };
