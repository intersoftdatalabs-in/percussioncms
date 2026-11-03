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
 * Locale date-time-pattern chrome for Developer → Locales (#5517).
 *
 * <p>Kept out of {@code messages.ts} so sibling Developer slices can land without
 * editing that shared catalog. English after {@code @} is the offline fallback.
 */
export const LOC_DATETIME_MSG_KEYS = {
  ACTION: "perc.ui.developer@Set locale date-time pattern",
  HINT: "perc.ui.developer@Sets the locale date-time pattern with the locale update. The new pattern shows only after that save succeeds. The language string stays. The label, description, enabled status, and other format fields stay because this update does not send them. A blank date-time pattern clears the date-time pattern. Cancel does not write.",
  FIELD: "perc.ui.developer@Date-time pattern",
  SAVE: "perc.ui.developer@Save date-time pattern",
  SAVED: "perc.ui.developer@Locale date-time pattern saved.",
  CLEARED: "perc.ui.developer@Locale date-time pattern cleared.",
  ERROR: "perc.ui.developer@Could not change the locale date-time pattern.",
  FORBIDDEN: "perc.ui.developer@You need the Admin role to change a locale date-time pattern.",
  INVALID: "perc.ui.developer@The locale date-time pattern was not changed.",
  CONFLICT:
    "perc.ui.developer@The locale date-time pattern was not changed. The previous pattern is unchanged.",
  CANCEL: "perc.ui.developer@Cancel",
} as const;

type LocaleDateTimePatternMsgKey = keyof typeof LOC_DATETIME_MSG_KEYS;

export const LOC_DATETIME_MSG: { readonly [K in LocaleDateTimePatternMsgKey]: string } = new Proxy(
  LOC_DATETIME_MSG_KEYS,
  {
    get(target, prop: string | symbol) {
      if (typeof prop !== "string" || !(prop in target)) {
        return undefined;
      }
      return message(target[prop as LocaleDateTimePatternMsgKey]);
    },
  },
) as { readonly [K in LocaleDateTimePatternMsgKey]: string };
