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
 * Locale measurement-system chrome for Developer → Locales (#5580).
 *
 * <p>Kept out of {@code messages.ts} so sibling Developer slices can land without
 * editing that shared catalog. English after {@code @} is the offline fallback.
 */
export const LOC_MEASURE_MSG_KEYS = {
  ACTION: "perc.ui.developer@Set locale measurement system",
  HINT: "perc.ui.developer@Sets the locale measurement system (us, uk, or metric) with the locale update. The new value shows only after that save succeeds. The language string stays. The label, description, enabled status, date/time patterns, separators, currency fields, text direction, first day of week, and other format fields stay because this update does not send them. A blank measurement system clears the measurement system. Values other than us, uk, and metric do not write. Cancel does not write.",
  FIELD: "perc.ui.developer@Measurement system",
  SAVE: "perc.ui.developer@Save measurement system",
  SAVED: "perc.ui.developer@Locale measurement system saved.",
  CLEARED: "perc.ui.developer@Locale measurement system cleared.",
  ERROR: "perc.ui.developer@Could not change the locale measurement system.",
  FORBIDDEN: "perc.ui.developer@You need the Admin role to change a locale measurement system.",
  INVALID: "perc.ui.developer@The locale measurement system was not changed.",
  CONFLICT:
    "perc.ui.developer@The locale measurement system was not changed. The previous measurement system is unchanged.",
  CANCEL: "perc.ui.developer@Cancel",
} as const;

type LocaleMeasurementMsgKey = keyof typeof LOC_MEASURE_MSG_KEYS;

export const LOC_MEASURE_MSG: { readonly [K in LocaleMeasurementMsgKey]: string } = new Proxy(
  LOC_MEASURE_MSG_KEYS,
  {
    get(target, prop: string | symbol) {
      if (typeof prop !== "string" || !(prop in target)) {
        return undefined;
      }
      return message(target[prop as LocaleMeasurementMsgKey]);
    },
  },
) as { readonly [K in LocaleMeasurementMsgKey]: string };
