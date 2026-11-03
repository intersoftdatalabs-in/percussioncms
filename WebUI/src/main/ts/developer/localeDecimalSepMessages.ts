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
 * Locale decimal-separator chrome for Developer → Locales (#5518).
 *
 * <p>Kept out of {@code messages.ts} so sibling Developer slices can land without
 * editing that shared catalog. English after {@code @} is the offline fallback.
 */
export const LOC_DECIMAL_MSG_KEYS = {
  ACTION: "perc.ui.developer@Set locale decimal separator",
  HINT: "perc.ui.developer@Sets the locale decimal separator with the locale update. The new separator shows only after that save succeeds. The language string stays. The label, description, enabled status, date/time patterns, grouping separator, and other format fields stay because this update does not send them. A blank decimal separator clears the decimal separator. Cancel does not write.",
  FIELD: "perc.ui.developer@Decimal separator",
  SAVE: "perc.ui.developer@Save decimal separator",
  SAVED: "perc.ui.developer@Locale decimal separator saved.",
  CLEARED: "perc.ui.developer@Locale decimal separator cleared.",
  ERROR: "perc.ui.developer@Could not change the locale decimal separator.",
  FORBIDDEN: "perc.ui.developer@You need the Admin role to change a locale decimal separator.",
  INVALID: "perc.ui.developer@The locale decimal separator was not changed.",
  CONFLICT:
    "perc.ui.developer@The locale decimal separator was not changed. The previous separator is unchanged.",
  CANCEL: "perc.ui.developer@Cancel",
} as const;

type LocaleDecimalSepMsgKey = keyof typeof LOC_DECIMAL_MSG_KEYS;

export const LOC_DECIMAL_MSG: { readonly [K in LocaleDecimalSepMsgKey]: string } = new Proxy(
  LOC_DECIMAL_MSG_KEYS,
  {
    get(target, prop: string | symbol) {
      if (typeof prop !== "string" || !(prop in target)) {
        return undefined;
      }
      return message(target[prop as LocaleDecimalSepMsgKey]);
    },
  },
) as { readonly [K in LocaleDecimalSepMsgKey]: string };
