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
 * Display-format label chrome for Developer → Display Formats (#5432).
 *
 * <p>Kept out of {@code messages.ts} so sibling Developer slices can land without
 * editing that shared catalog. English after {@code @} is the offline fallback.
 */
export const DF_LABEL_MSG_KEYS = {
  ACTION: "perc.ui.developer@Set display format label",
  HINT: "perc.ui.developer@Sets the display format label with the display format update. The new label shows only after that save succeeds. The name stays the catalog key. The description and columns stay because this update does not send them. A blank label does not clear the name. Cancel does not write.",
  FIELD: "perc.ui.developer@Display format label",
  SAVE: "perc.ui.developer@Save label",
  SAVED: "perc.ui.developer@Display format label saved.",
  CLEARED: "perc.ui.developer@Display format label cleared. The name is unchanged.",
  ERROR: "perc.ui.developer@Could not change the display format label.",
  FORBIDDEN: "perc.ui.developer@You need the Admin role to change a display format label.",
  INVALID: "perc.ui.developer@The display format label was not changed.",
  CONFLICT:
    "perc.ui.developer@The display format label was not changed. The previous label is unchanged.",
} as const;

type DisplayFormatLabelMsgKey = keyof typeof DF_LABEL_MSG_KEYS;

export const DF_LABEL_MSG: { readonly [K in DisplayFormatLabelMsgKey]: string } = new Proxy(
  DF_LABEL_MSG_KEYS,
  {
    get(target, prop: string | symbol) {
      if (typeof prop !== "string" || !(prop in target)) {
        return undefined;
      }
      return message(target[prop as DisplayFormatLabelMsgKey]);
    },
  },
) as { readonly [K in DisplayFormatLabelMsgKey]: string };
