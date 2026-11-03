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
 * Locale description chrome for Developer → Locales (#5498).
 *
 * <p>Kept out of {@code messages.ts} so sibling Developer slices can land without
 * editing that shared catalog. English after {@code @} is the offline fallback.
 */
export const LOC_DESC_MSG_KEYS = {
  ACTION: "perc.ui.developer@Set locale description",
  HINT: "perc.ui.developer@Sets the locale description with the locale update. The new description shows only after that save succeeds. The language string stays. The label, enabled status, and format stay because this update does not send them. A blank description clears the description. Cancel does not write.",
  FIELD: "perc.ui.developer@Locale description",
  SAVE: "perc.ui.developer@Save description",
  SAVED: "perc.ui.developer@Locale description saved.",
  CLEARED: "perc.ui.developer@Locale description cleared.",
  ERROR: "perc.ui.developer@Could not change the locale description.",
  FORBIDDEN: "perc.ui.developer@You need the Admin role to change a locale description.",
  INVALID: "perc.ui.developer@The locale description was not changed.",
  CONFLICT:
    "perc.ui.developer@The locale description was not changed. The previous description is unchanged.",
  CANCEL: "perc.ui.developer@Cancel",
} as const;

type LocaleDescriptionMsgKey = keyof typeof LOC_DESC_MSG_KEYS;

export const LOC_DESC_MSG: { readonly [K in LocaleDescriptionMsgKey]: string } = new Proxy(
  LOC_DESC_MSG_KEYS,
  {
    get(target, prop: string | symbol) {
      if (typeof prop !== "string" || !(prop in target)) {
        return undefined;
      }
      return message(target[prop as LocaleDescriptionMsgKey]);
    },
  },
) as { readonly [K in LocaleDescriptionMsgKey]: string };
