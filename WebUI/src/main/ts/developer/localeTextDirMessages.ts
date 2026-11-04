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
 * Locale text-direction chrome for Developer → Locales (#5571).
 *
 * <p>Kept out of {@code messages.ts} so sibling Developer slices can land without
 * editing that shared catalog. English after {@code @} is the offline fallback.
 */
export const LOC_TEXT_DIR_MSG_KEYS = {
  ACTION: "perc.ui.developer@Set locale text direction",
  HINT: "perc.ui.developer@Sets the locale text direction with the locale update. Choose ltr or rtl. The new direction shows only after that save succeeds. The language string stays. The label, description, enabled status, date/time patterns, separators, currency fields, and other format fields stay because this update does not send them. A blank direction clears the text direction. Cancel does not write.",
  FIELD: "perc.ui.developer@Text direction",
  SAVE: "perc.ui.developer@Save text direction",
  SAVED: "perc.ui.developer@Locale text direction saved.",
  CLEARED: "perc.ui.developer@Locale text direction cleared.",
  ERROR: "perc.ui.developer@Could not change the locale text direction.",
  FORBIDDEN: "perc.ui.developer@You need the Admin role to change a locale text direction.",
  INVALID: "perc.ui.developer@The locale text direction was not changed.",
  CONFLICT:
    "perc.ui.developer@The locale text direction was not changed. The previous direction is unchanged.",
  CANCEL: "perc.ui.developer@Cancel",
  LTR: "perc.ui.developer@ltr",
  RTL: "perc.ui.developer@rtl",
  BLANK: "perc.ui.developer@(blank)",
} as const;

type LocaleTextDirMsgKey = keyof typeof LOC_TEXT_DIR_MSG_KEYS;

export const LOC_TEXT_DIR_MSG: { readonly [K in LocaleTextDirMsgKey]: string } = new Proxy(
  LOC_TEXT_DIR_MSG_KEYS,
  {
    get(target, prop: string | symbol) {
      if (typeof prop !== "string" || !(prop in target)) {
        return undefined;
      }
      return message(target[prop as LocaleTextDirMsgKey]);
    },
  },
) as { readonly [K in LocaleTextDirMsgKey]: string };
