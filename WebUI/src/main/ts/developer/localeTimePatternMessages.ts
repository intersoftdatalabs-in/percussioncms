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
 * Locale time-pattern chrome for Developer → Locales (#5516).
 *
 * <p>Kept out of {@code messages.ts} so sibling Developer slices can land without
 * editing that shared catalog. English after {@code @} is the offline fallback.
 */
export const LOC_TIME_MSG_KEYS = {
  ACTION: "perc.ui.developer@Set locale time pattern",
  HINT: "perc.ui.developer@Sets the locale time pattern with the locale update. The new pattern shows only after that save succeeds. The language string stays. The label, description, enabled status, and other format fields stay because this update does not send them. A blank time pattern clears the time pattern. Cancel does not write.",
  FIELD: "perc.ui.developer@Time pattern",
  SAVE: "perc.ui.developer@Save time pattern",
  SAVED: "perc.ui.developer@Locale time pattern saved.",
  CLEARED: "perc.ui.developer@Locale time pattern cleared.",
  ERROR: "perc.ui.developer@Could not change the locale time pattern.",
  FORBIDDEN: "perc.ui.developer@You need the Admin role to change a locale time pattern.",
  INVALID: "perc.ui.developer@The locale time pattern was not changed.",
  CONFLICT:
    "perc.ui.developer@The locale time pattern was not changed. The previous pattern is unchanged.",
  CANCEL: "perc.ui.developer@Cancel",
} as const;

type LocaleTimePatternMsgKey = keyof typeof LOC_TIME_MSG_KEYS;

export const LOC_TIME_MSG: { readonly [K in LocaleTimePatternMsgKey]: string } = new Proxy(
  LOC_TIME_MSG_KEYS,
  {
    get(target, prop: string | symbol) {
      if (typeof prop !== "string" || !(prop in target)) {
        return undefined;
      }
      return message(target[prop as LocaleTimePatternMsgKey]);
    },
  },
) as { readonly [K in LocaleTimePatternMsgKey]: string };
