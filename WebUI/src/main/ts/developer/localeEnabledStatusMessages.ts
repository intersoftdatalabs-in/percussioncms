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
 * Locale enabled-status chrome for Developer → Locales (#5550).
 *
 * <p>Kept out of {@code messages.ts} so sibling Developer slices can land without
 * editing that shared catalog. English after {@code @} is the offline fallback.
 */
export const LOC_STATUS_MSG_KEYS = {
  ACTION: "perc.ui.developer@Set locale enabled status",
  HINT: "perc.ui.developer@Sets whether the locale is enabled with the locale update. The new status shows only after that save succeeds. The language string stays. The label, description, and format profile stay because this update does not send them. Cancel does not write.",
  FIELD: "perc.ui.developer@Enabled status",
  SAVE: "perc.ui.developer@Save enabled status",
  SAVED: "perc.ui.developer@Locale enabled status saved.",
  ERROR: "perc.ui.developer@Could not change the locale enabled status.",
  FORBIDDEN: "perc.ui.developer@You need the Admin role to change a locale enabled status.",
  INVALID: "perc.ui.developer@The locale enabled status was not changed.",
  CONFLICT:
    "perc.ui.developer@The locale enabled status was not changed. The previous status is unchanged.",
  CANCEL: "perc.ui.developer@Cancel",
  ACTIVE: "perc.ui.developer@active",
  INACTIVE: "perc.ui.developer@inactive",
} as const;

type LocaleEnabledStatusMsgKey = keyof typeof LOC_STATUS_MSG_KEYS;

export const LOC_STATUS_MSG: { readonly [K in LocaleEnabledStatusMsgKey]: string } = new Proxy(
  LOC_STATUS_MSG_KEYS,
  {
    get(target, prop: string | symbol) {
      if (typeof prop !== "string" || !(prop in target)) {
        return undefined;
      }
      return message(target[prop as LocaleEnabledStatusMsgKey]);
    },
  },
) as { readonly [K in LocaleEnabledStatusMsgKey]: string };
