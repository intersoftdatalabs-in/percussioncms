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
 * Locale fallback-chain chrome for Developer → Locales (#5788).
 *
 * <p>Kept out of {@code messages.ts} so sibling Developer slices can land without
 * editing that shared catalog. English after {@code @} is the offline fallback.
 */
export const LOC_FALLBACK_MSG_KEYS = {
  ACTION: "perc.ui.developer@Set locale fallback chain",
  HINT: "perc.ui.developer@Sets the ordered fallback chain with the locale update. The new chain shows only after that save succeeds. The language string stays. The label, description, enabled status, and every format field stay because this update does not send them. An empty chain clears the stored chain. Each entry must be a known locale; the locale itself, an unknown locale, or a malformed code does not write. Cancel does not write.",
  FIELD: "perc.ui.developer@Fallback chain",
  EMPTY: "perc.ui.developer@No fallback chain stored.",
  ADD_PLACEHOLDER: "perc.ui.developer@Add locale code, e.g. en-us",
  ADD: "perc.ui.developer@Add",
  MOVE_UP: "perc.ui.developer@Move up",
  MOVE_DOWN: "perc.ui.developer@Move down",
  REMOVE: "perc.ui.developer@Remove",
  SAVE: "perc.ui.developer@Save fallback chain",
  SAVED: "perc.ui.developer@Locale fallback chain saved.",
  CLEARED: "perc.ui.developer@Locale fallback chain cleared.",
  ERROR: "perc.ui.developer@Could not change the locale fallback chain.",
  FORBIDDEN:
    "perc.ui.developer@You need the Admin role to change a locale fallback chain.",
  INVALID: "perc.ui.developer@The locale fallback chain was not changed.",
  CONFLICT:
    "perc.ui.developer@The locale fallback chain was not changed. The previous chain is unchanged.",
  CANCEL: "perc.ui.developer@Cancel",
} as const;

type LocaleFallbackMsgKey = keyof typeof LOC_FALLBACK_MSG_KEYS;

export const LOC_FALLBACK_MSG: {
  readonly [K in LocaleFallbackMsgKey]: string;
} = new Proxy(LOC_FALLBACK_MSG_KEYS, {
  get(target, prop: string | symbol) {
    if (typeof prop !== "string" || !(prop in target)) {
      return undefined;
    }
    return message(target[prop as LocaleFallbackMsgKey]);
  },
}) as { readonly [K in LocaleFallbackMsgKey]: string };
