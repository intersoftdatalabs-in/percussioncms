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
 * Locale numbering-system chrome for Developer → Locales (#5582).
 *
 * <p>Kept out of {@code messages.ts} so sibling Developer slices can land without
 * editing that shared catalog. English after {@code @} is the offline fallback.
 */
export const LOC_NUMBERING_SYSTEM_MSG_KEYS = {
  ACTION: "perc.ui.developer@Set locale numbering system",
  HINT: "perc.ui.developer@Sets the locale numbering system with the locale update. The new value shows only after that save succeeds. The language string stays. The label, description, enabled status, date/time patterns, separators, currency fields, text direction, first day of week, measurement system, default timezone, and calendar stay because this update does not send them. A blank numbering system clears the numbering system. A numbering system longer than 16 characters does not write. Cancel does not write.",
  FIELD: "perc.ui.developer@Numbering system",
  SAVE: "perc.ui.developer@Save numbering system",
  SAVED: "perc.ui.developer@Locale numbering system saved.",
  CLEARED: "perc.ui.developer@Locale numbering system cleared.",
  ERROR: "perc.ui.developer@Could not change the locale numbering system.",
  FORBIDDEN:
    "perc.ui.developer@You need the Admin role to change a locale numbering system.",
  INVALID: "perc.ui.developer@The locale numbering system was not changed.",
  CONFLICT:
    "perc.ui.developer@The locale numbering system was not changed. The previous numbering system is unchanged.",
  CANCEL: "perc.ui.developer@Cancel",
} as const;

type LocaleNumberingSystemMsgKey = keyof typeof LOC_NUMBERING_SYSTEM_MSG_KEYS;

export const LOC_NUMBERING_SYSTEM_MSG: {
  readonly [K in LocaleNumberingSystemMsgKey]: string;
} = new Proxy(LOC_NUMBERING_SYSTEM_MSG_KEYS, {
  get(target, prop: string | symbol) {
    if (typeof prop !== "string" || !(prop in target)) {
      return undefined;
    }
    return message(target[prop as LocaleNumberingSystemMsgKey]);
  },
}) as { readonly [K in LocaleNumberingSystemMsgKey]: string };
