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
 * Display-format description chrome for Developer → Display Formats (#5381).
 *
 * <p>Kept out of {@code messages.ts} so sibling Developer slices can land without
 * editing that shared catalog. English after {@code @} is the offline fallback.
 */
export const DF_DESC_MSG_KEYS = {
  ACTION: "perc.ui.developer@Set display format description",
  HINT: "perc.ui.developer@Sets the display format description with the display format update. The new description shows only after that save succeeds. The name stays the catalog key. Columns and communities stay because this update does not send them. A blank description clears the description. Cancel does not write.",
  FIELD: "perc.ui.developer@Display format description",
  SAVE: "perc.ui.developer@Save description",
  SAVED: "perc.ui.developer@Display format description saved.",
  CLEARED: "perc.ui.developer@Display format description cleared.",
  ERROR: "perc.ui.developer@Could not change the display format description.",
  FORBIDDEN: "perc.ui.developer@You need the Admin role to change a display format description.",
  INVALID: "perc.ui.developer@The display format description was not changed.",
  CONFLICT:
    "perc.ui.developer@The display format description was not changed. The previous description is unchanged.",
} as const;

type DisplayFormatDescriptionMsgKey = keyof typeof DF_DESC_MSG_KEYS;

export const DF_DESC_MSG: { readonly [K in DisplayFormatDescriptionMsgKey]: string } = new Proxy(
  DF_DESC_MSG_KEYS,
  {
    get(target, prop: string | symbol) {
      if (typeof prop !== "string" || !(prop in target)) {
        return undefined;
      }
      return message(target[prop as DisplayFormatDescriptionMsgKey]);
    },
  },
) as { readonly [K in DisplayFormatDescriptionMsgKey]: string };
