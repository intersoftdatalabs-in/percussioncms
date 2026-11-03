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
 * Locale date-pattern chrome for Developer → Locales (#5500).
 *
 * <p>Kept out of {@code messages.ts} so sibling Developer slices can land without
 * editing that shared catalog. English after {@code @} is the offline fallback.
 */
export const LOC_DATE_MSG_KEYS = {
  ACTION: "perc.ui.developer@Set locale date pattern",
  HINT: "perc.ui.developer@Sets the locale date pattern with the locale update. The new pattern shows only after that save succeeds. The language string stays. The label, description, enabled status, and other format fields stay because this update does not send them. A blank date pattern clears the date pattern. Cancel does not write.",
  FIELD: "perc.ui.developer@Date pattern",
  SAVE: "perc.ui.developer@Save date pattern",
  SAVED: "perc.ui.developer@Locale date pattern saved.",
  CLEARED: "perc.ui.developer@Locale date pattern cleared.",
  ERROR: "perc.ui.developer@Could not change the locale date pattern.",
  FORBIDDEN: "perc.ui.developer@You need the Admin role to change a locale date pattern.",
  INVALID: "perc.ui.developer@The locale date pattern was not changed.",
  CONFLICT:
    "perc.ui.developer@The locale date pattern was not changed. The previous pattern is unchanged.",
  CANCEL: "perc.ui.developer@Cancel",
} as const;

type LocaleDatePatternMsgKey = keyof typeof LOC_DATE_MSG_KEYS;

export const LOC_DATE_MSG: { readonly [K in LocaleDatePatternMsgKey]: string } = new Proxy(
  LOC_DATE_MSG_KEYS,
  {
    get(target, prop: string | symbol) {
      if (typeof prop !== "string" || !(prop in target)) {
        return undefined;
      }
      return message(target[prop as LocaleDatePatternMsgKey]);
    },
  },
) as { readonly [K in LocaleDatePatternMsgKey]: string };
