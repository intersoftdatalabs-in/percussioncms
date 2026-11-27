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
 * Control display-name required chrome for Developer → CE Controls (#7345).
 *
 * <p>Reuses the {@code contentTypeFieldRequiredMessages} pattern: named
 * {@code perc.ui.developer@} codes resolved through {@code message()} with the
 * English text after {@code @} as the offline fallback. A blank display name
 * blocks the PUT before it fires, so the stored display name is kept.
 *
 * <p>Kept out of {@code messages.ts} so sibling Developer slices can land without
 * editing that shared catalog. English after {@code @} is the offline fallback.
 */
export const CTL_PROP_REQUIRED_MSG_KEYS = {
  ACTION: "perc.ui.developer@Control display name is required",
  HINT: "perc.ui.developer@Clearing the display name blocks save; the stored display name is kept. Press Escape in the field to cancel the edit without writing.",
  REQUIRED: "perc.ui.developer@Display name is required.",
} as const;

type ControlPropRequiredMsgKey = keyof typeof CTL_PROP_REQUIRED_MSG_KEYS;

export const CTL_PROP_REQUIRED_MSG: {
  readonly [K in ControlPropRequiredMsgKey]: string;
} = new Proxy(CTL_PROP_REQUIRED_MSG_KEYS, {
  get(target, prop: string | symbol) {
    if (typeof prop !== "string" || !(prop in target)) {
      return undefined;
    }
    return message(target[prop as ControlPropRequiredMsgKey]);
  },
}) as { readonly [K in ControlPropRequiredMsgKey]: string };
